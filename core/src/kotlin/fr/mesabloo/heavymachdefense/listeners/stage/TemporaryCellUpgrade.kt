package fr.mesabloo.heavymachdefense.listeners.stage

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager
import kotlin.reflect.KMutableProperty0

class TemporaryCellUpgrade(
    private val currentCells: KMutableProperty0<Long>,
    private val tempUpgradeCost: KMutableProperty0<Long>,
    private val tempUpgradeCount: KMutableProperty0<Long>
) : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        val currentCells = this.currentCells.get()
        val tempUpgradeCost = this.tempUpgradeCost.get()

        if (currentCells >= tempUpgradeCost) {
            this.currentCells.set(currentCells - tempUpgradeCost)
            // NOTE: no need to upgrade `tempUpgradeCost`
            this.tempUpgradeCount.set(this.tempUpgradeCount.get() + 1)
            stageAssetsManager.playUiSound(StageAssetsManager.SOUND_UPGRADE_BASE)
        }
    }
}