package fr.mesabloo.heavymachdefense.listeners

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import fr.mesabloo.heavymachdefense.ui.common.InGameDialog

class ShowComingSoon(private val stage: Stage) : ClickListener() {
    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        InGameDialog.showComingSoon(this.stage)
    }
}
