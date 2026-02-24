package fr.mesabloo.heavymachdefense.managers.assets

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.data.UpgradeKind
import fr.mesabloo.heavymachdefense.data.getBackgroundForLevel
import ktx.assets.load

class StageAssetsManager : Disposable {
    private var stageLevel: Int? = null
    var effectsVolume: Float = 1.0f

    companion object {
        private fun backgrounds(level: Int): Pair<String, String> =
            getBackgroundForLevel(level).let {
                val nb = it.toString().padStart(2, '0')
                Pair("gfx/terrains/$nb/01.jpg", "gfx/terrains/$nb/02.jpg")
            }

        const val MACHINE_BODIES = "gfx/models/machines/bodies.atlas"
        const val MACHINE_WEAPONS = "gfx/models/machines/weapons.atlas"
        const val MACHINE_FEET = "gfx/models/machines/feet.atlas"

        const val ALLY_TURRET_BODIES = "gfx/models/turrets/ally-bodies.atlas"
        const val ALLY_TURRET_WEAPONS = "gfx/models/turrets/ally-weapons.atlas"
        const val ENEMY_TURRET_BODIES = "gfx/models/turrets/enemy-bodies.atlas"
        const val ENEMY_TURRET_WEAPONS = "gfx/models/turrets/enemy-weapons.atlas"

        const val TANK_BODIES = "gfx/models/tanks/enemy-bodies.atlas"
        const val TANK_WEAPONS = "gfx/models/tanks/enemy-weapons.atlas"

        const val ALLY_PLANE = "gfx/models/planes/plane.png"
        const val ENEMY_PLANE = "gfx/models/planes/enemy-planes.atlas"

        const val SHIPS = "gfx/models/other/ships.atlas"

        const val ALLY_BULLETS = "gfx/models/bullets/ally.atlas"
        const val SHELL_BULLETS = "gfx/models/bullets/shell.atlas"
        const val ENEMY_BULLETS = "gfx/models/bullets/enemy-bullet.atlas"

        const val EFFECTS = "gfx/models/effects/effects.atlas"

        val WEAPON_SOUNDS: Map<MachineKind, List<String>> = mapOf(
            MachineKind.RIFLE to (1..10).map { "sfx/machine/rifle/%02d.wav".format(it) },
            MachineKind.HMG to (1..10).map { "sfx/machine/hmg/%02d.wav".format(it) },
            MachineKind.MISSILE to (1..10).map { "sfx/machine/missile/%02d.wav".format(it) },
            MachineKind.PLASMA to (1..5).map { "sfx/machine/plasma/%02d.wav".format(it) },
            MachineKind.ION to (1..3).map { "sfx/machine/ion/%02d.wav".format(it) },
            MachineKind.SHOTGUN to (1..3).map { "sfx/machine/shotgun/%02d.wav".format(it) },
            MachineKind.HEAVY_MISSILE to listOf("sfx/weapon/missile.wav"),
            MachineKind.TANKER to listOf("sfx/weapon/cannon.wav")
        )

        private val ALL_WEAPON_SOUND_PATHS = WEAPON_SOUNDS.values.flatten().distinct()

        const val SOUND_ENEMY_FIRE = "sfx/weapon/gun.wav"
        const val SOUND_GROUND_EXPLOSION = "sfx/effects/ground-explosion.wav"
        const val SOUND_MACH_EXPLOSION = "sfx/effects/mach-explosion.wav"
        val SOUND_BODY_HITS: List<String> = (1..4).map { "sfx/effects/body-hit-%02d.wav".format(it) }

        private val ALL_COMBAT_SOUND_PATHS = listOf(SOUND_ENEMY_FIRE, SOUND_GROUND_EXPLOSION, SOUND_MACH_EXPLOSION) + SOUND_BODY_HITS

        // UI / production sounds
        const val SOUND_BUILD_MACH = "sfx/ui/button-build-mach.wav"
        const val SOUND_BUILD_STARTED = "sfx/game/build-started.wav"
        const val SOUND_BUILD_COMPLETE = "sfx/game/build-complete.wav"
        const val SOUND_UPGRADE_BASE = "sfx/game/upgrade-base.wav"
        const val SOUND_CLICK = "sfx/ui/click.wav"
        const val SOUND_BUTTON_OK = "sfx/ui/button-ok.wav"
        const val SOUND_BUTTON_CANCEL = "sfx/ui/button-cancel.wav"
        const val SOUND_BUTTON_FORMATION = "sfx/ui/button-formation.wav"
        const val SOUND_BUTTON_SPECIAL = "sfx/ui/button-special.wav"
        const val SOUND_LOW_HP = "sfx/game/low-hp.wav"

        private val ALL_UI_SOUND_PATHS = listOf(
            SOUND_BUILD_MACH, SOUND_BUILD_STARTED, SOUND_BUILD_COMPLETE,
            SOUND_UPGRADE_BASE, SOUND_CLICK, SOUND_BUTTON_OK, SOUND_BUTTON_CANCEL,
            SOUND_BUTTON_FORMATION, SOUND_BUTTON_SPECIAL, SOUND_LOW_HP
        )

        const val ALLY_BASE = "gfx/models/base/ally-base.atlas"
        const val ENEMY_BASE = "gfx/models/base/enemy-base.atlas"
    }

