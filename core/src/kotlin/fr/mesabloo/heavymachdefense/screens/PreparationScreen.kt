package fr.mesabloo.heavymachdefense.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.data.GameSave
import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.data.models.MachineModel
import fr.mesabloo.heavymachdefense.ifDev
import fr.mesabloo.heavymachdefense.listeners.preparation.BackToStageSelection
import fr.mesabloo.heavymachdefense.listeners.preparation.StartStage
import fr.mesabloo.heavymachdefense.managers.FontManager
import fr.mesabloo.heavymachdefense.managers.assets.PreparationAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.preparationAssetsManager
import fr.mesabloo.heavymachdefense.managers.fontManager
import fr.mesabloo.heavymachdefense.ui.common.BackButton
import fr.mesabloo.heavymachdefense.ui.common.OkButton
import fr.mesabloo.heavymachdefense.world.UI_HEIGHT
import fr.mesabloo.heavymachdefense.world.UI_WIDTH
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.preferences.flush
import ktx.preferences.set
import kotlin.math.ceil

class PreparationScreen(
    game: MainGame,
    val level: Int,
    val save: GameSave,
    val saveIndex: Int,
    isLoading: Boolean = false
) : AbstractScreen(game, isLoading) {

    companion object {
        private const val MAX_MACHINE_LEVEL = 10
        private const val CELL_WIDTH = 160f
        private const val CELL_HEIGHT = 180f
        private const val ICON_SIZE = 80f
        private const val GRID_COLS = 3
        private const val GRID_GAP = 8f

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
    private val gridGroup = Group()
    private var upgradeOverlay: Group? = null
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

    override fun show() {
        super.show()
        if (this.isLoading) return

        ifDev {
            if (save.credits <= 0) save.credits = 20000
        }

        val font16 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!
        val font11 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_11_WHITE]!!

        // Background image (mechanical interior from stage selection screen)
        this.background.addActor(Image(preparationAssetsManager.texture(PreparationAssetsManager.BACKGROUND)).also {
            it.setSize(UI_WIDTH, UI_HEIGHT)
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

        // Machine grid (permanent container — only children are rebuilt on upgrade)
        this.background.addActor(gridGroup)
        buildMachineGrid()

        // Foreground overlay (left/right decorative frame from stage selection screen)
        this.background.addActor(Image(preparationAssetsManager.texture(PreparationAssetsManager.FOREGROUND)).also {
            it.setSize(UI_WIDTH, UI_HEIGHT)
            it.setPosition(0f, 0f)
            it.touchable = Touchable.disabled
        })

        // Bottom buttons (added after foreground so they receive touch)
        val btnY = 30f
        this.background.addActor(BackButton().also {
            it.setPosition(UI_WIDTH * 1f / 3f - it.width / 2f, btnY)
            it.addListener(BackToStageSelection(this))
        })
        this.background.addActor(OkButton().also {
            it.setPosition(UI_WIDTH * 2f / 3f - it.width / 2f, btnY)
            it.addListener(StartStage(this))
        })
    }

    private fun buildMachineGrid() {
        gridGroup.clearChildren()

        val font12 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_12_WHITE]!!
        val font11 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_11_WHITE]!!

        val gridTable = Table()
        gridTable.defaults().pad(GRID_GAP / 2f)

        var col = 0
        for (kind in MachineKind.values()) {
            gridTable.add(createMachineCell(kind, font12, font11)).size(CELL_WIDTH, CELL_HEIGHT)
            col++
            if (col >= GRID_COLS) {
                gridTable.row()
                col = 0
            }
        }

        gridTable.pack()
        val gridRows = ceil(MachineKind.values().size.toDouble() / GRID_COLS).toInt()
        val totalWidth = GRID_COLS * CELL_WIDTH + (GRID_COLS - 1) * GRID_GAP
        val totalHeight = gridRows * CELL_HEIGHT + (gridRows - 1) * GRID_GAP
        gridTable.setPosition(
            (UI_WIDTH - totalWidth) / 2f,
            (UI_HEIGHT - totalHeight) / 2f - 30f
        )
        gridGroup.addActor(gridTable)
    }

    private fun createMachineCell(kind: MachineKind, fontName: BitmapFont, fontLevel: BitmapFont): Group {
        val currentLevel = save.machineUpgrades[kind] ?: 1
        val isMax = currentLevel >= MAX_MACHINE_LEVEL

        val cell = Group()
        cell.setSize(CELL_WIDTH, CELL_HEIGHT)
        cell.touchable = Touchable.enabled

        // Layer 1: Build-slot background — explicitly sized to fill cell
        val bg = Image(preparationAssetsManager.texture(PreparationAssetsManager.BUILD_SLOT_BACKGROUND))
        bg.setSize(CELL_WIDTH, CELL_HEIGHT)
        bg.setPosition(0f, 0f)
        cell.addActor(bg)

        // Layer 2: Full machine preview (body + weapons + feet) — rotated 90° to face up
        val preview = createMachinePreview(kind, currentLevel)
        val previewW = preview.width
        val previewH = preview.height

        // Scale to fit upper area. After 90° rotation: visual width = previewH, visual height = previewW
        val maxDisplayW = CELL_WIDTH - 24f
        val maxDisplayH = CELL_HEIGHT - 70f
        val previewScale = minOf(maxDisplayW / previewH, maxDisplayH / previewW)
        preview.setScale(previewScale)
        preview.setOrigin(previewW / 2f, previewH / 2f)
        preview.rotation = 90f

        // Center in upper area (bottom ~50px reserved for labels)
        val machineCenterX = CELL_WIDTH / 2f
        val machineCenterY = (CELL_HEIGHT + 50f) / 2f
        preview.setPosition(machineCenterX - previewW / 2f, machineCenterY - previewH / 2f)
        preview.touchable = Touchable.disabled
        cell.addActor(preview)

        // Layer 3: Machine name
        val displayName = kind.machineName.uppercase().replace('-', ' ')
        val nameLabel = Label(displayName, Label.LabelStyle(fontName, Color.WHITE))
        nameLabel.setAlignment(Align.center)
        nameLabel.setSize(CELL_WIDTH, nameLabel.prefHeight)
        nameLabel.setPosition(0f, 28f)
        nameLabel.touchable = Touchable.disabled
        cell.addActor(nameLabel)

        // Layer 4: Level text
        val levelColor = if (isMax) Color(0.478f, 1f, 0.933f, 1f) else Color(0.7f, 0.7f, 0.7f, 1f)
        val levelText = if (isMax) "Lv.$currentLevel MAX" else "Lv.$currentLevel"
        val levelLabel = Label(levelText, Label.LabelStyle(fontLevel, levelColor))
        levelLabel.setAlignment(Align.center)
        levelLabel.setSize(CELL_WIDTH, levelLabel.prefHeight)
        levelLabel.setPosition(0f, 12f)
        levelLabel.touchable = Touchable.disabled
        cell.addActor(levelLabel)

        cell.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                showUpgradeOverlay(kind)
            }
        })

        return cell
    }

    private fun showUpgradeOverlay(kind: MachineKind) {
        if (upgradeOverlay != null) return

        val currentLevel = save.machineUpgrades[kind] ?: 1
        val isMax = currentLevel >= MAX_MACHINE_LEVEL
        val cost = if (isMax) 0L else getUpgradeCost(kind, currentLevel)
        val canAfford = !isMax && save.credits >= cost

        val font16 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_16_WHITE]!!
        val font11 = fontManager.bitmapFonts[FontManager.TREBUCHET_MS_BOLD_11_WHITE]!!

        val overlay = Group()
        overlay.setSize(UI_WIDTH, UI_HEIGHT)

        // Dim background — click to close
        val dim = Image(colorDrawable(Color(0f, 0f, 0f, 0.85f)))
        dim.setSize(UI_WIDTH, UI_HEIGHT)
        dim.touchable = Touchable.enabled
        dim.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                closeUpgradeOverlay()
            }
        })
        overlay.addActor(dim)

        // Dialog panel
        val dialog = Table()
        dialog.background = colorDrawable(Color(0.15f, 0.15f, 0.2f, 0.95f))
        dialog.pad(24f)
        dialog.touchable = Touchable.enabled

        // Full machine preview (body + weapons + feet, rotated 90° to face up)
        val iconSize = ICON_SIZE * 1.3f
        val preview = createMachinePreview(kind, currentLevel)
        val previewW = preview.width
        val previewH = preview.height
        // After 90° rotation: visual width = previewH, visual height = previewW
        val previewScale = minOf(iconSize / previewH, iconSize / previewW)
        preview.setScale(previewScale)
        preview.setOrigin(previewW / 2f, previewH / 2f)
        preview.rotation = 90f
        // Wrap in fixed-size container for Table layout
        val visualW = previewH * previewScale
        val visualH = previewW * previewScale
        val previewContainer = Group()
        previewContainer.setSize(visualW, visualH)
        preview.setPosition(visualW / 2f - previewW / 2f, visualH / 2f - previewH / 2f)
        preview.touchable = Touchable.disabled
        previewContainer.addActor(preview)
        dialog.add(previewContainer).size(visualW, visualH).padBottom(12f).row()

        // Machine name
        val displayName = kind.machineName.uppercase().replace('-', ' ')
        dialog.add(Label(displayName, Label.LabelStyle(font16, Color.WHITE))).padBottom(8f).row()

        // Level info
        val levelText = if (isMax) "Lv.$currentLevel (MAX)"
        else "Lv.$currentLevel \u2192 Lv.${currentLevel + 1}"
        dialog.add(Label(levelText, Label.LabelStyle(font11, Color(0.7f, 0.7f, 0.7f, 1f)))).padBottom(4f).row()

        // Cost
        val costText = if (isMax) "MAX" else "Cost: ${cost} cr"
        val costColor = when {
            isMax -> Color(0.5f, 0.5f, 0.5f, 1f)
            canAfford -> Color(0.478f, 1f, 0.933f, 1f)
            else -> Color(1f, 0.4f, 0.4f, 1f)
        }
        dialog.add(Label(costText, Label.LabelStyle(font11, costColor))).padBottom(16f).row()

        // Buttons
        val btnTable = Table()

        val upgradeBtnStyle = TextButton.TextButtonStyle()
        upgradeBtnStyle.font = font16
        if (canAfford) {
            upgradeBtnStyle.fontColor = Color.WHITE
            upgradeBtnStyle.up = colorDrawable(Color(0.3f, 0.5f, 0.8f, 1f))
            upgradeBtnStyle.down = colorDrawable(Color(0.2f, 0.4f, 0.7f, 1f))
            upgradeBtnStyle.down.leftWidth = 12f; upgradeBtnStyle.down.rightWidth = 12f
            upgradeBtnStyle.down.topHeight = 8f; upgradeBtnStyle.down.bottomHeight = 8f
        } else {
            upgradeBtnStyle.fontColor = Color(0.5f, 0.5f, 0.5f, 1f)
            upgradeBtnStyle.up = colorDrawable(Color(0.25f, 0.25f, 0.3f, 1f))
        }
        upgradeBtnStyle.up.leftWidth = 12f; upgradeBtnStyle.up.rightWidth = 12f
        upgradeBtnStyle.up.topHeight = 8f; upgradeBtnStyle.up.bottomHeight = 8f

        val upgradeBtn = TextButton("UPGRADE", upgradeBtnStyle)
        if (canAfford) {
            upgradeBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    performUpgrade(kind)
                    closeUpgradeOverlay()
                }
            })
        }

        val cancelBtnStyle = TextButton.TextButtonStyle()
        cancelBtnStyle.font = font16
        cancelBtnStyle.fontColor = Color.WHITE
        cancelBtnStyle.up = colorDrawable(Color(0.4f, 0.4f, 0.4f, 1f))
        cancelBtnStyle.down = colorDrawable(Color(0.3f, 0.3f, 0.3f, 1f))
        cancelBtnStyle.up.leftWidth = 12f; cancelBtnStyle.up.rightWidth = 12f
        cancelBtnStyle.up.topHeight = 8f; cancelBtnStyle.up.bottomHeight = 8f
        cancelBtnStyle.down.leftWidth = 12f; cancelBtnStyle.down.rightWidth = 12f
        cancelBtnStyle.down.topHeight = 8f; cancelBtnStyle.down.bottomHeight = 8f

        val cancelBtn = TextButton("CANCEL", cancelBtnStyle)
        cancelBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                closeUpgradeOverlay()
            }
        })

        btnTable.add(upgradeBtn).width(130f).padRight(12f)
        btnTable.add(cancelBtn).width(130f)
        dialog.add(btnTable)

        dialog.pack()
        dialog.setPosition(
            (UI_WIDTH - dialog.width) / 2f,
            (UI_HEIGHT - dialog.height) / 2f
        )
        overlay.addActor(dialog)

        this.upgradeOverlay = overlay
        this.background.addActor(overlay)
    }

    private fun closeUpgradeOverlay() {
        upgradeOverlay?.remove()
        upgradeOverlay = null
    }

    /** Assembles a full machine preview (body + weapons + feet) — same logic as Machine.kt */
    private fun createMachinePreview(kind: MachineKind, level: Int): Group {
        val model = MachineModel(kind.machineName, level)

        val group = Group()

        // Body
        val bodyRegion = preparationAssetsManager.bodyRegion(kind, level)
        val body = Image(bodyRegion)
        group.setSize(body.width, body.height)

        // Feet (behind body, visible for preview — first frame only)
        if (model.feetRegions != null && model.feetOffset != null) {
            val firstFrame = preparationAssetsManager.feetRegion(model.feetRegions[0])

            val leftFoot = Image(firstFrame)
            leftFoot.setPosition(
                body.width / 2f + model.feetOffset.first - leftFoot.width / 2f,
                body.height / 2f + model.feetOffset.second - leftFoot.height / 2f
            )
            group.addActor(leftFoot)

            val rightFoot = Image(TextureRegion(firstFrame).also { it.flip(false, true) })
            rightFoot.setPosition(
                body.width / 2f + model.feetOffset.first - rightFoot.width / 2f,
                body.height / 2f - model.feetOffset.second - rightFoot.height / 2f
            )
            group.addActor(rightFoot)
        }

        // Body (on top of feet)
        body.setPosition(0f, 0f)
        group.addActor(body)

        // Weapons (on top of body)
        val weaponRegion = preparationAssetsManager.weaponRegion(kind, level)
        if (weaponRegion != null) {
            val weapon1 = Image(weaponRegion)
            weapon1.setPosition(
                body.width / 2f + model.leftWeaponOffset.first - weapon1.width / 2f,
                body.height / 2f + model.leftWeaponOffset.second - weapon1.height / 2f
            )
            group.addActor(weapon1)

            val weapon2 = Image(TextureRegion(weaponRegion).also { it.flip(false, true) })
            weapon2.setPosition(
                body.width / 2f + model.rightWeaponOffset.first - weapon2.width / 2f,
                body.height / 2f + model.rightWeaponOffset.second - weapon2.height / 2f
            )
            group.addActor(weapon2)
        }

        return group
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

        // Rebuild grid to reflect new state
        rebuildMachineGrid()
    }

    private fun rebuildMachineGrid() {
        buildMachineGrid()

        creditsLabel.setText("${save.credits} cr")
        creditsLabel.setPosition(UI_WIDTH - 40f - creditsLabel.prefWidth, 960f)
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (::creditsLabel.isInitialized) {
            creditsLabel.setText("${save.credits} cr")
        }
    }

    override fun dispose() {
        super.dispose()
        pixelTexture?.dispose()
        preparationAssetsManager.dispose()
    }
}
