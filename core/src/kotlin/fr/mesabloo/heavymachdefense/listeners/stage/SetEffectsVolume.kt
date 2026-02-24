package fr.mesabloo.heavymachdefense.listeners.stage

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager
import kotlin.reflect.KMutableProperty0

class SetEffectsVolume(private val effectsVolume: KMutableProperty0<Float>) : ChangeListener() {
    override fun changed(event: ChangeEvent?, actor: Actor?) {
        val volume = (actor as Slider).value
        effectsVolume.set(volume)
        stageAssetsManager.effectsVolume = volume
    }
}