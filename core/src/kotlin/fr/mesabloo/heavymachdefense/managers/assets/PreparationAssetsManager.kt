package fr.mesabloo.heavymachdefense.managers.assets

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import fr.mesabloo.heavymachdefense.data.MachineKind
import ktx.assets.load

class PreparationAssetsManager : Disposable {
    companion object {
        const val MACHINE_BODIES = "gfx/models/machines/bodies.atlas"
        const val SELECT_BUTTONS = LevelSelectionAssetsManager.SELECT_BUTTONS
    }

    fun preload() {
        assetManager.load<TextureAtlas>(MACHINE_BODIES)
        assetManager.load<TextureAtlas>(SELECT_BUTTONS)
    }

    fun bodyRegion(kind: MachineKind, level: Int): TextureRegion {
        val oLevel = level.toString().padStart(2, '0')
        return assetManager.get<TextureAtlas>(MACHINE_BODIES)
            .findRegion("${kind.machineName}-$oLevel")
    }

    fun isFullyLoaded(): Boolean =
        assetManager.isLoaded(MACHINE_BODIES) && assetManager.isLoaded(SELECT_BUTTONS)

    override fun dispose() {
        assetManager.unload(MACHINE_BODIES)
        assetManager.unload(SELECT_BUTTONS)
    }
}

val preparationAssetsManager by lazy { PreparationAssetsManager() }
