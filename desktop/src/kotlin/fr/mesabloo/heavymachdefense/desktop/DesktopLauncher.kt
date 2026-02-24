package fr.mesabloo.heavymachdefense.desktop

import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import fr.mesabloo.heavymachdefense.MainGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration()
    config.setIdleFPS(30)
    config.setForegroundFPS(60)
    config.setWindowedMode(768, 1024)
    config.setTitle("Heavy MACH: Defense")
    config.setWindowIcon(Files.FileType.Internal, "gfx/nIcon57.png")

    config.setWindowListener(object : Lwjgl3WindowAdapter() {
        override fun closeRequested(): Boolean {
            com.badlogic.gdx.Gdx.app.exit()
            return false
        }
    })

    Lwjgl3Application(MainGame(), config)
}
