package fr.mesabloo.heavymachdefense.listeners.preparation

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Timer
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.managers.assets.assetManager
import fr.mesabloo.heavymachdefense.managers.assets.levelSelectionAssetsManager
import fr.mesabloo.heavymachdefense.screens.AbstractScreen
import fr.mesabloo.heavymachdefense.screens.PreparationScreen
import fr.mesabloo.heavymachdefense.screens.StageSelectionScreen

class BackToStageSelection(private val screen: PreparationScreen) : ClickListener() {
    private fun stageSelectionScreen(game: MainGame) = StageSelectionScreen(
        game,
        screen.save,
        screen.saveIndex,
        true
    )

    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        levelSelectionAssetsManager.preload()

        screen.addLoadingOverlay({
            if (!assetManager.isFinished)
                assetManager.update()
            levelSelectionAssetsManager.isFullyLoaded()
        }) {
            this@BackToStageSelection.screen.background
                .children.forEach { it.remove() }

            (this.changeScreen(stageSelectionScreen(this)) as AbstractScreen?)
                ?.addLoadingOverlayEnd()

            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    this@addLoadingOverlay.removeScreen<PreparationScreen>()?.dispose()
                }
            }, 0.050f)
        }
    }
}
