package fr.mesabloo.heavymachdefense.ui.common

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import fr.mesabloo.heavymachdefense.managers.FontManager
import fr.mesabloo.heavymachdefense.managers.fontManager

object InGameDialog {
    private var pixelTexture: Texture? = null

    private fun getPixelTexture(): Texture {
        if (pixelTexture == null) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            pixelTexture = Texture(pixmap)
            pixmap.dispose()
        }
        return pixelTexture!!
    }

    private fun colorDrawable(color: Color): Drawable {
        return TextureRegionDrawable(TextureRegion(getPixelTexture())).tint(color)
    }

    fun showTextInput(
        stage: Stage,
        title: String,
        hint: String,
        onConfirm: (String) -> Unit
    ) {
        val font = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!

        val overlay = Table()
        overlay.setFillParent(true)
        overlay.background = colorDrawable(Color(0f, 0f, 0f, 0.6f))

        val dialog = Table()
        dialog.background = colorDrawable(Color(0.15f, 0.15f, 0.2f, 0.95f))
        dialog.pad(24f)

        // Title
        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        val titleLabel = Label(title, labelStyle)
        dialog.add(titleLabel).padBottom(16f).row()

        // Text field
        val tfStyle = TextField.TextFieldStyle()
        tfStyle.font = font
        tfStyle.fontColor = Color.WHITE
        tfStyle.background = colorDrawable(Color(0.25f, 0.25f, 0.3f, 1f))
        tfStyle.background.leftWidth = 8f
        tfStyle.background.rightWidth = 8f
        tfStyle.background.topHeight = 6f
        tfStyle.background.bottomHeight = 6f
        tfStyle.cursor = colorDrawable(Color.WHITE)
        tfStyle.cursor.minWidth = 2f
        tfStyle.focusedBackground = colorDrawable(Color(0.3f, 0.3f, 0.4f, 1f))
        tfStyle.focusedBackground.leftWidth = 8f
        tfStyle.focusedBackground.rightWidth = 8f
        tfStyle.focusedBackground.topHeight = 6f
        tfStyle.focusedBackground.bottomHeight = 6f
        tfStyle.messageFont = font
        tfStyle.messageFontColor = Color(0.6f, 0.6f, 0.6f, 1f)

        val textField = TextField("", tfStyle)
        textField.messageText = hint
        textField.maxLength = 20
        dialog.add(textField).width(320f).height(36f).padBottom(20f).row()

        // Buttons
        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.fontColor = Color.WHITE
        btnStyle.up = colorDrawable(Color(0.3f, 0.5f, 0.8f, 1f))
        btnStyle.down = colorDrawable(Color(0.2f, 0.4f, 0.7f, 1f))
        btnStyle.up.leftWidth = 12f
        btnStyle.up.rightWidth = 12f
        btnStyle.up.topHeight = 8f
        btnStyle.up.bottomHeight = 8f
        btnStyle.down.leftWidth = 12f
        btnStyle.down.rightWidth = 12f
        btnStyle.down.topHeight = 8f
        btnStyle.down.bottomHeight = 8f

        val cancelStyle = TextButton.TextButtonStyle(btnStyle)
        cancelStyle.up = colorDrawable(Color(0.4f, 0.4f, 0.4f, 1f))
        cancelStyle.down = colorDrawable(Color(0.3f, 0.3f, 0.3f, 1f))
        cancelStyle.up.leftWidth = 12f
        cancelStyle.up.rightWidth = 12f
        cancelStyle.up.topHeight = 8f
        cancelStyle.up.bottomHeight = 8f
        cancelStyle.down.leftWidth = 12f
        cancelStyle.down.rightWidth = 12f
        cancelStyle.down.topHeight = 8f
        cancelStyle.down.bottomHeight = 8f

        val btnTable = Table()
        val okBtn = TextButton("OK", btnStyle)
        val cancelBtn = TextButton("Cancel", cancelStyle)

        okBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val text = textField.text
                overlay.remove()
                if (text.isNotBlank()) {
                    onConfirm(text)
                }
            }
        })

        cancelBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                overlay.remove()
            }
        })

        btnTable.add(okBtn).width(120f).padRight(16f)
        btnTable.add(cancelBtn).width(120f)
        dialog.add(btnTable)

        overlay.add(dialog)
        stage.addActor(overlay)
        stage.keyboardFocus = textField
    }

    fun showConfirm(
        stage: Stage,
        message: String,
        onConfirm: () -> Unit
    ) {
        val font = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!

        val overlay = Table()
        overlay.setFillParent(true)
        overlay.background = colorDrawable(Color(0f, 0f, 0f, 0.6f))

        val dialog = Table()
        dialog.background = colorDrawable(Color(0.15f, 0.15f, 0.2f, 0.95f))
        dialog.pad(24f)

        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        val msgLabel = Label(message, labelStyle)
        dialog.add(msgLabel).padBottom(20f).row()

        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.fontColor = Color.WHITE
        btnStyle.up = colorDrawable(Color(0.3f, 0.5f, 0.8f, 1f))
        btnStyle.down = colorDrawable(Color(0.2f, 0.4f, 0.7f, 1f))
        btnStyle.up.leftWidth = 12f
        btnStyle.up.rightWidth = 12f
        btnStyle.up.topHeight = 8f
        btnStyle.up.bottomHeight = 8f
        btnStyle.down.leftWidth = 12f
        btnStyle.down.rightWidth = 12f
        btnStyle.down.topHeight = 8f
        btnStyle.down.bottomHeight = 8f

        val cancelStyle = TextButton.TextButtonStyle(btnStyle)
        cancelStyle.up = colorDrawable(Color(0.4f, 0.4f, 0.4f, 1f))
        cancelStyle.down = colorDrawable(Color(0.3f, 0.3f, 0.3f, 1f))
        cancelStyle.up.leftWidth = 12f
        cancelStyle.up.rightWidth = 12f
        cancelStyle.up.topHeight = 8f
        cancelStyle.up.bottomHeight = 8f
        cancelStyle.down.leftWidth = 12f
        cancelStyle.down.rightWidth = 12f
        cancelStyle.down.topHeight = 8f
        cancelStyle.down.bottomHeight = 8f

        val btnTable = Table()
        val yesBtn = TextButton("Yes", btnStyle)
        val noBtn = TextButton("No", cancelStyle)

        yesBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                overlay.remove()
                onConfirm()
            }
        })

        noBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                overlay.remove()
            }
        })

        btnTable.add(yesBtn).width(120f).padRight(16f)
        btnTable.add(noBtn).width(120f)
        dialog.add(btnTable)

        overlay.add(dialog)
        stage.addActor(overlay)
    }
}
