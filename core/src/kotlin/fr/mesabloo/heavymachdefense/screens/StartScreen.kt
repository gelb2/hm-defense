package fr.mesabloo.heavymachdefense.screens

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import aurelienribon.tweenengine.equations.Cubic
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.scenes.scene2d.Actor
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.listeners.start.StartOnClick
import fr.mesabloo.heavymachdefense.managers.BackgroundMusicManager
import fr.mesabloo.heavymachdefense.managers.assets.startAssetsManager
import fr.mesabloo.heavymachdefense.managers.backgroundMusicManager
import fr.mesabloo.heavymachdefense.tweens.MusicAccessor
import fr.mesabloo.heavymachdefense.tweens.MusicAccessor.Companion.FADE_IN
import fr.mesabloo.heavymachdefense.ui.start.Background
import fr.mesabloo.heavymachdefense.ui.start.TapToStart

class StartScreen(game: MainGame, isLoading: Boolean = false) : AbstractScreen(game, isLoading) {
    private companion object {
        const val MAX_BACKGROUND_VOLUME = 0.6f
    }

    lateinit var tapToStart: Actor
        private set
    lateinit var backgroundImage: Actor
        private set
    val backgroundMusic: Music = backgroundMusicManager.load(BackgroundMusicManager.INTRO).also {
        it.volume = 1f
        it.isLooping = true
        it.position = 0f
    }

    private val tweenManager = TweenManager()

    init {
        // Tween engine needs concrete Music classes, not the Music interface.
        // Register all known backend implementations via reflection for platform independence.
        val musicClasses = listOf(
            "com.badlogic.gdx.backends.lwjgl3.audio.Mp3\$Music",
            "com.badlogic.gdx.backends.lwjgl3.audio.Ogg\$Music",
            "com.badlogic.gdx.backends.lwjgl3.audio.Wav\$Music",
            "com.badlogic.gdx.backends.android.AndroidMusic"
        )
        @Suppress("UNCHECKED_CAST")
        val accessor = MusicAccessor<com.badlogic.gdx.audio.Music>() as aurelienribon.tweenengine.TweenAccessor<*>
        for (className in musicClasses) {
            try {
                Tween.registerAccessor(Class.forName(className), accessor)
            } catch (_: ClassNotFoundException) { }
        }
    }

    override fun show() {
        super.show()

        if (this.isLoading)
            return

        this.background.addActor(Background().also { this.backgroundImage = it })
        this.background.addActor(TapToStart().also { this.tapToStart = it })

        this.backgroundImage.addListener(StartOnClick(this@StartScreen))

        this.backgroundMusic.play()
        Tween.to(this.backgroundMusic, FADE_IN, 0.5f)
            .target(MAX_BACKGROUND_VOLUME)
            .ease(Cubic.IN)
            .start(this.tweenManager)
    }

    override fun render(delta: Float) {
        super.render(delta)

        this.tweenManager.update(delta)
    }

    override fun dispose() {
        super.dispose()

        startAssetsManager.dispose()
        this.backgroundMusic.stop()
        this.backgroundMusic.dispose()
    }
}