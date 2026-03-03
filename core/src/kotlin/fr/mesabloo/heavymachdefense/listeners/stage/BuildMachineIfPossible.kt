package fr.mesabloo.heavymachdefense.listeners.stage

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.data.Builds
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager
import fr.mesabloo.heavymachdefense.ui.stage.BuildMachineItem
import fr.mesabloo.heavymachdefense.ui.stage.BuildQueue
import fr.mesabloo.heavymachdefense.ui.stage.slots.BuildSlot
import fr.mesabloo.heavymachdefense.ui.stage.slots.MachineBuildSlot

import kotlin.reflect.KMutableProperty0

class BuildMachineIfPossible(
    private val slot: BuildSlot,
    private val cells: KMutableProperty0<Long>,
    private val buildQueue: BuildQueue,
    private val builds: Builds,
    private val upgradeMenuShown: KMutableProperty0<Boolean>
) :
    ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (!this.slot.isDisabled && !this.upgradeMenuShown.get()) {
            val item = when (this.slot) {
                is MachineBuildSlot -> BuildMachineItem(this.slot.kind, this.slot.level, this.builds)
                else -> return  // Turret build not yet implemented
            }

            if (this.cells.get() < this.slot.cellCost) return  // guard against fast double-tap
            this.cells.set(this.cells.get() - this.slot.cellCost)
            stageAssetsManager.playUiSound(StageAssetsManager.SOUND_BUILD_MACH)
            this.buildQueue.build(item)
        }
    }
}