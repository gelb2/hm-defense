package fr.mesabloo.heavymachdefense.ui.stage.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import fr.mesabloo.heavymachdefense.world.UI_HEIGHT
import fr.mesabloo.heavymachdefense.world.UI_WIDTH

/**
 * Full-screen victory illustration displayed after stage clear.
 * Shows the game's title illustration with "STAGE CLEAR" text overlay.
 */
class VictoryIllustration : Group() {
    private val titleTexture: Texture = Texture(Gdx.files.internal("gfx/ui/splash/title.jpg"))

    private var elapsed = 0f
    private val fadeInDuration = 0.6f

    init {
        setSize(UI_WIDTH, UI_HEIGHT)

        val bg = Image(TextureRegionDrawable(TextureRegion(titleTexture)))
        bg.setSize(UI_WIDTH, UI_HEIGHT)
        addActor(bg)
    }

    override fun act(delta: Float) {
        super.act(delta)
        elapsed += delta
        color.a = (elapsed / fadeInDuration).coerceAtMost(1f)
    }

    fun dispose() {
        titleTexture.dispose()
    }
}
