package fr.mesabloo.heavymachdefense.listeners.saves

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.screens.SavesSelectionScreen
import fr.mesabloo.heavymachdefense.ui.common.InGameDialog

class DeleteSave(private val screen: SavesSelectionScreen) : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (this.screen.focusedIndex != -1) {
            InGameDialog.showConfirm(
                stage = this.screen.ui,
                message = "Do you really want to delete this save?"
            ) {
                val saveIndex = screen.focusedIndex
                if (saveIndex != -1)
                    screen.removeSaveFromIndex(saveIndex)
            }
        }
    }
}
