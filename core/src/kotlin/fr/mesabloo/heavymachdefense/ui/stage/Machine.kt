package fr.mesabloo.heavymachdefense.ui.stage

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.data.models.MachineModel
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager

class Machine(kind: MachineKind, level: Int) : Group() {
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
            stageAssetsManager.unsafeRegion(StageAssetsManager.MACHINE_WEAPONS, "${kind.machineName}-$oLevel").also {
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
    fun startWalkingAnimation() {
        val frames = this.feetFrames ?: return
        val framesFlipped = this.feetFramesFlipped ?: return
        val lFoot = this.leftFoot ?: return
        val rFoot = this.rightFoot ?: return

        lFoot.isVisible = true
        rFoot.isVisible = true

        this.addAction(object : Action() {
            private var elapsed = 0f
            private val frameDuration = 0.15f

            override fun act(delta: Float): Boolean {
                elapsed += delta

                // Cycle feet textures
                val frameIdx = ((elapsed / frameDuration).toInt()) % frames.size
                val altIdx = (frameIdx + frames.size / 2) % frames.size
                lFoot.drawable = TextureRegionDrawable(frames[frameIdx])
                rFoot.drawable = TextureRegionDrawable(framesFlipped[altIdx])

                // Walking body sway (reset each frame by GameWorld.render())
                actor.x += MathUtils.sin(elapsed * 6f) * 1.0f
                actor.y += MathUtils.sin(elapsed * 12f).coerceAtLeast(0f) * 0.8f

                return false
            }
        })
    }
}
