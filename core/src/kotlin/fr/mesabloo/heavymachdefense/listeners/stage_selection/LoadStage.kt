package fr.mesabloo.heavymachdefense.listeners.stage_selection

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Timer
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.managers.assets.assetManager
import fr.mesabloo.heavymachdefense.managers.assets.buttonAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.preparationAssetsManager
import fr.mesabloo.heavymachdefense.screens.AbstractScreen
import fr.mesabloo.heavymachdefense.screens.PreparationScreen
import fr.mesabloo.heavymachdefense.screens.StageSelectionScreen

class LoadStage(private val screen: StageSelectionScreen) : ClickListener() {
    private fun preparationScreen(game: MainGame, index: Int) = PreparationScreen(
        game,
        index + 1,
        this.screen.save,
        this.screen.saveIndex,
        true
    )

    override fun clicked(event: InputEvent?, x: Float, y: Float) {
        val index = this.screen.scrollPane.selected

        this.screen.scrollPane.touchable = Touchable.disabled

        preparationAssetsManager.preload()
        buttonAssetsManager.preload()

        this.screen.addLoadingOverlay({
            if (!assetManager.isFinished)
                assetManager.update()
            buttonAssetsManager.isFullyLoaded() && preparationAssetsManager.isFullyLoaded()
        }) {
            this@LoadStage.screen.background
                .children.forEach { it.remove() }

            (this.changeScreen(preparationScreen(this, index)) as AbstractScreen?)
                ?.addLoadingOverlayEnd()

            Timer.schedule(object: Timer.Task() {
                override fun run() {
                    this@addLoadingOverlay.removeScreen<StageSelectionScreen>()?.dispose()
                }
            }, 0.050f)
        }
    }
}