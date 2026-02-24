package fr.mesabloo.heavymachdefense.screens

import aurelienribon.tweenengine.TweenManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.utils.Timer
import fr.mesabloo.heavymachdefense.MainGame
import fr.mesabloo.heavymachdefense.PPM
import fr.mesabloo.heavymachdefense.ai.BaseEntity
import fr.mesabloo.heavymachdefense.ai.EnemyTankEntity
import fr.mesabloo.heavymachdefense.ai.GameObject
import fr.mesabloo.heavymachdefense.ai.MachineEntity
import fr.mesabloo.heavymachdefense.ai.Team
import fr.mesabloo.heavymachdefense.data.*
import fr.mesabloo.heavymachdefense.entities.buildMachineTemplate
import fr.mesabloo.heavymachdefense.entities.createBases
import fr.mesabloo.heavymachdefense.entities.createTerrainBody
import ktx.box2d.body
import ktx.box2d.box
import fr.mesabloo.heavymachdefense.data.Specials
import fr.mesabloo.heavymachdefense.listeners.stage.*
import fr.mesabloo.heavymachdefense.managers.BackgroundMusicManager
import fr.mesabloo.heavymachdefense.managers.WaveManager
import fr.mesabloo.heavymachdefense.managers.animationManager
import fr.mesabloo.heavymachdefense.managers.backgroundMusicManager
import fr.mesabloo.heavymachdefense.ifDev
import fr.mesabloo.heavymachdefense.managers.assets.StageAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.assetManager
import fr.mesabloo.heavymachdefense.managers.assets.stageAssetsManager
import fr.mesabloo.heavymachdefense.timers.cellMiningTimer
import fr.mesabloo.heavymachdefense.ui.stage.*
import fr.mesabloo.heavymachdefense.ui.stage.buttons.*
import fr.mesabloo.heavymachdefense.ui.stage.dialog.SystemMenu
import fr.mesabloo.heavymachdefense.ui.stage.EnemyTank
import fr.mesabloo.heavymachdefense.managers.assets.levelSelectionAssetsManager
import fr.mesabloo.heavymachdefense.managers.assets.buttonAssetsManager
import fr.mesabloo.heavymachdefense.ui.stage.game.AllyBase
import fr.mesabloo.heavymachdefense.ui.stage.game.Bullet
import fr.mesabloo.heavymachdefense.ui.stage.game.ExplosionEffect
import fr.mesabloo.heavymachdefense.ui.stage.game.GameResultOverlay
import fr.mesabloo.heavymachdefense.ui.stage.slots.MachineBuildSlot
import fr.mesabloo.heavymachdefense.ui.stage.slots.SpecialBuildSlot
import fr.mesabloo.heavymachdefense.ui.stage.slots.TurretBuildSlot
import fr.mesabloo.heavymachdefense.world.GameWorld
import fr.mesabloo.heavymachdefense.world.UI_HEIGHT
import fr.mesabloo.heavymachdefense.world.UI_WIDTH
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.actors.alpha
import ktx.actors.setScrollFocus
import ktx.preferences.flush
import ktx.preferences.set
import kotlin.math.pow
import kotlin.properties.Delegates

