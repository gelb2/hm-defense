package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.utils.Location
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Group

/**
 * Wraps a base building (AllyBase or EnemyBase) as a [GameObject]
 * so that it can be targeted by AI-driven units.
 *
 * Bases are static: they do not move, shoot, or run behavior trees.
 */
class BaseEntity(
    val baseGroup: Group,
    val body: Body,
    private val allObjects: MutableList<GameObject>,
    override val team: Team,
    var hp: Int = 500,
    var maxHp: Int = 500
) : GameObject() {

    private var tagged = false

    // --- GameObject abstracts ---

    override val objects: MutableList<GameObject> get() = allObjects

    override val range: Pair<Float, Float>? get() = null
    override val attackSpeed: Float get() = 0f

    override val isAlive: Boolean get() = hp > 0

    override fun walk() {}
    override fun stopInPlace() {}
    override fun aimAt(target: GameObject) {}
    override fun aimDefault() {}

    // --- Location<Vector2> ---

    override fun getPosition(): Vector2 = body.position

    override fun getOrientation(): Float = body.angle

    override fun setOrientation(orientation: Float) {}

    override fun vectorToAngle(vector: Vector2): Float =
        MathUtils.atan2(-vector.x, vector.y)

    override fun angleToVector(outVector: Vector2, angle: Float): Vector2 =
        outVector.set(-MathUtils.sin(angle), MathUtils.cos(angle))

    override fun newLocation(): Location<Vector2> = SimpleLocation()

    // --- Steerable<Vector2> ---

    override fun getLinearVelocity(): Vector2 = Vector2.Zero
    override fun getAngularVelocity(): Float = 0f
    override fun getBoundingRadius(): Float = 8f // ~128px / 2 / PPM

    override fun isTagged(): Boolean = tagged
    override fun setTagged(tagged: Boolean) { this.tagged = tagged }

    // --- Limiter ---

    override fun getZeroLinearSpeedThreshold(): Float = 0f
    override fun setZeroLinearSpeedThreshold(value: Float) {}
    override fun getMaxLinearSpeed(): Float = 0f
    override fun setMaxLinearSpeed(value: Float) {}
    override fun getMaxLinearAcceleration(): Float = 0f
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
