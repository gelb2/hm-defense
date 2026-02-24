package fr.mesabloo.heavymachdefense.ui.stage

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.physics.box2d.Body
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.data.models.MachineModel
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager

class Machine(kind: MachineKind, level: Int) : Group() {
    var physicsBody: Body? = null

    var hp: Int = 100
    var maxHp: Int = 100
    var attackDamage: Int = 10
    var attackSpeed: Float = 1.0f
    var attackRange: Float = 120f
    var detectionRange: Float = 180f

    val isAlive: Boolean get() = hp > 0
    var walking: Boolean = true

    private var leftFoot: Image? = null
    private var rightFoot: Image? = null
    private var feetFrames: List<TextureRegion>? = null
    private var feetFramesFlipped: List<TextureRegion>? = null

    init {
        val oLevel = level.toString().padStart(2, '0')

        val model = MachineModel(kind.machineName, level)

        val body =
            Image(stageAssetsManager.unsafeRegion(StageAssetsManager.MACHINE_BODIES, "${kind.machineName}-$oLevel"))
        val weapon1 =
            Image(stageAssetsManager.unsafeRegion(StageAssetsManager.MACHINE_WEAPONS, "${kind.machineName}-$oLevel"))
        val weapon2 = Image(
            TextureRegion(stageAssetsManager.unsafeRegion(StageAssetsManager.MACHINE_WEAPONS, "${kind.machineName}-$oLevel")).also {
                it.flip(false, true)
            })

        this.width = body.width
        this.height = body.height

        // Feet (drawn behind body)
        if (model.feetRegions != null && model.feetOffset != null) {
            val frames = model.feetRegions.map {
                stageAssetsManager.unsafeRegion(StageAssetsManager.MACHINE_FEET, it)
            }
            this.feetFrames = frames
            this.feetFramesFlipped = frames.map { TextureRegion(it).also { r -> r.flip(false, true) } }

            this.leftFoot = Image(frames[0]).also {
                it.setPosition(
                    body.width / 2f + model.feetOffset.first - it.width / 2f,
                    body.height / 2f + model.feetOffset.second - it.height / 2f
                )
                it.isVisible = false
                this.addActor(it)
            }
            this.rightFoot = Image(TextureRegion(frames[0]).also { it.flip(false, true) }).also {
                it.setPosition(
                    body.width / 2f + model.feetOffset.first - it.width / 2f,
                    body.height / 2f - model.feetOffset.second - it.height / 2f
                )
                it.isVisible = false
                this.addActor(it)
            }
        }

        // Body (drawn on top of feet)
        this.addActor(body.also {
            it.setPosition(0f, 0f)
        })

        // Weapons (drawn on top of body)
        this.addActor(weapon1.also {
            it.setPosition(
                body.width / 2f + model.leftWeaponOffset.first - it.width / 2f,
                body.height / 2f + model.leftWeaponOffset.second - it.height / 2f
            )
            it.zIndex = 5000
        })
        this.addActor(weapon2.also {
            it.setPosition(
                body.width / 2f + model.rightWeaponOffset.first - it.width / 2f,
                body.height / 2f + model.rightWeaponOffset.second - it.height / 2f
            )
            it.zIndex = 5000
        })
    }

    /**
     * Starts the walking animation cycle for feet sprites.
     * Only call this for terrain machines, not UI preview slots.
     */
    /**
     * Starts step-based walking animation. Each cycle:
     * left foot step (burst forward + sway right) → pause → right foot step (burst forward + sway left) → pause.
     * Controls Box2D body velocity directly so the machine lurches forward with each step.
     *
     * @param moveSpeed average forward speed in pixels/sec
     */
    fun startWalkingAnimation(moveSpeed: Float) {
        val frames = this.feetFrames ?: return
        val framesFlipped = this.feetFramesFlipped ?: return
        val lFoot = this.leftFoot ?: return
        val rFoot = this.rightFoot ?: return
        val pBody = this.physicsBody ?: return

        lFoot.isVisible = true
        rFoot.isVisible = true

        this.addAction(object : Action() {
            private var elapsed = 0f
            private val frameDuration = 0.15f
            private val stepDuration = frames.size * frameDuration   // 0.45s for 3 frames
            private val pauseDuration = 0.25f
            private val halfCycle = stepDuration + pauseDuration     // 0.70s
            private val fullCycle = halfCycle * 2f                   // 1.40s
            // Burst speed compensates for pauses to maintain average moveSpeed
            private val burstSpeed = moveSpeed * fullCycle / (stepDuration * 2f)
            private val swayAmount = 2.5f

            override fun act(delta: Float): Boolean {
                val machine = actor as Machine
                if (!machine.isAlive) {
                    pBody.setLinearVelocity(0f, 0f)
                    lFoot.isVisible = false
                    rFoot.isVisible = false
                    return true // remove action on death
                }
                if (!machine.walking) {
                    pBody.setLinearVelocity(0f, 0f)
                    return false
                }

                elapsed += delta
                val cycleTime = elapsed % fullCycle

                when {
                    // Left foot step: animate left foot, move forward, sway right
                    cycleTime < stepDuration -> {
                        val phaseTime = cycleTime
                        val frameIdx = (phaseTime / frameDuration).toInt().coerceIn(0, frames.size - 1)
                        lFoot.drawable = TextureRegionDrawable(frames[frameIdx])

                        pBody.setLinearVelocity(0f, burstSpeed / PPM)

                        val progress = phaseTime / stepDuration
                        actor.x += MathUtils.sin(progress * MathUtils.PI) * swayAmount
                    }
                    // Pause after left step
                    cycleTime < halfCycle -> {
                        pBody.setLinearVelocity(0f, 0f)
                    }
                    // Right foot step: animate right foot, move forward, sway left
                    cycleTime < halfCycle + stepDuration -> {
                        val phaseTime = cycleTime - halfCycle
                        val frameIdx = (phaseTime / frameDuration).toInt().coerceIn(0, frames.size - 1)
                        rFoot.drawable = TextureRegionDrawable(framesFlipped[frameIdx])

                        pBody.setLinearVelocity(0f, burstSpeed / PPM)

                        val progress = phaseTime / stepDuration
                        actor.x -= MathUtils.sin(progress * MathUtils.PI) * swayAmount
                    }
                    // Pause after right step
                    else -> {
                        pBody.setLinearVelocity(0f, 0f)
                    }
                }

                return false
            }
        })
    }
}
