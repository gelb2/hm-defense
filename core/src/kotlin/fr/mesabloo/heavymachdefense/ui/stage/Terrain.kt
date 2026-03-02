package fr.mesabloo.heavymachdefense.ui.stage

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager

class Terrain : Group() {
    val foregroundActors = mutableListOf<Actor>()

    /** When true, all gameplay actors (children) receive delta=0, freezing in place. */
    var gamePaused = false

    override fun act(delta: Float) {
        super.act(if (gamePaused) 0f else delta)
    }

    init {
        this.height = 2048f
        this.width = 512f

        val (bg1, bg2) = stageAssetsManager.background()!!

        this.addActor(Image(bg1).also {
            it.setPosition(0f, 0f)
        })
        this.addActor(Image(bg2).also {
            it.setPosition(0f, 1024f)
        })

        // Foreground overlays: rendered above units so they appear to pass behind buildings
        stageAssetsManager.foreground()?.let { (fg1, fg2) ->
            foregroundActors.add(Image(fg1).also {
                it.setPosition(0f, 0f)
                this.addActor(it)
            })
            foregroundActors.add(Image(fg2).also {
                it.setPosition(0f, 1024f)
                this.addActor(it)
            })
        }
    }
}
