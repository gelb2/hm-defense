package fr.mesabloo.heavymachdefense.ui.stage.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import fr.mesabloo.heavymachdefense.managers.FontManager
import fr.mesabloo.heavymachdefense.managers.fontManager
import fr.mesabloo.heavymachdefense.world.UI_HEIGHT
import fr.mesabloo.heavymachdefense.world.UI_WIDTH

/**
 * Full-screen overlay displayed when the game ends (victory or defeat).
 * Shows a dark semi-transparent background with centered text.
 * Fades in over time.
 */
class GameResultOverlay(isVictory: Boolean) : Group() {
    private val bgTexture: Texture
    private val font: BitmapFont = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_28_BLUE]!!
    private val text: String = if (isVictory) "VICTORY!" else "GAME OVER"
    private val layout: GlyphLayout = GlyphLayout(font, text)

    private var elapsed = 0f
    private val fadeInDuration = 0.8f

    init {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 1f)
        pixmap.fill()
        bgTexture = Texture(pixmap)
        pixmap.dispose()

        val bg = Image(TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(bgTexture)))
        bg.setSize(UI_WIDTH, UI_HEIGHT)
        addActor(bg)

        setSize(UI_WIDTH, UI_HEIGHT)
    }

    override fun act(delta: Float) {
        super.act(delta)
        elapsed += delta
        // Fade in the overlay
        val alpha = (elapsed / fadeInDuration).coerceAtMost(1f) * 0.75f
        color.a = alpha
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        // Draw text centered on screen
        val prevColor = font.color.cpy()
        font.color = Color(1f, 1f, 1f, color.a * parentAlpha / 0.75f)
        font.draw(
            batch, text,
            x + UI_WIDTH / 2f - layout.width / 2f,
            y + UI_HEIGHT / 2f + layout.height / 2f
        )
        font.color = prevColor
    }

    fun dispose() {
        bgTexture.dispose()
    }
}
