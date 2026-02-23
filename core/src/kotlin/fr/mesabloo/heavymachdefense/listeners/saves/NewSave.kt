package fr.mesabloo.heavymachdefense.listeners.saves

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.data.GameSave
import fr.mesabloo.heavymachdefense.screens.SavesSelectionScreen
import fr.mesabloo.heavymachdefense.ui.common.InGameDialog
import java.util.*

class NewSave(private val screen: SavesSelectionScreen) : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        if (this.screen.numberOfSaves < 5) {
            InGameDialog.showTextInput(
                stage = this.screen.ui,
                title = "Enter your username",
                hint = "Username"
            ) { text ->
                val save = GameSave(creationDate = Date(), lastAccessedDate = Date())
                save.name = text
                screen.addSave(save)
            }
        }
    }
}
