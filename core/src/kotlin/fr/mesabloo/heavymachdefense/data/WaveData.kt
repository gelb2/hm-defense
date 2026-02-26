package fr.mesabloo.heavymachdefense.data

import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

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
 * Tank tier definition for wave composition.
 * Each tier covers a range of tank model types (01-32) with distinct stat multipliers.
 */
private data class TierDef(
    val minType: Int,
    val maxType: Int,
    val hpMul: Float,
    val atkMul: Float,
    val countMul: Float,
    val spdMul: Float
)

/**
 * Generates default wave data for a given level (1-80).
 *
 * Uses a 4-tier system (SCOUT/MEDIUM/HEAVY/ELITE) to ensure diverse enemy composition
 * within each wave. Lower-tier enemies appear as fast, numerous cannon fodder while
 * higher-tier enemies are slow, tough elites. All 32 tank models are utilized across
 * the full level range.
 */
fun generateDefaultWaves(level: Int): LevelWaves {
    val progress = (level - 1).coerceIn(0, 79) / 79f // 0.0 ~ 1.0
    val rng = Random(level.toLong() * 31337) // deterministic per level, varied between levels

    // Tank types unlock gradually: level 1 → up to 02, level 80 → up to 32
    val maxUnlockedType = min(2 + (level * 30 / 80), 32)

    // Wave count: 3 at level 1, up to 6 at level 80
    val waveCount = (3 + (progress * 3)).roundToInt().coerceIn(3, 6)

    // Base stats scale with level
    val baseHp = (80 + progress * 920).roundToInt()        // 80 ~ 1000
    val baseAttack = (8 + progress * 72).roundToInt()       // 8 ~ 80
    val baseSpeed = 6f + progress * 4f                      // 6 ~ 10
    val baseAttackSpeed = 0.6f + progress * 0.6f            // 0.6 ~ 1.2

    // Build tier boundaries within 1..maxUnlockedType
    val tiers = buildTiers(maxUnlockedType)

    // Base enemy count per group before tier multiplier
    val baseCount = (3 + progress * 4).roundToInt().coerceIn(2, 8)
    val baseInterval = (3f - progress * 1.5f).coerceAtLeast(0.8f)
    val baseRange = 100f + progress * 40f
    val baseDetection = 160f + progress * 40f

    val waves = mutableListOf<Wave>()

    for (w in 0 until waveCount) {
        val waveProgress = w.toFloat() / (waveCount - 1).coerceAtLeast(1)
        val waveMul = 0.7f + waveProgress * 0.6f // 0.7 ~ 1.3

        // Wave delay: first wave starts quickly, subsequent waves spaced out
        val delay = if (w == 0) (5f - progress * 2f).coerceAtLeast(2f)
        else (15f + w * 10f - progress * 5f).coerceAtLeast(5f)

        // Select tiers for this wave based on wave position
        val waveTiers = selectWaveTiers(waveProgress, tiers)

        val groups = waveTiers.map { tier ->
            val tankType = rng.nextInt(tier.minType, tier.maxType + 1)
                .toString().padStart(2, '0')

            EnemySpawnInfo(
                tankType = tankType,
                count = (baseCount * tier.countMul * waveMul).roundToInt().coerceAtLeast(1),
                interval = baseInterval,
                hp = (baseHp * tier.hpMul * waveMul).roundToInt().coerceAtLeast(1),
                attack = (baseAttack * tier.atkMul * waveMul).roundToInt().coerceAtLeast(1),
                attackSpeed = baseAttackSpeed,
                range = baseRange,
                detectionRange = baseDetection,
                speed = baseSpeed * tier.spdMul
            )
        }

        waves.add(Wave(delay = delay, groups = groups))
    }

    return LevelWaves(waves)
}

/**
 * Build 4 tiers (SCOUT/MEDIUM/HEAVY/ELITE) from the unlocked tank type range.
 * Returns only valid tiers (minType <= maxType).
 * For small ranges (e.g., level 4 with maxType=3), fewer tiers are produced.
 */
private fun buildTiers(maxUnlockedType: Int): List<TierDef> {
    val tierSize = maxOf(maxUnlockedType / 4, 1)
    return listOf(
        // SCOUT: fast, weak, numerous (bottom 25%)
        TierDef(
            minType = 1,
            maxType = minOf(tierSize, maxUnlockedType),
            hpMul = 0.3f, atkMul = 0.3f, countMul = 2.0f, spdMul = 1.3f
        ),
        // MEDIUM: balanced (25-50%)
        TierDef(
            minType = tierSize + 1,
            maxType = minOf(tierSize * 2, maxUnlockedType),
            hpMul = 0.6f, atkMul = 0.6f, countMul = 1.2f, spdMul = 1.1f
        ),
        // HEAVY: tough, fewer (50-75%)
        TierDef(
            minType = tierSize * 2 + 1,
            maxType = minOf(tierSize * 3, maxUnlockedType),
            hpMul = 1.0f, atkMul = 1.0f, countMul = 0.8f, spdMul = 0.9f
        ),
        // ELITE: boss-class, rare, slow (top 25%)
        TierDef(
            minType = tierSize * 3 + 1,
            maxType = maxUnlockedType,
            hpMul = 1.5f, atkMul = 1.5f, countMul = 0.5f, spdMul = 0.7f
        )
    ).filter { it.minType <= it.maxType }
}

/**
 * Select which tiers compose a wave based on wave progress (0.0=first, 1.0=last).
 *
 * Pattern:
 * - Early waves (0~33%):  SCOUT + MEDIUM
 * - Mid waves (33~66%):   MEDIUM + HEAVY (+ SCOUT if available)
 * - Late waves (66~100%): HEAVY + ELITE (+ SCOUT for dramatic contrast)
 *
 * Falls back gracefully when fewer than 4 tiers exist (low levels).
 */
private fun selectWaveTiers(waveProgress: Float, tiers: List<TierDef>): List<TierDef> {
    if (tiers.size <= 1) return tiers

    val scout = tiers.first()
    val elite = tiers.last()

    return when {
        tiers.size == 2 -> {
            // Only 2 tiers: always mix both
            listOf(scout, elite)
        }
        tiers.size == 3 -> {
            val mid = tiers[1]
            when {
                waveProgress < 0.4f -> listOf(scout, mid)
                waveProgress < 0.7f -> listOf(mid, elite)
                else -> listOf(elite, scout) // late: elites + scouts for contrast
            }
        }
        else -> {
            // 4 tiers: full diversity
            val medium = tiers[1]
            val heavy = tiers[2]
            when {
                waveProgress < 0.33f -> listOf(scout, medium)
                waveProgress < 0.66f -> listOf(scout, medium, heavy)
                else -> listOf(heavy, elite, scout)
            }
        }
    }
}
