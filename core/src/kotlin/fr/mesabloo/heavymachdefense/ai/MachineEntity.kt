package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.utils.Location
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.ui.stage.Machine
import kotlin.math.abs

/**
 * Bridges the visual [Machine] (Scene2D Group) and AI [GameObject] (Steerable) systems.
 * Wraps a Machine actor and its Box2D body so that the behavior tree can query
 * position, range, health, and control walk/stop.
 */
class MachineEntity(
    val machine: Machine,
    val body: Body,
    private val allObjects: MutableList<GameObject>,
    private val moveSpeed: Float
) : GameObject() {

    private var tagged = false

    // --- Body rotation ---
    /** Current body facing angle in degrees (0°=right, 90°=up). */
    private var facingAngleDeg = 90f  // start facing up

    companion object {
        private const val FIXED_DT = 1f / 60f
        /**
         * Machine body turn rate (degrees/sec).
         * Faster than enemy tanks (240 vs 180) because:
         * - Entire Group rotates (body+feet+weapons) → misalignment is very visible
         * - aimAt() snaps instantly to target → fast recovery needed after target lost
         * - Flow field is already smooth (bilinear interpolated) → no extra smoothing needed
         */
        private const val BODY_TURN_RATE = 240f
    }

    // --- GameObject abstracts ---

    override val objects: MutableList<GameObject> get() = allObjects
    override val team: Team = Team.ALLY

    override val range: Pair<Float, Float>?
        get() = Pair(machine.attackRange / PPM, machine.detectionRange / PPM)

    override val attackSpeed: Float get() = machine.attackSpeed

    override val isAlive: Boolean get() = machine.isAlive

    override fun walk() {
        machine.walking = true
        // When no target is locked, rotate body toward movement direction.
        // When a target exists, aimAt() handles rotation (takes priority).
        if (!hasTarget) {
            updateBodyRotation()
        }
    }

    override fun stopInPlace() {
        machine.walking = false
        // Don't zero velocity — Machine's walking Action handles deceleration.
        // The Action checks machine.walking each frame and smoothly reduces speed.
    }

    override fun aimAt(target: GameObject) {
        val myPos = body.position
        val targetPos = target.getPosition()
        val angle = MathUtils.atan2(targetPos.y - myPos.y, targetPos.x - myPos.x) * MathUtils.radiansToDegrees
        machine.rotation = angle
        facingAngleDeg = angle  // keep facing angle in sync with aim
    }

    override fun aimDefault() {
        // Smoothly return to flow field direction, or default upward
        val flow = machine.navGrid?.getFlowToTop(body.position.x, body.position.y)
        val targetAngle = if (flow != null) {
            MathUtils.atan2(flow.y, flow.x) * MathUtils.radiansToDegrees
        } else {
            90f // default: face upward
        }
        facingAngleDeg = rotateToward(facingAngleDeg, targetAngle, BODY_TURN_RATE * FIXED_DT)
        machine.rotation = facingAngleDeg
    }

    // --- Body rotation (2-layer: flow field → rate-limit) ---

    /**
     * Smoothly rotate machine body toward movement direction.
     *
     * Unlike enemy tanks (which use a 4-layer filter with EMA + dead zone),
     * ally machines use a simpler 2-layer approach:
     *
     * Layer 1 — **Flow field direction**: Bilinear-interpolated NavGrid flow
     *   is already noise-free, so additional EMA smoothing is unnecessary.
     * Layer 2 — **Rate-limited rotation**: 240°/s. Fast enough to track flow
     *   field changes and recover quickly after aimAt() snaps to a target.
     *
     * EMA and dead zone are omitted because:
     * - The entire Machine Group rotates (body+feet+weapons) — misalignment
     *   between facing and movement direction is immediately visible.
     * - aimAt() instantly snaps rotation to the target. After the target dies,
     *   EMA lag + dead zone would leave the machine "crab-walking" for ~0.5s.
     * - The bilinear-interpolated flow field is already smooth enough.
     */
    private fun updateBodyRotation() {
        val flow = machine.navGrid?.getFlowToTop(body.position.x, body.position.y)
            ?: return  // no flow field → keep current facing

        val targetAngle = MathUtils.atan2(flow.y, flow.x) * MathUtils.radiansToDegrees
        facingAngleDeg = rotateToward(facingAngleDeg, targetAngle, BODY_TURN_RATE * FIXED_DT)
        machine.rotation = facingAngleDeg
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

    override fun getBoundingRadius(): Float = (machine.width / 2f) / PPM

    override fun isTagged(): Boolean = tagged

    override fun setTagged(tagged: Boolean) {
        this.tagged = tagged
    }

    // --- Limiter (minimal, steering behaviors not used) ---

    override fun getZeroLinearSpeedThreshold(): Float = 0.001f
    override fun setZeroLinearSpeedThreshold(value: Float) {}
    override fun getMaxLinearSpeed(): Float = moveSpeed / PPM
    override fun setMaxLinearSpeed(value: Float) {}
    override fun getMaxLinearAcceleration(): Float = 0f
    override fun setMaxLinearAcceleration(value: Float) {}
    override fun getMaxAngularSpeed(): Float = 0f
    override fun setMaxAngularSpeed(value: Float) {}
    override fun getMaxAngularAcceleration(): Float = 0f
    override fun setMaxAngularAcceleration(value: Float) {}

    /**
     * Minimal Location implementation for [newLocation].
     */
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
