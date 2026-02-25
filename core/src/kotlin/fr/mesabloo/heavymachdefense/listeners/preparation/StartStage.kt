package fr.mesabloo.heavymachdefense.listeners.preparation

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Timer
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.managers.assets.assetManager
import fr.mesabloo.heavymachdefense.managers.assets.buttonAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager
import fr.mesabloo.heavymachdefense.screens.AbstractScreen
import fr.mesabloo.heavymachdefense.screens.PreparationScreen
import fr.mesabloo.heavymachdefense.screens.StageScreen

class StartStage(private val screen: PreparationScreen) : ClickListener() {
    private fun stageScreen(game: MainGame) = StageScreen(
        game,
        screen.level,
        screen.save,
        screen.saveIndex,
        true
    )

    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        stageAssetsManager.preload(screen.level)
        buttonAssetsManager.preload()

        screen.addLoadingOverlay({
            if (!assetManager.isFinished)
                assetManager.update()
            buttonAssetsManager.isFullyLoaded() && stageAssetsManager.isFullyLoaded()
        }) {
            this@StartStage.screen.background
                .children.forEach { it.remove() }

            (this.changeScreen(stageScreen(this)) as AbstractScreen?)
                ?.addLoadingOverlayEnd()

            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    this@addLoadingOverlay.removeScreen<PreparationScreen>()?.dispose()
                    buttonAssetsManager.dispose()
                }
            }, 0.050f)
        }
    }
}
