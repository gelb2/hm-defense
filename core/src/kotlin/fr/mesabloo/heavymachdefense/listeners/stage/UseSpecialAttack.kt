package fr.mesabloo.heavymachdefense.listeners.stage

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.ui.stage.slots.SpecialBuildSlot
import kotlin.reflect.KMutableProperty0

class UseSpecialAttack(
    private val slot: SpecialBuildSlot,
    private val upgradeMenuShown: KMutableProperty0<Boolean>,
    private val onUse: (SpecialBuildSlot) -> Unit
) : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (!this.slot.isDisabled && !this.upgradeMenuShown.get()) {
            this.onUse(this.slot)
        }
    }
}
