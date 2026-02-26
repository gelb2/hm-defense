package fr.mesabloo.heavymachdefense.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import fr.mesabloo.heavymachdefense.BG_BORDER
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.TERRAIN_WIDTH
import fr.mesabloo.heavymachdefense.ifDebug
import fr.mesabloo.heavymachdefense.ui.stage.Terrain
import ktx.actors.contains
import ktx.box2d.createWorld
import kotlin.math.abs
import kotlin.math.sqrt

class GameWorld(private val terrain: Terrain) : Disposable {
    val world: World = createWorld()

    private val debugRenderer = Box2DDebugRenderer()

    private val bodies = Array<Body>(50)
    private val unitBodies = mutableListOf<Body>()

    init {
        this.world.setContactListener(object : ContactListener {
            override fun beginContact(p0: Contact) {
                val bodyA = p0.fixtureA
                val bodyB = p0.fixtureB

                if ((bodyA.userData == BG_BORDER && !bodyB.isSensor) || (bodyB.userData == BG_BORDER && !bodyA.isSensor)) {
                    Gdx.app.debug(this.javaClass.simpleName, "Contact with border detected")
                }
            }

            override fun endContact(p0: Contact?) {

            }

            override fun preSolve(p0: Contact?, p1: Manifold?) {

            }

            override fun postSolve(p0: Contact?, p1: ContactImpulse?) {

            }
        })
    }

    fun render(deltaTime: Float) {
        adjustUnitVelocities()

        this.world.step(1 / 60f, 6, 2)

        bodies.clear()
        this.world.getBodies(bodies)

        val terrainMaxX = TERRAIN_WIDTH / PPM

        bodies.forEach { body ->
            val pos = body.position
            (body.userData as? Actor)?.also {
                // Clamp kinematic bodies to terrain X bounds
                if (body.type == BodyDef.BodyType.KinematicBody) {
                    val hw = it.width / 2f / PPM
                    val clampedX = pos.x.coerceIn(hw, terrainMaxX - hw)
                    if (clampedX != pos.x) {
                        body.setTransform(clampedX, pos.y, 0f)
                    }
                }

                it.setPosition(body.position.x * PPM - it.width / 2f, body.position.y * PPM - it.height / 2f)

                if (!this.terrain.contains(it)) {
                    this.terrain.addActor(it)
                }
            }
        }

        // Bases render above units (units emerge from behind bases)
        bodies.forEach { body ->
            if (body.type == BodyDef.BodyType.StaticBody) {
                (body.userData as? Actor)?.toFront()
            }
        }

        ifDebug {
            this.debugRenderer.render(this.world, this.terrain.stage.camera.combined.cpy().translate(128f, 180f, 0f))
        }
    }

    companion object {
        // --- Velocity thresholds ---
        /** Velocity below this threshold is treated as "not moving" */
        private const val MIN_VELOCITY_THRESHOLD = 0.01f

        // --- Proximity-based avoidance (gentle steering for nearby units) ---
        /** Edge-to-edge distance (world units) within which units repel each other laterally */
        private const val INFLUENCE_RADIUS = 2.0f
        /** Lateral repulsion strength per nearby unit */
        private const val LATERAL_REPULSION = 0.3f
        /** Forward slowdown strength (0=none, 1=full stop when directly blocked) */
        private const val FORWARD_SLOWDOWN = 0.6f
        /** Edge-to-edge Y distance for forward blockage detection */
        private const val FORWARD_CHECK_DIST = 3.0f
        /** Maximum lateral speed as fraction of original forward speed */
        private const val MAX_LATERAL_RATIO = 0.8f
        /** Minimum forward speed ratio */
        private const val MIN_FORWARD_RATIO = 0.1f
        /** Lateral velocity smoothing factor (0=no change, 1=instant) */
        private const val LATERAL_SMOOTHING = 0.3f

        // --- Overlap separation (velocity-based, replaces setTransform) ---
        /** Edge-to-edge buffer maintained between units (~1.6px) */
        private const val SEPARATION_BUFFER = 0.1f
        /** On-screen: overlap → velocity factor (wu/s per wu of overlap) */
        private const val OVERLAP_SEPARATION_VEL = 3.0f
        /** Off-screen: much faster correction (invisible to player) */
        private const val OFFSCREEN_SEPARATION_VEL = 30.0f

        // --- Off-screen spawn: visibility boundaries (pixels) ---
        private const val VISIBLE_BOTTOM_PX = 0f
        private const val VISIBLE_TOP_PX = 2048f
    }

    // ---------------------------------------------------------------
    // Unified velocity-based avoidance + overlap separation
    // ---------------------------------------------------------------

