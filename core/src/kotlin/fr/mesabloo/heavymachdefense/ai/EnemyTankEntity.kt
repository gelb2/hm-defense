package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.utils.Location
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.ui.stage.EnemyTank

class EnemyTankEntity(
    val tank: EnemyTank,
    val body: Body,
    private val allObjects: MutableList<GameObject>,
    private val moveSpeed: Float
) : GameObject() {

    private var tagged = false

    // --- GameObject abstracts ---

    override val objects: MutableList<GameObject> get() = allObjects
    override val team: Team = Team.ENEMY

    override val range: Pair<Float, Float>?
        get() = Pair(tank.attackRange / PPM, tank.detectionRange / PPM)

    override val attackSpeed: Float get() = tank.attackSpeed

    override val isAlive: Boolean get() = tank.isAlive

    override fun walk() {
        tank.walking = true
        body.setLinearVelocity(0f, -moveSpeed / PPM) // walk DOWNWARD
    }

    override fun stopInPlace() {
        tank.walking = false
        body.setLinearVelocity(0f, 0f)
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
