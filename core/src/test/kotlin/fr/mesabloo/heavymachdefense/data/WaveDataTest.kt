package fr.mesabloo.heavymachdefense.data

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveDataTest {

    // --- generateDefaultWaves basic contract ---

    @Test
    fun `level 1 generates 3 waves`() {
        val result = generateDefaultWaves(1)
        assertEquals(3, result.waves.size)
    }

    @Test
    fun `level 80 generates 6 waves`() {
        val result = generateDefaultWaves(80)
        assertEquals(6, result.waves.size)
    }

    @Test
    fun `all levels 1-80 generate valid waves without crash`() {
        for (level in 1..80) {
            val result = generateDefaultWaves(level)
            assertTrue(result.waves.isNotEmpty(), "Level $level has no waves")
            for ((wi, wave) in result.waves.withIndex()) {
                assertTrue(wave.delay > 0f, "Level $level wave $wi has non-positive delay ${wave.delay}")
                assertTrue(wave.groups.isNotEmpty(), "Level $level wave $wi has no groups")
                for ((gi, group) in wave.groups.withIndex()) {
                    assertTrue(group.count > 0, "Level $level wave $wi group $gi has count=${group.count}")
                    assertTrue(group.hp > 0, "Level $level wave $wi group $gi has hp=${group.hp}")
                    assertTrue(group.attack > 0, "Level $level wave $wi group $gi has attack=${group.attack}")
                    assertTrue(group.speed > 0f, "Level $level wave $wi group $gi has speed=${group.speed}")
                    assertTrue(group.attackSpeed > 0f, "Level $level wave $wi group $gi has attackSpeed=${group.attackSpeed}")
                    assertTrue(group.range > 0f, "Level $level wave $wi group $gi has range=${group.range}")
                    assertTrue(group.detectionRange > 0f, "Level $level wave $wi group $gi has detectionRange=${group.detectionRange}")

                    // Tank type must be in 01-32 range
                    val typeNum = group.tankType.toInt()
                    assertTrue(typeNum in 1..32, "Level $level wave $wi group $gi has tankType=${group.tankType} out of 1..32")
                }
            }
        }
    }

    // --- Determinism ---

    @Test
    fun `same level produces identical waves`() {
        val a = generateDefaultWaves(25)
        val b = generateDefaultWaves(25)
        assertEquals(a, b)
    }

    @Test
    fun `different levels produce different waves`() {
        val a = generateDefaultWaves(1)
        val b = generateDefaultWaves(80)
        assertTrue(a != b, "Level 1 and 80 should not be identical")
    }

    // --- Stats scaling ---

    @Test
    fun `level 80 enemies are stronger than level 1`() {
        val level1 = generateDefaultWaves(1)
        val level80 = generateDefaultWaves(80)

        val maxHp1 = level1.waves.flatMap { it.groups }.maxOf { it.hp }
        val maxHp80 = level80.waves.flatMap { it.groups }.maxOf { it.hp }
        assertTrue(maxHp80 > maxHp1, "Level 80 maxHp=$maxHp80 should be > level 1 maxHp=$maxHp1")
    }

    // --- Tank type range ---

    @Test
    fun `level 1 uses only low-numbered tank types`() {
        val result = generateDefaultWaves(1)
        val types = result.waves.flatMap { it.groups }.map { it.tankType.toInt() }
        assertTrue(types.all { it <= 5 }, "Level 1 should only use low tank types, got: $types")
    }

    @Test
    fun `level 80 can use high-numbered tank types`() {
        val result = generateDefaultWaves(80)
        val maxType = result.waves.flatMap { it.groups }.maxOf { it.tankType.toInt() }
        assertTrue(maxType > 20, "Level 80 should use high tank types, max found: $maxType")
    }

    // --- First wave delay ---

    @Test
    fun `first wave delay is reasonable`() {
        for (level in 1..80) {
            val result = generateDefaultWaves(level)
            val firstDelay = result.waves.first().delay
            assertTrue(firstDelay in 1f..10f, "Level $level first delay=$firstDelay out of 1..10 range")
        }
    }

    // --- Out-of-range level values (coercion safety) ---

    @Test
    fun `level 0 does not crash`() {
        val result = generateDefaultWaves(0)
        assertTrue(result.waves.isNotEmpty())
        result.waves.flatMap { it.groups }.forEach { g ->
            assertTrue(g.hp > 0)
            assertTrue(g.count > 0)
            assertTrue(g.speed > 0f)
        }
    }

    @Test
    fun `negative level does not crash`() {
        val result = generateDefaultWaves(-10)
        assertTrue(result.waves.isNotEmpty())
        result.waves.flatMap { it.groups }.forEach { g ->
            assertTrue(g.hp > 0)
            assertTrue(g.count > 0)
        }
    }

    @Test
    fun `level above 80 does not crash`() {
        val result = generateDefaultWaves(999)
        assertTrue(result.waves.isNotEmpty())
        result.waves.flatMap { it.groups }.forEach { g ->
            assertTrue(g.hp > 0)
            assertTrue(g.count > 0)
            val typeNum = g.tankType.toInt()
            assertTrue(typeNum in 1..32, "Tank type $typeNum out of range")
        }
    }

    // --- Wave count monotonicity ---

    @Test
    fun `wave count never decreases as level increases`() {
        var prevWaveCount = 0
        for (level in 1..80) {
            val waveCount = generateDefaultWaves(level).waves.size
            assertTrue(waveCount >= prevWaveCount,
                "Wave count decreased at level $level: $prevWaveCount -> $waveCount")
            prevWaveCount = waveCount
        }
    }

    // --- Speed never zero ---

    @Test
    fun `no enemy has zero speed at any level`() {
        for (level in 1..80) {
            val result = generateDefaultWaves(level)
            for (wave in result.waves) {
                for (group in wave.groups) {
                    assertTrue(group.speed > 0f,
                        "Level $level has enemy with speed=${group.speed}")
                }
            }
        }
    }

    // --- Interval bounds ---

    @Test
    fun `spawn interval never below minimum across all levels`() {
        for (level in 1..80) {
            val result = generateDefaultWaves(level)
            for (wave in result.waves) {
                for (group in wave.groups) {
                    assertTrue(group.interval >= 0.8f,
                        "Level $level has interval=${group.interval}, expected >= 0.8")
                }
            }
        }
    }

    // --- Subsequent wave delays are larger ---

    @Test
    fun `subsequent waves have increasing delay within each level`() {
        for (level in 1..80) {
            val delays = generateDefaultWaves(level).waves.map { it.delay }
            for (i in 1 until delays.size) {
                assertTrue(delays[i] > delays[0],
                    "Level $level wave $i delay=${delays[i]} not greater than first wave delay=${delays[0]}")
            }
        }
    }
}