    //////////////////////////////////////////////

    object UI {
        const val BUILD_SLOT = "gfx/ui/game/build-slot.png"
        const val MACH_SLOT = "gfx/ui/game/mach-slot.png"
        const val CONTROLS = "gfx/ui/game/controls.png"
        const val HP_GAUGE = "gfx/ui/game/hp-gauge.png"
        const val BUTTONS = "gfx/ui/buttons/stage.atlas"
        const val TITLE = "gfx/ui/game/title.atlas"
        const val MENU_BUTTONS = "gfx/ui/game/menu-buttons.atlas"
        const val RADAR_MARKS = "gfx/ui/game/radar/marks.atlas"
        const val RADAR_BACKGROUND = "gfx/ui/game/radar/background.png"
        const val RADAR_BORDER = "gfx/ui/game/radar/border.png"
        const val PLAYER_HP_GAUGE = "gfx/ui/game/player-hp-gauge.png"
        const val ENEMY_HP_GAUGE = "gfx/ui/game/enemy-hp-gauge.png"
        const val UPGRADE_MENU_BUTTONS = "gfx/ui/buttons/base-upgrades.atlas"
        const val UPGRADE_MENU_LEFT = "gfx/ui/game/upgrades-left.png"
        const val UPGRADE_MENU_RIGHT = "gfx/ui/game/upgrades-right.png"
        const val UPGRADE_CELL = "gfx/ui/game/upgrade-cell.png"

        const val BUILD_SLOT_FOREGROUND = "gfx/ui/game/build-slot/foreground.png"
        const val BUILD_SLOT_BACKGROUND = "gfx/ui/game/build-slot/background.png"
        const val BUILD_SLOT_INDICATOR_BLUE = "gfx/ui/game/build-slot/indicator-blue.png"
        const val BUILD_SLOT_INDICATOR_RED = "gfx/ui/game/build-slot/indicator-red.png"
        const val BUILD_SLOT_COVER = "gfx/ui/game/build-slot/cover.png"

        const val UPGRADE_EQUIPMENT = "gfx/ui/buttons/upgrade-equip.atlas"

        const val BUILD_QUEUE_ITEM_BACKGROUND = "gfx/ui/game/build-queue/background.png"
        const val BUILD_QUEUE_GAUGE = "gfx/ui/game/build-queue/gauge.png"

        const val SPECIAL_ICONS = "gfx/ui/game/build-slot/icons-special.atlas"

        enum class ButtonKind(internal val str: String) {
            MENU("menu"),
            BASE_UPGRADE("base-upgrade")
        }

        enum class TitleKind(internal val str: String) {
            BUILD_MACH("build-mach"),
            SPECIAL_ATTACK("special-attack")
        }

        fun button(kind: ButtonKind, isSelected: Boolean): TextureRegion =
            assetManager.get<TextureAtlas>(BUTTONS)
                .findRegion("${kind.str}${if (isSelected) "-selected" else ""}")

        fun title(kind: TitleKind): TextureRegion =
            assetManager.get<TextureAtlas>(TITLE)
                .findRegion(kind.str)

        fun unsafeAnimationTile(path: String, tile: String): TextureRegion =
            assetManager.get<TextureAtlas>(path).findRegion(tile)

        fun upgradeButton(kind: UpgradeKind, isSelected: Boolean, isDisabled: Boolean): TextureRegion =
            assetManager.get<TextureAtlas>(UPGRADE_MENU_BUTTONS)
                .findRegion("${kind.str}-${if (isSelected) "selected" else if (isDisabled) "disabled" else "normal"}")
    }

    object Dialog {
        fun unsafeTexture(buttonAtlas: String, regionName: String): TextureRegion =
            assetManager.get<TextureAtlas>(buttonAtlas).findRegion(regionName)

        const val SYSTEM_MENU_BACKGROUND = "gfx/ui/dialog/sys-menu/background.png"
        const val SYSTEM_MENU_BUTTONS = "gfx/ui/buttons/sys-menu.atlas"
        const val SLIDER_RULE = "gfx/ui/dialog/sys-menu/slider-rule.png"
        const val SLIDER_THUMB = "gfx/ui/dialog/sys-menu/slider-thumb.png"
        const val VOLUME_OPTIONS = "gfx/ui/dialog/sys-menu/volume-options.png"
    }

