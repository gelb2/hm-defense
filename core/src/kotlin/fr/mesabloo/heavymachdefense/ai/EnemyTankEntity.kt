package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.utils.Location
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.data.NavGrid
import fr.mesabloo.heavymachdefense.ui.stage.EnemyTank
import kotlin.math.abs
import kotlin.math.sqrt

class EnemyTankEntity(
    val tank: EnemyTank,
    val body: Body,
    private val allObjects: MutableList<GameObject>,
    private val moveSpeed: Float,
    private val navGrid: NavGrid? = null
) : GameObject() {

    var paralyzedTimer: Float = 0f
        set(value) {
            // Reset speed factor when paralysis begins (smooth re-acceleration on recovery)
            if (value > 0f && field <= 0f) speedFactor = 0f
            field = value
        }

    private var tagged = false

    // --- Acceleration/deceleration ---
    /** Speed factor 0.0 (stopped) to 1.0 (full speed). Smoothly transitions. */
    private var speedFactor = 0f

    // --- Body rotation ---
    /** Current body facing angle in degrees (0°=right, 90°=up, -90°=down). */
    private var facingAngleDeg = -90f  // start facing down
    /** EMA-smoothed direction vector (unit vector, avoids velocity noise). */
    private var smoothedDirX = 0f
    private var smoothedDirY = -1f  // initial: pointing down
    /** Hysteresis state: true when actively rotating toward target. */
    private var isRotating = false

    companion object {
        /** Fixed timestep matching world.step(1/60f) */
        private const val FIXED_DT = 1f / 60f
        /** Reach full speed in ~0.3s */
        private const val ACCEL_RATE = 3.3f
        /** Stop in ~0.2s */
        private const val DECEL_RATE = 5.0f
        /** Body turn rate in degrees per second */
        private const val BODY_TURN_RATE = 180f
        /** Minimum speed² (world units) below which body rotation is not updated */
        private const val MIN_ROTATION_SPEED_SQ = 0.05f * 0.05f

        // --- Rotation smoothing (3-layer filter) ---
        /** EMA alpha per frame. λ=8 at 60fps → half-life ≈ 0.087s. */
        private const val ROTATION_EMA_ALPHA = 0.125f
        /** Start rotating when angle diff exceeds this (degrees). */
        private const val ROTATION_ENTER_DEG = 8f
        /** Stop rotating when angle diff drops below this (degrees). */
        private const val ROTATION_EXIT_DEG = 3f
    }

    // --- GameObject abstracts ---

    override val objects: MutableList<GameObject> get() = allObjects
    override val team: Team = Team.ENEMY

    override val range: Pair<Float, Float>?
        get() = Pair(tank.attackRange / PPM, tank.detectionRange / PPM)

    override val attackSpeed: Float get() = tank.attackSpeed

    override val isAlive: Boolean get() = tank.isAlive

    override fun walk() {
        if (paralyzedTimer > 0f) return
        tank.walking = true

        // Rotate body toward actual movement direction (velocity from last physics step)
        updateBodyRotation()

        // Accelerate toward full speed
        speedFactor = (speedFactor + ACCEL_RATE * FIXED_DT).coerceAtMost(1f)
        applyFlowVelocity()
    }

    override fun stopInPlace() {
        tank.walking = false

        // Decelerate toward stop (Arrive behavior)
        speedFactor = (speedFactor - DECEL_RATE * FIXED_DT).coerceAtLeast(0f)

        if (speedFactor > 0.001f) {
            // Still decelerating — maintain flow field direction at reduced speed
            applyFlowVelocity()
        } else {
            speedFactor = 0f
            body.setLinearVelocity(0f, 0f)
        }
    }

    /**
     * Apply velocity along flow field direction, scaled by current [speedFactor].
     */
    private fun applyFlowVelocity() {
        val speed = (moveSpeed / PPM) * speedFactor
        val flow = navGrid?.getFlowToBase(body.position.x, body.position.y)
        if (flow != null) {
            body.setLinearVelocity(flow.x * speed, flow.y * speed)
        } else {
            body.setLinearVelocity(0f, -speed) // fallback: straight down
        }
    }

    // --- Body rotation (3-layer filter: flow field → EMA → dead zone → rate-limit) ---

    /**
     * Smoothly rotate tank body toward movement direction.
     *
     * Layer 1 — **Flow field direction**: Uses the bilinear-interpolated NavGrid flow
     *   direction instead of noisy post-collision-avoidance velocity. Falls back to
     *   velocity direction when no flow field is available.
     * Layer 2 — **EMA smoothing**: Exponential moving average on the direction vector
     *   filters residual noise (λ=8, half-life ≈ 0.087s).
     * Layer 3 — **Angular dead zone with hysteresis**: Rotation only activates when
     *   angle diff > 8° and deactivates when < 3°, preventing micro-oscillations.
     * Layer 4 — **Rate-limited rotation**: Max 180°/s turn speed.
     */
    private fun updateBodyRotation() {
        // Layer 1: Flow field direction (smooth, bilinear-interpolated, no collision noise)
        val flow = navGrid?.getFlowToBase(body.position.x, body.position.y)
        val rawDirX: Float
        val rawDirY: Float
        if (flow != null) {
            rawDirX = flow.x
            rawDirY = flow.y
        } else {
            // Fallback: velocity direction
            val vel = body.linearVelocity
            val speedSq = vel.x * vel.x + vel.y * vel.y
            if (speedSq < MIN_ROTATION_SPEED_SQ) return
            val invLen = 1f / sqrt(speedSq)
            rawDirX = vel.x * invLen
            rawDirY = vel.y * invLen
        }

        // Layer 2: EMA on direction vector (filters cell-boundary jumps)
        smoothedDirX += (rawDirX - smoothedDirX) * ROTATION_EMA_ALPHA
        smoothedDirY += (rawDirY - smoothedDirY) * ROTATION_EMA_ALPHA

        val targetAngle = MathUtils.atan2(smoothedDirY, smoothedDirX) * MathUtils.radiansToDegrees

        // Layer 3: Angular dead zone with hysteresis
        val angleDiff = abs(shortAngleDist(facingAngleDeg, targetAngle))
        if (!isRotating && angleDiff > ROTATION_ENTER_DEG) {
            isRotating = true
        } else if (isRotating && angleDiff < ROTATION_EXIT_DEG) {
            isRotating = false
        }
        if (!isRotating) return

        // Layer 4: Rate-limited rotation
        facingAngleDeg = rotateToward(facingAngleDeg, targetAngle, BODY_TURN_RATE * FIXED_DT)
        tank.rotation = facingAngleDeg
    }

    /** Shortest angular distance from [from] to [to] in degrees, range (-180, 180]. */
    private fun shortAngleDist(from: Float, to: Float): Float =
        ((to - from) % 360f + 540f) % 360f - 180f

    /** Rotate [current] toward [target] by at most [maxDelta] degrees. */
    private fun rotateToward(current: Float, target: Float, maxDelta: Float): Float {
        val diff = shortAngleDist(current, target)
        return if (abs(diff) <= maxDelta) target
        else current + maxDelta * if (diff > 0f) 1f else -1f
    }

    override fun aimAt(target: GameObject) {
        val myPos = body.position
        val targetPos = target.getPosition()
        val worldAngle = MathUtils.atan2(targetPos.y - myPos.y, targetPos.x - myPos.x) * MathUtils.radiansToDegrees
        // Weapon rotation is relative to the tank group's rotation
        tank.weaponImage.rotation = worldAngle - tank.rotation
    }

    override fun aimDefault() {
        tank.weaponImage.rotation = 0f // align with tank body
    }

    // --- Location<Vector2> ---

    override fun getPosition(): Vector2 = body.position

    override fun getOrientation(): Float = body.angle

    override fun setOrientation(orientation: Float) {
        body.setTransform(body.position, orientation)
    }

    override fun vectorToAngle(vector: Vector2): Float =
        MathUtils.atan2(-vector.x, vector.y)

    override fun angleToVector(outVector: Vector2, angle: Float): Vector2 =
        outVector.set(-MathUtils.sin(angle), MathUtils.cos(angle))

    override fun newLocation(): Location<Vector2> = SimpleLocation()

    // --- Steerable<Vector2> ---

    override fun getLinearVelocity(): Vector2 = body.linearVelocity

    override fun getAngularVelocity(): Float = body.angularVelocity

    override fun getBoundingRadius(): Float = (tank.width / 2f) / PPM

    override fun isTagged(): Boolean = tagged

    override fun setTagged(tagged: Boolean) {
        this.tagged = tagged
    }

    // --- Limiter ---

    override fun getZeroLinearSpeedThreshold(): Float = 0.001f
    override fun setZeroLinearSpeedThreshold(value: Float) {}
    override fun getMaxLinearSpeed(): Float = moveSpeed / PPM
    override fun setMaxLinearSpeed(value: Float) {}
    override fun getMaxLinearAcceleration(): Float = ACCEL_RATE * moveSpeed / PPM
    override fun setMaxLinearAcceleration(value: Float) {}
    override fun getMaxAngularSpeed(): Float = 0f
    override fun setMaxAngularSpeed(value: Float) {}
    override fun getMaxAngularAcceleration(): Float = 0f
    override fun setMaxAngularAcceleration(value: Float) {}

    private class SimpleLocation : Location<Vector2> {
        private val position = Vector2()
        private var orientation = 0f

        override fun getPosition(): Vector2 = position
        override fun getOrientation(): Float = orientation
        override fun setOrientation(orientation: Float) { this.orientation = orientation }
        override fun vectorToAngle(vector: Vector2): Float = MathUtils.atan2(-vector.x, vector.y)
        override fun angleToVector(outVector: Vector2, angle: Float): Vector2 =
            outVector.set(-MathUtils.sin(angle), MathUtils.cos(angle))
        override fun newLocation(): Location<Vector2> = SimpleLocation()
    }
}
