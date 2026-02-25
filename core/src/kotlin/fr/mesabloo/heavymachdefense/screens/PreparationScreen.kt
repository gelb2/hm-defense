package fr.mesabloo.heavymachdefense.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import fr.mesabloo.heavymachdefense.DEV
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.data.GameSave
import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.ifDev
import fr.mesabloo.heavymachdefense.listeners.preparation.BackToStageSelection
import fr.mesabloo.heavymachdefense.listeners.preparation.StartStage
import fr.mesabloo.heavymachdefense.managers.FontManager
import fr.mesabloo.heavymachdefense.managers.assets.LevelSelectionAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.levelSelectionAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.preparationAssetsManager
import fr.mesabloo.heavymachdefense.managers.fontManager
import fr.mesabloo.heavymachdefense.ui.common.BackButton
import fr.mesabloo.heavymachdefense.ui.common.InGameDialog
import fr.mesabloo.heavymachdefense.ui.common.OkButton
import fr.mesabloo.heavymachdefense.world.UI_WIDTH
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.actors.setScrollFocus
import ktx.preferences.flush
import ktx.preferences.set

class PreparationScreen(
    game: MainGame,
    val level: Int,
    val save: GameSave,
    val saveIndex: Int,
    isLoading: Boolean = false
) : AbstractScreen(game, isLoading) {

    companion object {
        private const val MAX_MACHINE_LEVEL = 10

        fun getUpgradeCost(kind: MachineKind, currentLevel: Int): Long {
            val baseCost = when (kind) {
                MachineKind.RIFLE -> 100L
                MachineKind.MISSILE -> 150L
                MachineKind.HMG -> 200L
                MachineKind.SHOTGUN -> 175L
                MachineKind.PLASMA -> 250L
                MachineKind.ION -> 300L
                MachineKind.HEAVY_MISSILE -> 250L
                MachineKind.TANKER -> 150L
            }
            return baseCost * currentLevel
        }
    }

    private lateinit var creditsLabel: Label
    private lateinit var machineTable: Table
    private lateinit var scrollPane: ScrollPane

    private var dimTexture: Texture? = null
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

    private fun colorDrawable(color: Color): com.badlogic.gdx.scenes.scene2d.utils.Drawable {
        return TextureRegionDrawable(TextureRegion(getPixelTexture())).tint(color)
    }

    override fun show() {
        super.show()

        if (this.isLoading) return

        ifDev {
            if (save.credits <= 0) save.credits = 20000
        }

        val font16 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!
        val font11 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_11_WHITE]!!

        // Background
        this.background.addActor(Image(colorDrawable(Color(0.08f, 0.08f, 0.12f, 1f))).also {
            it.setSize(768f, 1024f)
            it.setPosition(0f, 0f)
        })

        // Title
        this.background.addActor(Label("MACHINE UPGRADE", Label.LabelStyle(font16, Color.WHITE)).also {
            it.setPosition(40f, 960f)
        })

        // Credits display
        this.creditsLabel = Label("${save.credits} cr", Label.LabelStyle(font16, Color(0.478f, 1f, 0.933f, 1f)))
        this.creditsLabel.setPosition(UI_WIDTH - 40f - creditsLabel.prefWidth, 960f)
        this.background.addActor(creditsLabel)

        // Stage info
        this.background.addActor(Label("Stage $level", Label.LabelStyle(font11, Color(0.7f, 0.7f, 0.7f, 1f))).also {
            it.setPosition(40f, 940f)
        })

        // Machine list
        buildMachineList()

        // Bottom buttons
        val btnY = 30f

        this.background.addActor(BackButton().also {
            it.setPosition(UI_WIDTH * 1f / 3f - it.width / 2f, btnY)
            it.addListener(BackToStageSelection(this))
        })

        val startButton = OkButton()
        startButton.also {
            it.setPosition(UI_WIDTH * 2f / 3f - it.width / 2f, btnY)
            it.addListener(StartStage(this))
        }
        this.background.addActor(startButton)

        if (::scrollPane.isInitialized && !scrollPane.hasScrollFocus()) {
            scrollPane.setScrollFocus(true)
        }
    }

    private val normalColor = Color(0.15f, 0.15f, 0.22f, 1f)
    private val selectedColor = Color(0.2f, 0.3f, 0.45f, 1f)
    private val disabledColor = Color(0.1f, 0.1f, 0.13f, 1f)

    private fun buildMachineList() {
        val font16 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!
        val font11 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_11_WHITE]!!

        machineTable = Table()
        machineTable.touchable = Touchable.enabled

        for (kind in MachineKind.values()) {
            val currentLevel = save.machineUpgrades[kind] ?: 1
            val isMax = currentLevel >= MAX_MACHINE_LEVEL
            val cost = if (isMax) 0L else getUpgradeCost(kind, currentLevel)
            val canAfford = !isMax && save.credits >= cost

            val row = Table()
            row.touchable = Touchable.enabled
            row.pad(8f, 12f, 8f, 12f)

            // Current level icon
            val currentIcon = Image(preparationAssetsManager.bodyRegion(kind, currentLevel))
            currentIcon.touchable = Touchable.disabled
            row.add(currentIcon).size(32f, 32f).padRight(10f)

            // Machine name + level info
            val infoTable = Table()
            val displayName = kind.machineName.uppercase().replace('-', ' ')
            infoTable.add(Label(displayName, Label.LabelStyle(font16, Color.WHITE)).also {
                it.touchable = Touchable.disabled
            }).left().row()

            val levelText = if (isMax) "Lv.$currentLevel (MAX)" else "Lv.$currentLevel"
            infoTable.add(Label(levelText, Label.LabelStyle(font11, Color(0.7f, 0.7f, 0.7f, 1f))).also {
                it.touchable = Touchable.disabled
            }).left()

            row.add(infoTable).expandX().left().padRight(6f)

            // Next level icon (if not max)
            if (!isMax) {
                row.add(Label(">", Label.LabelStyle(font11, Color(0.5f, 0.5f, 0.5f, 1f))).also {
                    it.touchable = Touchable.disabled
                }).padRight(6f)

                val nextIcon = Image(preparationAssetsManager.bodyRegion(kind, currentLevel + 1))
                nextIcon.touchable = Touchable.disabled
                row.add(nextIcon).size(32f, 32f).padRight(10f)
            } else {
                row.add().padRight(6f)
                row.add().size(32f, 32f).padRight(10f)
            }

            // Cost
            val costText = if (isMax) "MAX" else "${cost}cr"
            val costColor = if (isMax) Color(0.5f, 0.5f, 0.5f, 1f)
                else if (canAfford) Color(0.478f, 1f, 0.933f, 1f)
                else Color(1f, 0.4f, 0.4f, 1f)
            row.add(Label(costText, Label.LabelStyle(font11, costColor)).also {
                it.touchable = Touchable.disabled
                it.setAlignment(Align.right)
            }).minWidth(55f).right()

            // Color background
            row.background = colorDrawable(if (canAfford) normalColor else disabledColor)

            // Click listener: single tap = highlight, double tap = upgrade confirm
            val machineKind = kind
            row.addListener(object : ClickListener() {
                init {
                    setTapCountInterval(0.4f)
                }

                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val cl = save.machineUpgrades[machineKind] ?: 1
                    val mx = cl >= MAX_MACHINE_LEVEL
                    val cst = if (mx) 0L else getUpgradeCost(machineKind, cl)
                    val afford = !mx && save.credits >= cst
                    if (!afford) return

                    if (tapCount >= 2) {
                        val dn = machineKind.machineName.uppercase().replace('-', ' ')
                        InGameDialog.showConfirm(
                            this@PreparationScreen.ui,
                            "Upgrade $dn to Lv.${cl + 1}? ($cst cr)"
                        ) {
                            performUpgrade(machineKind)
                        }
                    } else {
                        updateRowHighlight(machineKind)
                    }
                }
            })

            machineTable.add(row).fillX().expandX().pad(2f).row()
        }

        scrollPane = ScrollPane(machineTable)
        scrollPane.setScrollingDisabled(true, false)
        scrollPane.setSmoothScrolling(true)
        scrollPane.touchable = Touchable.enabled

        this.background.addActor(Table().also {
            it.setPosition(40f, 120f)
            it.setSize(688f, 800f)
            it.touchable = Touchable.enabled
            it.add(scrollPane).expand().fill()
        })
    }

    private fun updateRowHighlight(highlightKind: MachineKind?) {
        MachineKind.values().forEachIndexed { index, kind ->
            val row = machineTable.children[index] as Table
            val currentLevel = save.machineUpgrades[kind] ?: 1
            val isMax = currentLevel >= MAX_MACHINE_LEVEL
            val cost = if (isMax) 0L else getUpgradeCost(kind, currentLevel)
            val canAfford = !isMax && save.credits >= cost

            row.background = colorDrawable(
                when {
                    kind == highlightKind && canAfford -> selectedColor
                    !canAfford -> disabledColor
                    else -> normalColor
                }
            )
        }
    }

    private fun performUpgrade(kind: MachineKind) {
        val currentLevel = save.machineUpgrades[kind] ?: 1
        if (currentLevel >= MAX_MACHINE_LEVEL) return
        val cost = getUpgradeCost(kind, currentLevel)
        if (save.credits < cost) return

        save.credits -= cost
        save.machineUpgrades[kind] = currentLevel + 1

        // Persist save
        Gdx.app.getPreferences(GameSave.PREFERENCES_PATH).flush {
            this[saveIndex.toString()] = Json.encodeToString(save)
        }

        // Rebuild the list to reflect new state
        rebuildMachineList()
    }

    private fun rebuildMachineList() {
        // Remove old scroll container
        val toRemove = background.children.filterIsInstance<Table>().find { child ->
            child.children.any { it is ScrollPane }
        }
        toRemove?.remove()

        buildMachineList()

        // Update credits label
        creditsLabel.setText("${save.credits} cr")
        creditsLabel.setPosition(UI_WIDTH - 40f - creditsLabel.prefWidth, 960f)

        if (::scrollPane.isInitialized && !scrollPane.hasScrollFocus()) {
            scrollPane.setScrollFocus(true)
        }
    }

    override fun render(delta: Float) {
        super.render(delta)

        // Keep credits display updated
        if (::creditsLabel.isInitialized) {
            creditsLabel.setText("${save.credits} cr")
        }
    }

    override fun dispose() {
        super.dispose()
        dimTexture?.dispose()
        pixelTexture?.dispose()
        preparationAssetsManager.dispose()
    }
}
