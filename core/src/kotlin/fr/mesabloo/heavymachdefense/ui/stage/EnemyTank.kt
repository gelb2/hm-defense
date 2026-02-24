package fr.mesabloo.heavymachdefense.ui.stage

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager

class EnemyTank(tankTypeId: String) : Group() {
    var physicsBody: Body? = null

    var hp: Int = 100
    var maxHp: Int = 100
    var attackDamage: Int = 10
    var attackSpeed: Float = 1.0f
    var attackRange: Float = 100f
    var detectionRange: Float = 160f

    val isAlive: Boolean get() = hp > 0
    var walking: Boolean = true

    val weaponImage: Image

    init {
        val bodyRegion = stageAssetsManager.unsafeRegion(StageAssetsManager.TANK_BODIES, tankTypeId)
        val bodyImage = Image(bodyRegion)

        this.width = bodyImage.width
        this.height = bodyImage.height

        this.addActor(bodyImage.also {
            it.setPosition(0f, 0f)
        })

        val weaponRegion = stageAssetsManager.unsafeRegion(StageAssetsManager.TANK_WEAPONS, tankTypeId)
        this.weaponImage = Image(weaponRegion)
        this.addActor(weaponImage.also {
            it.setPosition(
                bodyImage.width / 2f - it.width / 2f,
                bodyImage.height / 2f - it.height / 2f
            )
            it.setOrigin(it.width / 2f, it.height / 2f)
            it.zIndex = 5000
        })
    }
}