class StageScreen(
    game: MainGame,
    private val level: Int,
    val save: GameSave,
    val saveIndex: Int,
    isLoading: Boolean = false
) :
    AbstractScreen(game, isLoading) {
    private companion object {
        const val TEMPORARY_CELL_UPGRADE_RATIO = 0.6f
        const val SLOT_MENU_WIDTH = 128f
        const val SLOT_MENU_HEIGHT = 710f
        const val MACHINE_SPEED = 12.5f // pixels per second
        const val ENEMY_SPEED = 10f // pixels per second
        const val SPAWN_X_MIN = 48f   // terrain is 512px; leave margin for unit half-width
        const val SPAWN_X_MAX = 464f
        const val SPAWN_Y_PROXIMITY = 200f // only check entities near the spawn Y
    }

    private lateinit var buildQueue: BuildQueue
    private lateinit var allyBase: AllyBase
    private lateinit var gameWorld: GameWorld
    private lateinit var waveManager: WaveManager

    private lateinit var allyBaseEntity: BaseEntity
    private lateinit var enemyBaseEntity: BaseEntity

    private var gameEnded = false
    private var gameEndTimer = 0f
    private var gameResultOverlay: GameResultOverlay? = null

    private val gameObjects = mutableListOf<GameObject>()

    private val btreeSource by lazy { Gdx.files.internal("ai/machine.btree").readString() }
    private val btreeParser = BehaviorTreeParser<GameObject>(BehaviorTreeParser.DEBUG_NONE)

    private val upgrades: Upgrades = Json.decodeFromString(Gdx.files.internal("data/upgrades.json").readString())
    private val builds: Builds = Json {
        allowStructuredMapKeys = true // this is because we store 'Pair's as 'HashMap' keys
    }.decodeFromString(Gdx.files.internal("data/build-info.json").readString())
    private val specials: Specials = Json.decodeFromString(Gdx.files.internal("data/special-info.json").readString())

    private var upgradeMenuShown: Boolean = false

    private lateinit var title: Title
    private lateinit var terrain: Terrain

    private var playerLife: Long = 500L
    private var enemyLife: Long = 500L
    private var lowHpWarningPlayed: Boolean = false

    private val menuTweenManager = TweenManager()

    private var maxCells: Long
    private var currentCells: Long
    private var temporaryCellUpgradesCount: Long = 0
    private var temporaryCellUpgradeCost: Long

    private var cellResearchMultiplier: Float
    private var crResearchMultiplier: Float
    private var buildTimeMultiplier: Float

    private var baseDefenseLevel by Delegates.observable(1) { _, old, new ->
        if (!this.isLoading && old != new) {
            this.allyBase.defLevel = new
        }
    }
    private var baseAttackLevel by Delegates.observable(1) { _, old, new ->
        if (!this.isLoading && old != new) {
            this.allyBase.atkLevel = new
        }
    }

    private lateinit var systemMenu: SystemMenu

    private val bgm = backgroundMusicManager.load(BackgroundMusicManager.GAMEPLAY).also {
        it.isLooping = true
        it.volume = 0f
    }
    private var backgroundMusicVolume: Float = 1.0f
    private var effectsVolume: Float = 1.0f

    private lateinit var upgradeEquipButton: UpgradeEquipment

    private var currentMenuKind by Delegates.observable(StageAssetsManager.UI.TitleKind.BUILD_MACH) { _, old, new ->
        if (old != new) {
            this.upgradeEquipButton.kind = new
            this.title.kind = new
        }
    }

    private lateinit var machineSlots: Group
    private lateinit var specialSlots: Group

    init {
        this.maxCells = this.upgrades.cell_storage[this.save.mainUpgrades[UpgradeKind.CELL_STORAGE]?.minus(1)
            ?: 0].storage * 2f.pow(this.temporaryCellUpgradesCount.toFloat()).toLong()
        this.temporaryCellUpgradeCost = (this.maxCells * TEMPORARY_CELL_UPGRADE_RATIO).toLong()
        this.currentCells = this.maxCells * 2 / 5

        this.cellResearchMultiplier =
            this.upgrades.cell_research[this.save.mainUpgrades[UpgradeKind.CELL_RESEARCH]?.minus(1) ?: 0].multiplier
        this.crResearchMultiplier =
            this.upgrades.cr_research[this.save.mainUpgrades[UpgradeKind.CR_RESEARCH]?.minus(1) ?: 0].multiplier
        this.buildTimeMultiplier =
            this.upgrades.build_time[this.save.mainUpgrades[UpgradeKind.BUILD_TIME]?.minus(1) ?: 0].multiplier
    }

    override fun show() {
        super.show()

        if (this.isLoading)
            return

        this.bgm.volume = this.backgroundMusicVolume
        this.bgm.play()

        stageAssetsManager.effectsVolume = this.effectsVolume

        this.systemMenu = SystemMenu(this::backgroundMusicVolume, this::effectsVolume, this)

        lateinit var scrollpane: ScrollPane
        this.background.addActor(ScrollPane(Terrain().also {
            this.terrain = it
        }).also {
            scrollpane = it

            val yOffset = 180f

            it.setBounds(128f, yOffset, 512f, UI_HEIGHT - yOffset)
            it.setSmoothScrolling(true)
            it.setScrollbarsVisible(false)
            it.setScrollingDisabled(true, false)
            it.setOverscroll(false, false)

            it.layout()
            it.scrollTo(0f, 0f, 512f, 1024f - yOffset)
            //it.layout()
        })

        this.background.addActor(HpGauges(this::playerLife, this::enemyLife).also {
            it.setPosition(128f, UI_HEIGHT - it.height - 3f)
        })
        this.background.addActor(BuildSlot().also {
            it.setPosition(0f, 0f)
        })
        this.background.addActor(MachSlot().also {
            it.setPosition(UI_WIDTH - it.width, 57f)
        })
        this.background.addActor(CellCounter(this::maxCells, this::currentCells).also {
            it.setPosition(652f, 995f)
        })
        this.background.addActor(
            CellTemporaryUpgrade(
                this::currentCells,
                this::temporaryCellUpgradeCost,
                this::temporaryCellUpgradesCount
            ).also {
                it.setPosition(630f, 972f)
            })

        this.background.addActor(BuildQueue(this::upgradeMenuShown, this.upgrades, this.save) { item ->
            stageAssetsManager.playUiSound(StageAssetsManager.SOUND_BUILD_COMPLETE)
            when (item) {
                is BuildMachineItem -> spawnMachine(item.kind, item.level)
            }
        }.also {
            this.buildQueue = it

            it.height = 508f
            it.width = 64f
            it.setPosition(22f, 692f - it.height)
        })

        val slotsContent = Group().also {
            this.machineSlots = it

            ifDev {
                this.save.buildSlots = MachineKind.values().map { MachineSlot(it) }.toMutableList()
                SpecialKind.values().forEach { kind -> this.save.specialCount[kind] = 99 }
            }

            val slotCount = this.save.buildSlots.size
            val contentHeight = 76f + slotCount * 102f
            it.setSize(SLOT_MENU_WIDTH, contentHeight)

            var currentY = contentHeight - 76f
            for (slot in this.save.buildSlots) {
                it.addActor(when (slot) {
                    is MachineSlot -> MachineBuildSlot(
                        slot,
                        this.save,
                        this.builds,
                        this::currentCells,
                        this.buildQueue
                    )
                    is TurretSlot -> TurretBuildSlot(
                        slot,
                        this.save,
                        this.builds,
                        this::currentCells,
                        this.buildQueue
                    )
                    else -> TODO()
                }.also {
                    it.setPosition(40f, currentY)

                    it.addListener(
                        BuildMachineIfPossible(
                            it,
                            this::currentCells,
                            this.buildQueue,
                            this.builds,
                            this::upgradeMenuShown
                        )
                    )
                })

                currentY -= 102f
            }

            it.alpha = 1f
            it.touchable = Touchable.enabled
        }
        this.background.addActor(ScrollPane(slotsContent).also {
            it.setPosition(640f, 966f - SLOT_MENU_HEIGHT)
            it.setSize(SLOT_MENU_WIDTH, SLOT_MENU_HEIGHT)
            it.setScrollingDisabled(true, false)
            it.setOverscroll(false, false)
            it.setFlickScroll(true)
            it.touchable = Touchable.enabled
        })
        this.background.addActor(Group().also {
            this.specialSlots = it

            var currentY = SLOT_MENU_HEIGHT - 76f
            for (slot in this.save.specialSlots) {
                it.addActor(when (slot) {
                    is SpecialSlot -> SpecialBuildSlot(slot, this.save, this.specials)
                    else -> TODO()
                }.also { buildSlot ->
                    buildSlot.setPosition(40f, currentY)
                    buildSlot.addListener(UseSpecialAttack(buildSlot, this::upgradeMenuShown) { slot ->
                        executeSpecialAttack(slot.kind)
                    })
                })

                currentY -= 102f
            }

            it.setPosition(640f, 966f - SLOT_MENU_HEIGHT)
            it.setSize(SLOT_MENU_WIDTH, SLOT_MENU_HEIGHT)

            it.alpha = 0f
            it.touchable = Touchable.disabled
        })

        this.background.addActor(UpgradeEquipment(this.currentMenuKind).also {
            this.upgradeEquipButton = it

            it.setPosition(650f, 180f)
        })

        val controlsGroup = Group()

        controlsGroup.addActor(Controls().also {
            it.setPosition(0f, 512f)
        })
        controlsGroup.addActor(StageNumber(level).also {
            it.setPosition(142f - it.width / 2f, 79f + 512f)
        })
        controlsGroup.addActor(Credits(save::credits).also {
            it.setPosition(176f - it.width, 30f + 512f)
        })
        controlsGroup.addActor(MenuButton().also {
            it.setPosition(587f, 89f + 512f)

            it.addListener(ShowSystemMenu(this.systemMenu, this.ui, this::upgradeMenuShown))
        })
        controlsGroup.addActor(BaseUpgradeButton().also {
            it.setPosition(587f, 22f + 512f)
            it.addListener(ShowUpgradeMenu(this.menuTweenManager, controlsGroup, scrollpane, this::upgradeMenuShown))
        })
        controlsGroup.addActor(Title(this.currentMenuKind).also {
            this.title = it

            it.setPosition(UI_WIDTH / 2f - it.width / 2f + 3f, 130f + 512f)
        })

        controlsGroup.addActor(BuildMachMenuButton().also {
            it.setPosition(UI_WIDTH / 2f - it.width / 2f - 90f, 47f + 512f)
            it.addListener(RemoveClickIfUpgradeMenuShown(this::upgradeMenuShown))
            it.addListener(ResetAnimationsForOthers(controlsGroup, it))
            it.addListener(PlayAnimation(it))
            it.addListener(ShowBuildSideMenu(this::currentMenuKind, this.machineSlots, this.specialSlots))

            animationManager.setCurrentKeyframe(it.animationId, 7)
        })
        controlsGroup.addActor(SpecialAttackMenuButton().also {
            it.setPosition(UI_WIDTH / 2f - it.width / 2f + 2f, 47f + 512f)
            it.addListener(RemoveClickIfUpgradeMenuShown(this::upgradeMenuShown))
            it.addListener(ResetAnimationsForOthers(controlsGroup, it))
            it.addListener(PlayAnimation(it))
            it.addListener(ShowSpecialSideMenu(this::currentMenuKind, this.machineSlots, this.specialSlots))

            animationManager.setCurrentKeyframe(it.animationId, 0)
        })
        controlsGroup.addActor(
            UpgradeMenu(
                this.save,
                this.upgrades,
                ShowUpgradeMenu(this.menuTweenManager, controlsGroup, scrollpane, this::upgradeMenuShown)
            ).also {
                it.setPosition(0f, -512f + 512f)
            })

        controlsGroup.setPosition(0f, -512f)

        this.background.addActor(controlsGroup)

        this.terrain.setScrollFocus(true)

        this.gameWorld = GameWorld(this.terrain)

        createTerrainBody(this.gameWorld)
        val basesResult = createBases(this.gameWorld, this.upgrades, this::save)
        this.allyBase = basesResult.allyBase

        // Register bases as targetable game objects
        this.allyBaseEntity = BaseEntity(basesResult.allyBase, basesResult.allyBody, this.gameObjects, Team.ALLY, 500, 500)
        this.enemyBaseEntity = BaseEntity(basesResult.enemyBase, basesResult.enemyBody, this.gameObjects, Team.ENEMY, 500, 500)
        this.gameObjects.add(allyBaseEntity)
        this.gameObjects.add(enemyBaseEntity)

        this.background.addActor(Radar(scrollpane).also {
            it.setPosition(22f, 698f)
        })

        // Load wave data for this level
        val wavePath = "data/waves/level-${level.toString().padStart(2, '0')}.json"
        val waveFile = Gdx.files.internal(wavePath)
        if (waveFile.exists()) {
            val levelWaves: LevelWaves = Json.decodeFromString(waveFile.readString())
            this.waveManager = WaveManager(levelWaves) { info -> spawnEnemyTank(info) }
        } else {
            this.waveManager = WaveManager(generateDefaultWaves(level)) { info -> spawnEnemyTank(info) }
        }
    }

    private fun spawnMachine(kind: MachineKind, level: Int) {
        if (gameEnded) return
        val machine = buildMachineTemplate(kind, level)

        // Apply combat stats from build-info
        val stats = this.builds.machines[Pair(kind, level)]
        if (stats != null) {
            machine.hp = stats.hp
            machine.maxHp = stats.hp
            machine.attackDamage = stats.attack
            machine.attackSpeed = stats.attackSpeed
            machine.attackRange = stats.range
            machine.detectionRange = stats.detectionRange
        }

        // Face upward (only for terrain machines, not UI slots)
        machine.setOrigin(machine.width / 2f, machine.height / 2f)
        machine.rotation = 90f

        val body = this.gameWorld.world.body {
            type = BodyDef.BodyType.KinematicBody
            box(
                width = machine.width / PPM,
                height = machine.height / PPM
            ) {
                density = 10f
                restitution = 0f
                friction = 1f
                isSensor = false
            }
            userData = machine
            val machineSpawnY = 160f + machine.height / 2f
            position.set(
                findNonOverlappingSpawnX(machine.width, Team.ALLY, machineSpawnY) / PPM,
                machineSpawnY / PPM
            )
        }

        // Walking animation controls body velocity (step-based movement)
        machine.physicsBody = body
        machine.startWalkingAnimation(MACHINE_SPEED)

        // Register AI entity
        val entity = MachineEntity(machine, body, this.gameObjects, MACHINE_SPEED)
        entity.behaviorTree = this.btreeParser.parse(this.btreeSource, entity)
        entity.onShoot = { shooter, target ->
            val shooterPos = shooter.getPosition().cpy().scl(PPM)
            val targetPos = target.getPosition().cpy().scl(PPM)
            val shooterEntity = shooter as MachineEntity
            val damage = shooterEntity.machine.attackDamage
            val kind = shooterEntity.machine.kind

            // Per-machine bullet sprite, speed, and hit effect
            val (bulletRegion, bulletSpeed, hitEffect) = when (kind) {
                MachineKind.RIFLE -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "00"), 350f, "damage"
                )
                MachineKind.HMG -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "04"), 400f, "damage-hmg"
                )
                MachineKind.MISSILE -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.SHELL_BULLETS, "00"), 250f, "explode-01"
                )
                MachineKind.HEAVY_MISSILE -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.SHELL_BULLETS, "01"), 200f, "explode-02"
                )
                MachineKind.ION -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "06"), 450f, "explode-ion"
                )
                MachineKind.PLASMA -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "10"), 300f, "explode-plasma"
                )
                MachineKind.SHOTGUN -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "03"), 380f, "damage"
                )
                MachineKind.TANKER -> Triple(
                    stageAssetsManager.unsafeRegion(StageAssetsManager.ALLY_BULLETS, "08"), 300f, "explode-01"
                )
            }

            // Weapon fire sound (random variation)
            stageAssetsManager.randomWeaponSound(kind).play(this.effectsVolume)

            // Smoke trail for missile/shell type bullets
            val smokeTrail = when (kind) {
                MachineKind.MISSILE, MachineKind.HEAVY_MISSILE ->
                    assetManager.get<TextureAtlas>(StageAssetsManager.EFFECTS).findRegions("smoke")
                else -> null
            }

            val bullet = Bullet(
                bulletRegion, shooterPos, targetPos, bulletSpeed,
                damage,
                { target.isAlive },
                { hitPos ->
                    when (target) {
                        is EnemyTankEntity -> {
                            target.tank.hp -= damage
                            stageAssetsManager.randomBodyHitSound().play(this.effectsVolume)
                        }
                        is BaseEntity -> target.hp -= damage
                    }
                    spawnEffect(hitEffect, hitPos)
                },
                {},
                trailRegions = smokeTrail
            )
            this.terrain.addActor(bullet)
        }
        this.gameObjects.add(entity)
    }

    /**
     * Find a spawn X (in pixels) that avoids overlapping with existing entities of the given team.
     * Only considers entities near the given [spawnY] (in pixels) to avoid false positives from
     * entities that have already walked far away.
     */
    private fun findNonOverlappingSpawnX(unitWidth: Float, team: Team, spawnY: Float): Float {
        val halfW = unitWidth / 2f
        val spawnYWorld = spawnY / PPM

        // Only consider same-team entities near the spawn Y
        val occupiedXs = gameObjects
            .filter { obj ->
                obj.isAlive && obj.team == team && obj !is BaseEntity &&
                    kotlin.math.abs(obj.getPosition().y - spawnYWorld) < SPAWN_Y_PROXIMITY / PPM
            }
            .map { it.getPosition().x * PPM }

        if (occupiedXs.isEmpty()) {
            return SPAWN_X_MIN + Math.random().toFloat() * (SPAWN_X_MAX - SPAWN_X_MIN)
        }

        // Try several random candidates and pick the one with the most clearance
        var bestX = SPAWN_X_MIN + Math.random().toFloat() * (SPAWN_X_MAX - SPAWN_X_MIN)
        var bestMinDist = 0f

        for (i in 0 until 20) {
            val candidateX = SPAWN_X_MIN + Math.random().toFloat() * (SPAWN_X_MAX - SPAWN_X_MIN)
            val minDist = occupiedXs.minOf { kotlin.math.abs(it - candidateX) }
            if (minDist > bestMinDist) {
                bestMinDist = minDist
                bestX = candidateX
            }
            if (minDist >= unitWidth) break // no overlap
        }

        return bestX.coerceIn(SPAWN_X_MIN + halfW, SPAWN_X_MAX - halfW)
    }

    private fun spawnEffect(effectName: String, pixelPos: Vector2) {
        val atlas = assetManager.get<TextureAtlas>(StageAssetsManager.EFFECTS)
        val regions = atlas.findRegions(effectName)
        if (regions.size == 0) return
        val effect = ExplosionEffect(regions)
        effect.setPosition(pixelPos.x - effect.width / 2f, pixelPos.y - effect.height / 2f)
        this.terrain.addActor(effect)
    }

    // ======================== Special Attacks ========================

    private fun executeSpecialAttack(kind: SpecialKind) {
        if (gameEnded) return
        val count = this.save.specialCount[kind] ?: 0
        if (count <= 0) return
        this.save.specialCount[kind] = count - 1

        val maxLevel = when (kind) {
            SpecialKind.AIRSTRIKE_BOMB -> specials.airstrikeBomb.size
            SpecialKind.AIRSTRIKE_MISSILE -> specials.airstrikeMissile.size
            SpecialKind.AIRSTRIKE_NUKE -> specials.airstrikeNuke.size
            SpecialKind.AIRSTRIKE_EMP -> specials.airstrikeEMP.size
            SpecialKind.CROSSFIRE_MISSILE -> specials.crossfireMissile.size
        }
        val level = (this.save.specialUpgrades[kind] ?: 1).coerceIn(1..maxLevel)
        val idx = level - 1

        stageAssetsManager.playUiSound(StageAssetsManager.SOUND_AIRSTRIKE)

        when (kind) {
            SpecialKind.AIRSTRIKE_BOMB -> executeAirstrikeBomb(specials.airstrikeBomb[idx])
            SpecialKind.AIRSTRIKE_MISSILE -> executeAirstrikeMissile(specials.airstrikeMissile[idx])
            SpecialKind.AIRSTRIKE_NUKE -> executeAirstrikeNuke(specials.airstrikeNuke[idx])
            SpecialKind.AIRSTRIKE_EMP -> executeAirstrikeEMP(specials.airstrikeEMP[idx])
            SpecialKind.CROSSFIRE_MISSILE -> executeCrossfireMissile(specials.crossfireMissile[idx])
        }
    }

    private fun executeAirstrikeBomb(info: AirstrikeBombInfo) {
        val damage = info.unitDamage
        for (i in 0 until info.bombCount) {
            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    if (gameEnded) return
                    val enemies = gameObjects.filter { it is EnemyTankEntity && it.isAlive }
                    if (enemies.isEmpty()) return
                    val target = enemies.random() as EnemyTankEntity
                    val pos = target.getPosition().cpy().scl(PPM)
                    pos.x += (-20..20).random()
                    pos.y += (-20..20).random()
                    target.tank.hp -= damage
                    spawnEffect("explode-ground", pos)
                    stageAssetsManager.sound(StageAssetsManager.SOUND_GROUND_HIT).play(effectsVolume)
                }
            }, i * 0.12f)
        }
    }

    private fun executeAirstrikeMissile(info: AirstrikeMissileInfo) {
        val damage = info.unitDamage
        for (i in 0 until info.bombCount) {
            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    if (gameEnded) return
                    val enemies = gameObjects.filter { it is EnemyTankEntity && it.isAlive }
                    if (enemies.isEmpty()) return
                    val target = enemies.random() as EnemyTankEntity
                    val pos = target.getPosition().cpy().scl(PPM)
                    pos.x += (-15..15).random()
                    pos.y += (-15..15).random()
                    target.tank.hp -= damage
                    spawnEffect("explode-02", pos)
                    stageAssetsManager.sound("sfx/weapon/missile.wav").play(effectsVolume)
                }
            }, i * 0.15f)
        }
    }

    private fun executeAirstrikeNuke(info: AirstrikeNukeInfo) {
        val damage = info.unitDamage
        val rangeWorld = info.range.toFloat() / PPM

        val enemies = gameObjects.filterIsInstance<EnemyTankEntity>().filter { it.isAlive }
        if (enemies.isEmpty()) return
        val avgX = enemies.map { it.getPosition().x }.average().toFloat()
        val avgY = enemies.map { it.getPosition().y }.average().toFloat()
        val centerPixel = Vector2(avgX * PPM, avgY * PPM)

        Timer.schedule(object : Timer.Task() {
            override fun run() {
                if (gameEnded) return
                val targets = gameObjects.filterIsInstance<EnemyTankEntity>().filter {
                    it.isAlive && it.getPosition().dst(avgX, avgY) <= rangeWorld
                }
                for (target in targets) {
                    target.tank.hp -= damage
                }
                spawnEffect("explode-boss", centerPixel)
                for (j in 0 until 5) {
                    val offset = Vector2(
                        centerPixel.x + (-60..60).random(),
                        centerPixel.y + (-60..60).random()
                    )
                    Timer.schedule(object : Timer.Task() {
                        override fun run() {
                            if (gameEnded) return
                            spawnEffect("explode-ground", offset)
                        }
                    }, j * 0.08f)
                }
                stageAssetsManager.sound(StageAssetsManager.SOUND_GROUND_EXPLOSION).play(effectsVolume)
            }
        }, 0.3f)
    }

    private fun executeAirstrikeEMP(info: AirStrikeEMPInfo) {
        val damage = info.unitDamage
        val rangeWorld = info.range.toFloat() / PPM
        val paralysisTime = info.paralysisTime.toFloat()

        val enemies = gameObjects.filterIsInstance<EnemyTankEntity>().filter { it.isAlive }
        if (enemies.isEmpty()) return
        val avgX = enemies.map { it.getPosition().x }.average().toFloat()
        val avgY = enemies.map { it.getPosition().y }.average().toFloat()

        Timer.schedule(object : Timer.Task() {
            override fun run() {
                if (gameEnded) return
                val targets = gameObjects.filterIsInstance<EnemyTankEntity>().filter {
                    it.isAlive && it.getPosition().dst(avgX, avgY) <= rangeWorld
                }
                for (target in targets) {
                    target.tank.hp -= damage
                    target.paralyzedTimer = paralysisTime
                    target.stopInPlace()
                    val pos = target.getPosition().cpy().scl(PPM)
                    spawnEffect("lightning", pos)
                }
                stageAssetsManager.sound(StageAssetsManager.SOUND_GROUND_HIT).play(effectsVolume)
            }
        }, 0.3f)
    }

    private fun executeCrossfireMissile(info: CrossfireMissileInfo) {
        val damage = info.unitDamage
        val machines = gameObjects.filterIsInstance<MachineEntity>().filter { it.isAlive }
        if (machines.isEmpty()) return

        val misslesPerMachine = 4
        var delay = 0f

        for (machine in machines) {
            val startPos = machine.getPosition().cpy().scl(PPM)

            for (j in 0 until misslesPerMachine) {
                Timer.schedule(object : Timer.Task() {
                    override fun run() {
                        if (gameEnded) return
                        val enemies = gameObjects.filterIsInstance<EnemyTankEntity>().filter { it.isAlive }
                        if (enemies.isEmpty()) return
                        val target = enemies.random()
                        val targetPos = target.getPosition().cpy().scl(PPM)
                        targetPos.x += (-15..15).random()
                        targetPos.y += (-15..15).random()

                        val bulletRegion = stageAssetsManager.unsafeRegion(StageAssetsManager.SHELL_BULLETS, "00")
                        val smokeTrail = assetManager.get<TextureAtlas>(StageAssetsManager.EFFECTS).findRegions("smoke")

                        // Alternate arc direction: odd missiles curve left, even curve right
                        val arcDir = if (j % 2 == 0) 1f else -1f
                        val arcAmount = (80f + (0..40).random()) * arcDir

                        val bullet = Bullet(
                            bulletRegion, startPos.cpy(), targetPos, 350f,
                            damage,
                            { target.isAlive },
                            { hitPos ->
                                target.tank.hp -= damage
                                spawnEffect("explode-01", hitPos)
                                stageAssetsManager.sound(StageAssetsManager.SOUND_GROUND_HIT).play(effectsVolume)
                            },
                            {},
                            trailRegions = smokeTrail,
                            arcHeight = arcAmount
                        )
                        terrain.addActor(bullet)
                        stageAssetsManager.sound("sfx/weapon/missile.wav").play(effectsVolume * 0.7f)
                    }
                }, delay)
                delay += 0.1f
            }
        }
    }

    // ======================== Enemy Spawning ========================

    private fun spawnEnemyTank(info: EnemySpawnInfo) {
        if (gameEnded) return
        val tank = EnemyTank(info.tankType)
        tank.hp = info.hp
        tank.maxHp = info.hp
        tank.attackDamage = info.attack
        tank.attackSpeed = info.attackSpeed
        tank.attackRange = info.range
        tank.detectionRange = info.detectionRange

        tank.setOrigin(tank.width / 2f, tank.height / 2f)
        tank.rotation = -90f // face downward

        val tankSpawnY = 1900f + tank.height / 2f
        val spawnX = findNonOverlappingSpawnX(tank.width, Team.ENEMY, tankSpawnY) / PPM
        val spawnY = tankSpawnY / PPM

        val body = this.gameWorld.world.body {
            type = BodyDef.BodyType.KinematicBody
            box(
                width = tank.width / PPM,
                height = tank.height / PPM
            ) {
                density = 10f
                restitution = 0f
                friction = 1f
                isSensor = false
            }
            userData = tank
            position.set(spawnX, spawnY)
        }

        tank.physicsBody = body

        val entity = EnemyTankEntity(tank, body, this.gameObjects, info.speed)
        entity.behaviorTree = this.btreeParser.parse(this.btreeSource, entity)
        entity.onShoot = { shooter, target ->
            val shooterPos = shooter.getPosition().cpy().scl(PPM)
            val targetPos = target.getPosition().cpy().scl(PPM)
            val shooterEntity = shooter as EnemyTankEntity
            val damage = shooterEntity.tank.attackDamage
            val bulletRegion = stageAssetsManager.unsafeRegion(StageAssetsManager.ENEMY_BULLETS, "00")

            // Enemy tank fire sound
            stageAssetsManager.sound(StageAssetsManager.SOUND_ENEMY_FIRE).play(this.effectsVolume)

            val bullet = Bullet(
                bulletRegion, shooterPos, targetPos, 300f,
                damage,
                { target.isAlive },
                { hitPos ->
                    when (target) {
                        is MachineEntity -> target.machine.hp -= damage
                        is BaseEntity -> target.hp -= damage
                    }
                    spawnEffect("damage", hitPos)
                },
                {}
            )
            this.terrain.addActor(bullet)
        }
        this.gameObjects.add(entity)
    }

    override fun render(delta: Float) {
        this.menuTweenManager.update(delta)

        // Sync BGM volume with slider
        this.bgm.volume = this.backgroundMusicVolume

        // update cell storage capacity
        this.maxCells =
            this.upgrades.cell_storage[this.save.mainUpgrades[UpgradeKind.CELL_STORAGE]?.minus(1)
                ?: 0].storage * 2f.pow(this.temporaryCellUpgradesCount.toFloat()).toLong()
        this.temporaryCellUpgradeCost = (this.maxCells * TEMPORARY_CELL_UPGRADE_RATIO).toLong()

        this.cellResearchMultiplier =
            this.upgrades.cell_research[this.save.mainUpgrades[UpgradeKind.CELL_RESEARCH]?.minus(1) ?: 0].multiplier
        this.crResearchMultiplier =
            this.upgrades.cr_research[this.save.mainUpgrades[UpgradeKind.CR_RESEARCH]?.minus(1) ?: 0].multiplier
        this.buildTimeMultiplier =
            this.upgrades.build_time[this.save.mainUpgrades[UpgradeKind.BUILD_TIME]?.minus(1) ?: 0].multiplier

        this.baseDefenseLevel = this.save.mainUpgrades[UpgradeKind.BASE_DEFENSE] ?: 1
        this.baseAttackLevel = this.save.mainUpgrades[UpgradeKind.BASE_CANNON] ?: 1

        super.render(delta)

        if (!this.isLoading) {
            // Game end countdown
            if (gameEnded) {
                gameEndTimer += delta
                if (gameEndTimer >= 3f) {
                    returnToStageSelect()
                }
                // Still render but skip gameplay updates
                return
            }

            this.gameWorld.render(delta)

            // Spawn enemies from wave data
            this.waveManager.update(delta)

            // Step behavior trees (after physics so positions are current)
            for (obj in this.gameObjects.toList()) {
                if (obj.isAlive) {
                    obj.shootCooldownRemaining = (obj.shootCooldownRemaining - delta).coerceAtLeast(0f)
                    // EMP paralysis: skip btree step while frozen
                    if (obj is EnemyTankEntity && obj.paralyzedTimer > 0f) {
                        obj.paralyzedTimer -= delta
                        obj.body.setLinearVelocity(0f, 0f)
                        continue
                    }
                    obj.behaviorTree?.step()
                }
            }

            // Clean up dead entities (safe to destroyBody after world.step())
            this.gameObjects.removeAll { obj ->
                if (!obj.isAlive) {
                    val deathPos = obj.getPosition().cpy().scl(PPM)
                    when (obj) {
                        is MachineEntity -> {
                            this.gameWorld.world.destroyBody(obj.body)
                            obj.machine.remove()
                            val deathEffect = when (obj.machine.kind) {
                                MachineKind.MISSILE, MachineKind.HEAVY_MISSILE, MachineKind.TANKER -> "explode-02"
                                MachineKind.ION -> "explode-ion"
                                MachineKind.PLASMA -> "explode-plasma"
                                else -> "explode-npc"
                            }
                            spawnEffect(deathEffect, deathPos)
                            stageAssetsManager.sound(StageAssetsManager.SOUND_MACH_EXPLOSION).play(this.effectsVolume)
                        }
                        is EnemyTankEntity -> {
                            this.gameWorld.world.destroyBody(obj.body)
                            obj.tank.remove()
                            spawnEffect("explode-npc", deathPos)
                            stageAssetsManager.sound(StageAssetsManager.SOUND_GROUND_EXPLOSION).play(this.effectsVolume)
                        }
                    }
                    true
                } else false
            }

            // Sync HP bars with base entity HP
            playerLife = allyBaseEntity.hp.toLong().coerceAtLeast(0L)
            enemyLife = enemyBaseEntity.hp.toLong().coerceAtLeast(0L)

            // Low HP warning when base drops below 25%
            if (!lowHpWarningPlayed && allyBaseEntity.isAlive
                && allyBaseEntity.hp < allyBaseEntity.maxHp * 0.25f) {
                lowHpWarningPlayed = true
                stageAssetsManager.playUiSound(StageAssetsManager.SOUND_LOW_HP)
            }

            // Check game over / victory
            if (!allyBaseEntity.isAlive && !gameEnded) {
                gameEnded = true
                gameEndTimer = 0f
                showGameResult(false)
            } else if (!enemyBaseEntity.isAlive && !gameEnded) {
                gameEnded = true
                gameEndTimer = 0f
                // Unlock next stage if this is the furthest cleared
                if (this.level > this.save.lastStageCompleted) {
                    this.save.lastStageCompleted = this.level.coerceAtMost(79)
                    Gdx.app.getPreferences(GameSave.PREFERENCES_PATH).flush {
                        this[this@StageScreen.saveIndex.toString()] = Json.encodeToString(this@StageScreen.save)
                    }
                }
                showGameResult(true)
            }
        }
    }

    private fun showGameResult(isVictory: Boolean) {
        val overlay = GameResultOverlay(isVictory)
        this.gameResultOverlay = overlay
        // Add to the UI foreground so it draws above everything
        this.background.addActor(overlay)
        overlay.zIndex = Int.MAX_VALUE
    }

    private fun returnToStageSelect() {
        if (this.isLoading) return
        gameEnded = false // prevent re-entry

        levelSelectionAssetsManager.preload()
        buttonAssetsManager.preload()

        this.addLoadingOverlay({
            if (!assetManager.isFinished)
                assetManager.update()
            levelSelectionAssetsManager.isFullyLoaded() && buttonAssetsManager.isFullyLoaded()
        }) {
            this@StageScreen.background.children.forEach { it.remove() }

            val stageSelect = StageSelectionScreen(
                this@StageScreen.game,
                this@StageScreen.save,
                this@StageScreen.saveIndex,
                true
            )
            (this.changeScreen(stageSelect) as AbstractScreen?)
                ?.addLoadingOverlayEnd()

            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    this@addLoadingOverlay.removeScreen<StageScreen>()?.dispose()
                }
            }, 0.050f)
        }
    }

    override fun pause() {
        super.pause()

        cellMiningTimer.stop()
    }

    override fun resume() {
        super.resume()

        cellMiningTimer.start()
    }

    override fun dispose() {
        super.dispose()

        this.bgm.stop()

        this.gameResultOverlay?.dispose()
        this.gameWorld.dispose()

        animationManager.dispose()
        stageAssetsManager.dispose()

        cellMiningTimer.clear()
    }
}