package fr.mesabloo.heavymachdefense.ui.stage

import com.badlogic.gdx.scenes.scene2d.ui.Label
import fr.mesabloo.heavymachdefense.managers.FontManager
import fr.mesabloo.heavymachdefense.managers.WaveManager
import fr.mesabloo.heavymachdefense.managers.fontManager

class WaveCounter(private val waveManager: WaveManager) :
    Label("WAVE 1", LabelStyle(fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE], null)) {

    override fun act(delta: Float) {
        super.act(delta)
        this.setText("WAVE ${waveManager.currentWave}")
    }
}
