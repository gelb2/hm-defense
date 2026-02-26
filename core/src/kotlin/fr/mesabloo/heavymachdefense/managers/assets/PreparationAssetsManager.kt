package fr.mesabloo.heavymachdefense.managers.assets

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import fr.mesabloo.heavymachdefense.data.MachineKind
import ktx.assets.load

class PreparationAssetsManager : Disposable {
    companion object {
        const val MACHINE_BODIES = "gfx/models/machines/bodies.atlas"
        const val SELECT_BUTTONS = LevelSelectionAssetsManager.SELECT_BUTTONS
        const val BACKGROUND = LevelSelectionAssetsManager.BACKGROUND
        const val FOREGROUND = LevelSelectionAssetsManager.FOREGROUND
    }

    fun preload() {
        assetManager.load<TextureAtlas>(MACHINE_BODIES)
        assetManager.load<TextureAtlas>(SELECT_BUTTONS)
        assetManager.load<Texture>(BACKGROUND)
        assetManager.load<Texture>(FOREGROUND)
    }

    fun bodyRegion(kind: MachineKind, level: Int): TextureRegion {
        val oLevel = level.toString().padStart(2, '0')
        return assetManager.get<TextureAtlas>(MACHINE_BODIES)
            .findRegion("${kind.machineName}-$oLevel")
    }

    fun texture(path: String): TextureRegion = TextureRegion(assetManager.get<Texture>(path))

    fun isFullyLoaded(): Boolean =
        assetManager.isLoaded(MACHINE_BODIES) &&
                assetManager.isLoaded(SELECT_BUTTONS) &&
                assetManager.isLoaded(BACKGROUND) &&
                assetManager.isLoaded(FOREGROUND)

    override fun dispose() {
        assetManager.unload(MACHINE_BODIES)
        assetManager.unload(SELECT_BUTTONS)
        assetManager.unload(BACKGROUND)
        assetManager.unload(FOREGROUND)
    }
}

val preparationAssetsManager by lazy { PreparationAssetsManager() }