    /**
     * Single system controlling all lateral unit movement via velocity.
     *
     * For each unit, computes two forces:
     * 1. **Proximity repulsion** — gentle steering away from nearby units (on-screen moving only)
     * 2. **Overlap separation** — stronger push when units actually overlap (all units)
     *
     * Both are applied as velocity, eliminating the position-teleport jitter
     * that occurred when setTransform-based separation fought with velocity-based movement.
     *
     * Runs BEFORE [World.step] so the adjusted velocity takes effect
     * in the current frame's physics simulation.
     */
    private fun adjustUnitVelocities() {
        bodies.clear()
        world.getBodies(bodies)

        val units = unitBodies
        units.clear()
        bodies.forEach { body ->
            if (body.type == BodyDef.BodyType.KinematicBody && body.userData is Actor) {
                units.add(body)
            }
        }

        val n = units.size
        if (n < 2) return

        val terrainMaxX = TERRAIN_WIDTH / PPM

        for (i in 0 until n) {
            val body = units[i]
            val vel = body.linearVelocity
            val actor = body.userData as Actor
            val hw = actor.width / 2f / PPM
            val hh = actor.height / 2f / PPM
            val x = body.position.x
            val y = body.position.y

            val yPixels = y * PPM
            val isOffScreen = yPixels < VISIBLE_BOTTOM_PX || yPixels > VISIBLE_TOP_PX
            val isMoving = abs(vel.y) >= MIN_VELOCITY_THRESHOLD

            var lateralPush = 0f       // proximity-based repulsion
            var forwardScale = 1f      // forward slowdown
            var overlapPush = 0f       // overlap separation (accumulated)

            for (j in 0 until n) {
                if (j == i) continue
                val other = units[j]
                val oActor = other.userData as Actor
                val ohw = oActor.width / 2f / PPM
                val ohh = oActor.height / 2f / PPM
                val ox = other.position.x
                val oy = other.position.y

                val gapX = abs(ox - x) - hw - ohw
                val gapY = abs(oy - y) - hh - ohh

                // --- Overlap separation (ALL units: moving/paused, on/off-screen) ---
                if (gapX < SEPARATION_BUFFER && gapY < 0f) {
                    val overlapX = SEPARATION_BUFFER - gapX
                    val dx = ox - x
                    val pushDir = if (dx > 0.001f) -1f
                                  else if (dx < -0.001f) 1f
                                  else if (i % 2 == 0) -1f else 1f
                    overlapPush += pushDir * overlapX
                }

                // --- Proximity repulsion + forward slowdown (on-screen moving only) ---
                if (!isMoving || isOffScreen) continue

                if (gapX > INFLUENCE_RADIUS && gapY > INFLUENCE_RADIUS) continue

                val xProx = ((INFLUENCE_RADIUS - gapX) / INFLUENCE_RADIUS).coerceIn(0f, 1f)
                val yProx = ((INFLUENCE_RADIUS - gapY) / INFLUENCE_RADIUS).coerceIn(0f, 1f)

                if (xProx > 0f && yProx > 0f) {
                    val dx = ox - x
                    val pushDir = if (dx > 0.001f) -1f
                                  else if (dx < -0.001f) 1f
                                  else if (i % 2 == 0) -1f else 1f
                    lateralPush += pushDir * xProx * yProx * LATERAL_REPULSION
                }

                val fwd = if (vel.y > 0f) 1f else -1f
                val forwardDist = (oy - y) * fwd
                if (forwardDist > 0f && gapX < 0f) {
                    val edgeGapY = (forwardDist - hh - ohh).coerceAtLeast(0f)
                    val fProx = (1f - edgeGapY / FORWARD_CHECK_DIST).coerceIn(0f, 1f)
                    if (fProx > 0f) {
                        forwardScale = minOf(forwardScale, 1f - fProx * FORWARD_SLOWDOWN)
                    }
                }
            }

            // Convert overlap to velocity
            val sepFactor = if (isOffScreen) OFFSCREEN_SEPARATION_VEL else OVERLAP_SEPARATION_VEL
            val overlapVel = overlapPush * sepFactor

            when {
                isMoving && !isOffScreen -> {
                    // On-screen moving: proximity repulsion + overlap separation + forward control
                    val speed = abs(vel.y)
                    val fwd = if (vel.y > 0f) 1f else -1f

                    val proximityLat = (lateralPush * speed)
                        .coerceIn(-speed * MAX_LATERAL_RATIO, speed * MAX_LATERAL_RATIO)
                    val latVel = proximityLat + overlapVel
                    val fwdVel = fwd * speed * forwardScale.coerceAtLeast(MIN_FORWARD_RATIO)

                    val clampedLatVel = when {
                        latVel < 0f && x - hw < 0.1f -> 0f
                        latVel > 0f && x + hw > terrainMaxX - 0.1f -> 0f
                        else -> latVel
                    }

                    val prevLat = lastLateralVel[body] ?: 0f
                    val smoothedLatVel = prevLat + (clampedLatVel - prevLat) * LATERAL_SMOOTHING
                    lastLateralVel[body] = smoothedLatVel

                    var finalX = smoothedLatVel
                    var finalY = fwdVel
                    val totalSpeedSq = finalX * finalX + finalY * finalY
                    val maxSpeed = speed * 1.2f // allow slight overshoot for overlap resolution
                    if (totalSpeedSq > maxSpeed * maxSpeed) {
                        val scale = maxSpeed / sqrt(totalSpeedSq)
                        finalX *= scale
                        finalY *= scale
                    }

                    body.setLinearVelocity(finalX, finalY)
                }
                isOffScreen && abs(overlapVel) > 0.001f -> {
                    // Off-screen: raw fast separation velocity (no smoothing)
                    val clampedVel = when {
                        overlapVel < 0f && x - hw < 0.1f -> 0f
                        overlapVel > 0f && x + hw > terrainMaxX - 0.1f -> 0f
                        else -> overlapVel
                    }
                    body.setLinearVelocity(clampedVel, vel.y)
                }
                !isMoving && abs(overlapVel) > 0.001f -> {
                    // Paused but overlapping: gentle separation velocity
                    val clampedVel = when {
                        overlapVel < 0f && x - hw < 0.1f -> 0f
                        overlapVel > 0f && x + hw > terrainMaxX - 0.1f -> 0f
                        else -> overlapVel
                    }
                    val prevLat = lastLateralVel[body] ?: 0f
                    val smoothed = prevLat + (clampedVel - prevLat) * LATERAL_SMOOTHING
                    lastLateralVel[body] = smoothed
                    body.setLinearVelocity(smoothed, 0f)
                }
            }
        }
    }

    private val lastLateralVel = HashMap<Body, Float>()

    override fun dispose() {
        this.world.dispose()
    }
}