    private fun allAtlases() = listOf(
        MACHINE_BODIES,
        MACHINE_WEAPONS,
        MACHINE_FEET,
        ALLY_TURRET_BODIES,
        ALLY_TURRET_WEAPONS,
        ENEMY_TURRET_BODIES,
        ENEMY_TURRET_WEAPONS,
        TANK_BODIES,
        TANK_WEAPONS,
        ENEMY_PLANE,
        SHIPS,
        ALLY_BULLETS,
        SHELL_BULLETS,
        ENEMY_BULLETS,
        ALLY_BASE,
        ENEMY_BASE,
        UI.BUTTONS,
        UI.TITLE,
        UI.MENU_BUTTONS,
        UI.RADAR_MARKS,
        UI.UPGRADE_MENU_BUTTONS,
        Dialog.SYSTEM_MENU_BUTTONS,
        UI.UPGRADE_EQUIPMENT,
        UI.SPECIAL_ICONS,
        EFFECTS
    )

    private fun allTextures() = this.stageLevel?.let {
        val (bg1, bg2) = backgrounds(it)

        listOf(
            bg1,
            bg2,
            ALLY_PLANE,
            UI.BUILD_SLOT,
            UI.MACH_SLOT,
            UI.CONTROLS,
            UI.HP_GAUGE,
            UI.RADAR_BORDER,
            UI.RADAR_BACKGROUND,
            UI.PLAYER_HP_GAUGE,
            UI.ENEMY_HP_GAUGE,
            UI.UPGRADE_MENU_LEFT,
            UI.UPGRADE_MENU_RIGHT,
            UI.UPGRADE_CELL,
            Dialog.SYSTEM_MENU_BACKGROUND,
            Dialog.VOLUME_OPTIONS,
            Dialog.SLIDER_THUMB,
            Dialog.SLIDER_RULE,
            UI.BUILD_SLOT_FOREGROUND,
            UI.BUILD_SLOT_BACKGROUND,
            UI.BUILD_SLOT_INDICATOR_BLUE,
            UI.BUILD_SLOT_INDICATOR_RED,
            UI.BUILD_SLOT_COVER,
            UI.BUILD_QUEUE_ITEM_BACKGROUND,
            UI.BUILD_QUEUE_GAUGE
        )
    } ?: listOf()

    fun preload(level: Int) {
        this.stageLevel = level

        this.allTextures().forEach { assetManager.load<Texture>(it) }
        this.allAtlases().forEach { assetManager.load<TextureAtlas>(it) }
        ALL_WEAPON_SOUND_PATHS.forEach { assetManager.load<Sound>(it) }
        ALL_COMBAT_SOUND_PATHS.forEach { assetManager.load<Sound>(it) }
        ALL_UI_SOUND_PATHS.forEach { assetManager.load<Sound>(it) }
    }

    fun isFullyLoaded(): Boolean =
        this.stageLevel != null
                && this.allTextures().all { assetManager.isLoaded(it) }
                && this.allAtlases().all { assetManager.isLoaded(it) }
                && ALL_WEAPON_SOUND_PATHS.all { assetManager.isLoaded(it) }
                && ALL_COMBAT_SOUND_PATHS.all { assetManager.isLoaded(it) }
                && ALL_UI_SOUND_PATHS.all { assetManager.isLoaded(it) }

    fun get(path: String): TextureRegion = TextureRegion(assetManager.get<Texture>(path))

    fun unsafeRegion(path: String, regionName: String): TextureRegion =
        assetManager.get<TextureAtlas>(path).findRegion(regionName)

    fun safeRegion(path: String, regionName: String): TextureRegion? =
        assetManager.get<TextureAtlas>(path).findRegion(regionName)

    fun randomWeaponSound(kind: MachineKind): Sound {
        val paths = WEAPON_SOUNDS[kind]!!
        val path = paths.random()
        return assetManager.get(path)
    }

    fun sound(path: String): Sound = assetManager.get(path)

    fun randomBodyHitSound(): Sound = assetManager.get(SOUND_BODY_HITS.random())

    override fun dispose() {
        if (this.stageLevel != null) {
            val (bg1, bg2) = backgrounds(this.stageLevel!!)

            assetManager.unload(bg1)
            assetManager.unload(bg2)
        }
        ALL_WEAPON_SOUND_PATHS.forEach {
            if (assetManager.isLoaded(it)) assetManager.unload(it)
        }
        ALL_COMBAT_SOUND_PATHS.forEach {
            if (assetManager.isLoaded(it)) assetManager.unload(it)
        }
        ALL_UI_SOUND_PATHS.forEach {
            if (assetManager.isLoaded(it)) assetManager.unload(it)
        }
    }

    fun playUiSound(path: String) {
        sound(path).play(effectsVolume)
    }

    fun background(): Pair<TextureRegion, TextureRegion>? = this.stageLevel?.let { level ->
        backgrounds(level).let { Pair(this.get(it.first), this.get(it.second)) }
    }
}

val stageAssetsManager by lazy { StageAssetsManager() }