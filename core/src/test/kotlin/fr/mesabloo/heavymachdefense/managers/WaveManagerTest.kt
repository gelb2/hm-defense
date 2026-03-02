package fr.mesabloo.heavymachdefense.managers

import fr.mesabloo.heavymachdefense.data.EnemySpawnInfo
import fr.mesabloo.heavymachdefense.data.LevelWaves
import fr.mesabloo.heavymachdefense.data.Wave
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveManagerTest {

    private fun simpleEnemy(count: Int = 2, interval: Float = 1f) = EnemySpawnInfo(
        tankType = "01",
        count = count,
        interval = interval,
        hp = 100,
        attack = 10,
        attackSpeed = 1f,
        range = 100f,
        detectionRange = 150f,
        speed = 10f
    )

    private fun singleWaveLevel(delay: Float = 2f, count: Int = 2): LevelWaves {
        return LevelWaves(waves = listOf(Wave(delay = delay, groups = listOf(simpleEnemy(count = count)))))
    }

    // --- Basic spawning ---

    @Test
    fun `no spawns before wave delay`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 5f), spawns::add)

        // Advance to just before delay
        manager.update(4.9f)
        assertTrue(spawns.isEmpty(), "Should not spawn before delay")
    }

    @Test
    fun `first spawn after wave delay`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 2f, count = 3), spawns::add)

        manager.update(2.1f)
        assertEquals(1, spawns.size, "First enemy should spawn after delay")
    }

    @Test
    fun `all enemies spawn at correct intervals`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 1f, count = 3), spawns::add)

        manager.update(1.5f)  // first spawn at t=1.5
        assertEquals(1, spawns.size)

        manager.update(1.5f)  // second spawn at t=3.0 (timeSinceWaveStart=1.5, interval=1.0)
        assertEquals(2, spawns.size)

        manager.update(1.5f)  // third spawn at t=4.5
        assertEquals(3, spawns.size)
    }

    // --- Multi-wave ---

    @Test
    fun `advances to second wave after first completes`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        // 3 waves so completing wave 2 doesn't trigger loop reset
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(simpleEnemy(count = 1, interval = 1f))),
            Wave(delay = 2f, groups = listOf(simpleEnemy(count = 1, interval = 1f))),
            Wave(delay = 100f, groups = listOf(simpleEnemy(count = 1, interval = 1f)))
        ))
        val manager = WaveManager(level, spawns::add)

        assertEquals(1, manager.currentWave)
        assertEquals(3, manager.totalWaves)

        // First wave: delay 1s, 1 enemy → spawns immediately after delay
        manager.update(1.5f)  // t=1.5
        assertEquals(1, spawns.size, "Wave 1 should spawn 1 enemy")

        // Wave 2 delay=2s. elapsed=1.5+1.0=2.5 >= 2 → triggers wave 2
        manager.update(1.0f)  // t=2.5
        assertEquals(2, spawns.size, "Wave 2 should spawn 1 enemy")
        // After wave 2 completes, index advances to 2 → currentWave = 3
        assertEquals(3, manager.currentWave)
    }

    // --- Loop behavior ---

    @Test
    fun `loops back to wave 1 after all waves exhausted`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(simpleEnemy(count = 1)))
        ))
        val manager = WaveManager(level, spawns::add)

        // Complete wave 1
        manager.update(1.1f)
        assertEquals(1, spawns.size)

        // After completion, loopPauseRemaining = 5s
        // Wait through pause
        manager.update(5.1f)

        // Now elapsed is reset, new wave starts after delay=1
        manager.update(1.1f)
        assertEquals(2, spawns.size, "Should spawn again after loop")
    }

    // --- Edge cases ---

    @Test
    fun `empty waves list does not crash`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(LevelWaves(waves = emptyList()), spawns::add)

        // Should not crash even with many updates
        repeat(100) { manager.update(1f) }
        assertTrue(spawns.isEmpty())
    }

    @Test
    fun `zero delta does not advance`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 1f), spawns::add)

        repeat(100) { manager.update(0f) }
        assertTrue(spawns.isEmpty(), "Zero delta should not trigger any spawns")
    }

    @Test
    fun `wave and total counters are correct`() {
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(simpleEnemy(count = 1))),
            Wave(delay = 5f, groups = listOf(simpleEnemy(count = 1))),
            Wave(delay = 8f, groups = listOf(simpleEnemy(count = 1)))
        ))
        val manager = WaveManager(level) {}

        assertEquals(1, manager.currentWave)
        assertEquals(3, manager.totalWaves)
    }
}
