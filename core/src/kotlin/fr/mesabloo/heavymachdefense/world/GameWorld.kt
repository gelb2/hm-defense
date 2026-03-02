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

        // Foreground overlays render above units (units pass behind buildings)
        terrain.foregroundActors.forEach { it.toFront() }

        // Bases render above units and foreground (units emerge from behind bases)
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
        /** Edge-to-edge distance (world units) within which units repel each other */
        private const val INFLUENCE_RADIUS = 2.0f
        /** Separation strength per nearby unit */
        private const val SEPARATION_STRENGTH = 0.3f
        /** Forward slowdown strength (0=none, 1=full stop when directly blocked) */
        private const val FORWARD_SLOWDOWN = 0.6f
        /** Edge-to-edge Y distance for forward blockage detection */
        private const val FORWARD_CHECK_DIST = 3.0f
        /** Maximum separation speed as fraction of original speed */
        private const val MAX_SEPARATION_RATIO = 0.8f
        /** Minimum forward speed ratio */
        private const val MIN_FORWARD_RATIO = 0.1f
        /** Velocity smoothing factor (0=no change, 1=instant) */
        private const val VELOCITY_SMOOTHING = 0.3f

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

        // --- Spatial hash ---
        /** Cell size for spatial hash (world units). Must be >= INFLUENCE_RADIUS. */
        private const val CELL_SIZE = 4.0f
    }

    // ---------------------------------------------------------------
    // Spatial hash for efficient neighbor lookups
    // ---------------------------------------------------------------

    private val spatialCells = HashMap<Long, MutableList<Int>>(64)
    private val cellListPool = mutableListOf<MutableList<Int>>()

    private fun spatialKey(x: Float, y: Float): Long {
        val cx = (x / CELL_SIZE).toInt()
        val cy = (y / CELL_SIZE).toInt()
        return (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
    }

    /** Build spatial hash from current unit positions. */
    private fun buildSpatialHash(units: List<Body>) {
        // Return lists to pool
        for (list in spatialCells.values) {
            list.clear()
            cellListPool.add(list)
        }
        spatialCells.clear()

        for (i in units.indices) {
            val pos = units[i].position
            val key = spatialKey(pos.x, pos.y)
            val list = spatialCells.getOrPut(key) {
                if (cellListPool.isNotEmpty()) cellListPool.removeAt(cellListPool.size - 1)
                else mutableListOf()
            }
            list.add(i)
        }
    }

    /** Query neighbors within ±1 cell of the given position. Returns indices into units list. */
    private fun queryNeighbors(x: Float, y: Float, callback: (Int) -> Unit) {
        val cx = (x / CELL_SIZE).toInt()
        val cy = (y / CELL_SIZE).toInt()
        for (dx in -1..1) {
            for (dy in -1..1) {
                val key = ((cx + dx).toLong() shl 32) or ((cy + dy).toLong() and 0xFFFFFFFFL)
                spatialCells[key]?.forEach { callback(it) }
            }
        }
    }

    // ---------------------------------------------------------------
    // Unified velocity-based avoidance + overlap separation
    // ---------------------------------------------------------------

    /**
     * Steering-based velocity adjustment system.
     *
     * For each unit, computes:
     * 1. **Directional separation** — push away from nearby units, preferring
     *    the direction perpendicular to movement (avoids fighting flow field)
     * 2. **Overlap separation** — stronger push when units actually overlap
     * 3. **Forward slowdown** — reduce speed when blocked ahead
     *
     * Uses spatial hashing for O(n·k) neighbor queries instead of O(n²).
     *
     * Runs BEFORE [World.step] so adjusted velocities take effect
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
        if (n < 2) {
            // Clean up stale velocity entries when no units to process
            if (lastVelocity.isNotEmpty()) lastVelocity.clear()
            return
        }

        // Purge velocity history for destroyed bodies
        if (lastVelocity.size > n) {
            lastVelocity.keys.retainAll(units.toSet())
        }

        val terrainMaxX = TERRAIN_WIDTH / PPM

        // Build spatial hash for efficient neighbor queries
        buildSpatialHash(units)

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
            val speedSq = vel.x * vel.x + vel.y * vel.y
            val isMoving = speedSq >= MIN_VELOCITY_THRESHOLD * MIN_VELOCITY_THRESHOLD
            val totalSpeed = if (isMoving) sqrt(speedSq) else 0f

            // Velocity direction (unit vector). For perpendicular separation calculation.
            val velDirX: Float
            val velDirY: Float
            if (isMoving && totalSpeed > 0.001f) {
                velDirX = vel.x / totalSpeed
                velDirY = vel.y / totalSpeed
            } else {
                velDirX = 0f
                velDirY = 1f  // Default: assume upward for allies, downward handled below
            }

            // Perpendicular to velocity direction (for directional separation)
            val perpX = -velDirY
            val perpY = velDirX

            var separationX = 0f       // directional separation push
            var separationY = 0f
            var forwardScale = 1f      // forward slowdown
            var overlapPushX = 0f      // overlap separation (accumulated)

            // Query nearby units via spatial hash
            queryNeighbors(x, y) { j ->
                if (j == i) return@queryNeighbors
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
                    overlapPushX += pushDir * overlapX
                }

                // --- Directional separation + forward slowdown (on-screen moving only) ---
                if (!isMoving || isOffScreen) return@queryNeighbors

                if (gapX > INFLUENCE_RADIUS && gapY > INFLUENCE_RADIUS) return@queryNeighbors

                val xProx = ((INFLUENCE_RADIUS - gapX) / INFLUENCE_RADIUS).coerceIn(0f, 1f)
                val yProx = ((INFLUENCE_RADIUS - gapY) / INFLUENCE_RADIUS).coerceIn(0f, 1f)

                if (xProx > 0f && yProx > 0f) {
                    // Direction from this unit to neighbor
                    val dx = ox - x
                    val dy = oy - y

                    // Project neighbor offset onto perpendicular axis
                    // Positive dot = neighbor is to the "right" of our movement direction
                    val perpDot = dx * perpX + dy * perpY

                    // Push in the opposite perpendicular direction
                    val pushSign = if (perpDot > 0.001f) -1f
                                   else if (perpDot < -0.001f) 1f
                                   else if (i % 2 == 0) -1f else 1f

                    val strength = xProx * yProx * SEPARATION_STRENGTH
                    separationX += perpX * pushSign * strength
                    separationY += perpY * pushSign * strength
                }

                // Forward slowdown: check if neighbor is ahead in our movement direction
                val toOtherAlongVel = (ox - x) * velDirX + (oy - y) * velDirY
                if (toOtherAlongVel > 0f && gapX < 0f) {
                    val edgeGapY = (abs(oy - y) - hh - ohh).coerceAtLeast(0f)
                    val fProx = (1f - edgeGapY / FORWARD_CHECK_DIST).coerceIn(0f, 1f)
                    if (fProx > 0f) {
                        forwardScale = minOf(forwardScale, 1f - fProx * FORWARD_SLOWDOWN)
                    }
                }
            }

            // Convert overlap to velocity
            val sepFactor = if (isOffScreen) OFFSCREEN_SEPARATION_VEL else OVERLAP_SEPARATION_VEL
            val overlapVel = overlapPushX * sepFactor

            when {
                isMoving && !isOffScreen -> {
                    // On-screen moving: directional separation + overlap + forward control

                    // Scale separation by speed and clamp
                    val sepMag = sqrt(separationX * separationX + separationY * separationY)
                    val maxSep = totalSpeed * MAX_SEPARATION_RATIO
                    var scaledSepX = separationX * totalSpeed
                    var scaledSepY = separationY * totalSpeed
                    if (sepMag * totalSpeed > maxSep) {
                        val scale = maxSep / (sepMag * totalSpeed)
                        scaledSepX *= scale
                        scaledSepY *= scale
                    }

                    // Apply forward slowdown to velocity
                    val effectiveForward = forwardScale.coerceAtLeast(MIN_FORWARD_RATIO)
                    var targetX = vel.x * effectiveForward + scaledSepX + overlapVel
                    var targetY = vel.y * effectiveForward + scaledSepY

                    // Clamp at terrain boundaries (X only)
                    if (targetX < 0f && x - hw < 0.1f) targetX = 0f
                    if (targetX > 0f && x + hw > terrainMaxX - 0.1f) targetX = 0f

                    // Smooth velocity transition to reduce jitter
                    val prev = lastVelocity[body]
                    if (prev != null) {
                        targetX = prev[0] + (targetX - prev[0]) * VELOCITY_SMOOTHING
                        targetY = prev[1] + (targetY - prev[1]) * VELOCITY_SMOOTHING
                    }
                    lastVelocity.getOrPut(body) { FloatArray(2) }.also {
                        it[0] = targetX; it[1] = targetY
                    }

                    // Clamp total speed to ~120% of original (allow slight overshoot for overlap)
                    val finalSpeedSq = targetX * targetX + targetY * targetY
                    val maxSpeed = totalSpeed * 1.2f
                    if (finalSpeedSq > maxSpeed * maxSpeed) {
                        val scale = maxSpeed / sqrt(finalSpeedSq)
                        targetX *= scale
                        targetY *= scale
                    }

                    body.setLinearVelocity(targetX, targetY)
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
                    val prev = lastVelocity[body]
                    val prevX = prev?.get(0) ?: 0f
                    val smoothed = prevX + (clampedVel - prevX) * VELOCITY_SMOOTHING
                    lastVelocity.getOrPut(body) { FloatArray(2) }.also {
                        it[0] = smoothed; it[1] = 0f
                    }
                    body.setLinearVelocity(smoothed, 0f)
                }
            }
        }
    }

    /** Per-body velocity history for smoothing. Stores [x, y] of last adjusted velocity. */
    private val lastVelocity = HashMap<Body, FloatArray>()

    override fun dispose() {
        this.world.dispose()
    }
}
