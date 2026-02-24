package fr.mesabloo.heavymachdefense.data

import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class EnemySpawnInfo(
    val tankType: String,
    val count: Int,
    val interval: Float,
    val hp: Int,
    val attack: Int,
    val attackSpeed: Float,
    val range: Float,
    val detectionRange: Float,
    val speed: Float
)

@Serializable
data class Wave(
    val delay: Float,
    val groups: List<EnemySpawnInfo>
)

@Serializable
data class LevelWaves(
    val waves: List<Wave>
)

/**
 * Generates default wave data for a given level (1-80).
 * Difficulty scales progressively: more enemies, higher stats, more tank types.
 */
fun generateDefaultWaves(level: Int): LevelWaves {
    val progress = (level - 1).coerceIn(0, 79) / 79f // 0.0 ~ 1.0

    // Tank types unlock gradually: level 1 uses 01-02, level 80 uses up to 32
    val maxTankType = min(2 + (level * 30 / 80), 32)
    val minTankType = (maxTankType - 4).coerceAtLeast(1)

    // Wave count: 3 at level 1, up to 6 at level 80
    val waveCount = (3 + (progress * 3)).roundToInt().coerceIn(3, 6)

    // Base stats scale with level
    val baseHp = (80 + progress * 920).roundToInt()        // 80 ~ 1000
    val baseAttack = (8 + progress * 72).roundToInt()       // 8 ~ 80
    val baseSpeed = 6f + progress * 4f                      // 6 ~ 10
    val baseAttackSpeed = 0.6f + progress * 0.6f            // 0.6 ~ 1.2

    val waves = mutableListOf<Wave>()

    for (w in 0 until waveCount) {
        val waveProgress = w.toFloat() / (waveCount - 1).coerceAtLeast(1)

        // Earlier waves have lighter enemies, later waves have heavier
        val hpMul = 0.7f + waveProgress * 0.6f
        val atkMul = 0.7f + waveProgress * 0.6f

        // Delay between waves decreases with level
        val delay = if (w == 0) (5f - progress * 2f).coerceAtLeast(2f)
        else (15f + w * 10f - progress * 5f).coerceAtLeast(5f)

        // Enemies per group: more at higher levels
        val count = (3 + progress * 4 + waveProgress * 2).roundToInt().coerceIn(2, 10)
        val interval = (3f - progress * 1.5f).coerceAtLeast(1f)

        // Pick tank type for this wave
        val tankIdx = minTankType + ((maxTankType - minTankType) * waveProgress).roundToInt()
        val tankType = tankIdx.coerceIn(1, 32).toString().padStart(2, '0')

        val groups = mutableListOf<EnemySpawnInfo>()

        groups.add(
            EnemySpawnInfo(
                tankType = tankType,
                count = count,
                interval = interval,
                hp = (baseHp * hpMul).roundToInt(),
                attack = (baseAttack * atkMul).roundToInt(),
                attackSpeed = baseAttackSpeed,
                range = (100f + progress * 40f),
                detectionRange = (160f + progress * 40f),
                speed = baseSpeed + (1f - waveProgress) * 2f  // lighter tanks move faster
            )
        )

        // Later waves add a second group for variety
        if (w >= waveCount / 2 && maxTankType > minTankType + 1) {
            val secondTankIdx = minTankType + ((maxTankType - minTankType) * (waveProgress * 0.5f)).roundToInt()
            val secondType = secondTankIdx.coerceIn(1, 32).toString().padStart(2, '0')
            groups.add(
                EnemySpawnInfo(
                    tankType = secondType,
                    count = (count * 0.6f).roundToInt().coerceAtLeast(2),
                    interval = interval * 0.8f,
                    hp = (baseHp * hpMul * 0.7f).roundToInt(),
                    attack = (baseAttack * atkMul * 1.2f).roundToInt(),
                    attackSpeed = baseAttackSpeed * 1.1f,
                    range = (100f + progress * 30f),
                    detectionRange = (160f + progress * 30f),
                    speed = baseSpeed + 2f
                )
            )
        }

        waves.add(Wave(delay = delay, groups = groups))
    }

    return LevelWaves(waves)
}
