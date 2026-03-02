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

    // --- Negative delta ---

    @Test
    fun `negative delta does not spawn`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 1f), spawns::add)

        // Negative delta should not advance elapsed time meaningfully
        repeat(50) { manager.update(-0.1f) }
        assertTrue(spawns.isEmpty(), "Negative delta should not cause spawns")
    }

    // --- Large delta spanning multiple intervals ---

    @Test
    fun `large delta spawns only one enemy per update call`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val manager = WaveManager(singleWaveLevel(delay = 1f, count = 5), spawns::add)

        // Single large delta should only spawn 1 per update (no burst)
        manager.update(100f)
        assertEquals(1, spawns.size, "Single update should spawn at most 1 enemy per group")
    }

    // --- Zero interval edge case ---

    @Test
    fun `zero interval spawns one enemy per update call`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(simpleEnemy(count = 3, interval = 0f)))
        ))
        val manager = WaveManager(level, spawns::add)

        manager.update(1.5f)
        assertEquals(1, spawns.size, "First spawn after delay")

        // With interval=0, next update should spawn immediately
        manager.update(0.01f)
        assertEquals(2, spawns.size, "Second spawn with zero interval")

        manager.update(0.01f)
        assertEquals(3, spawns.size, "Third spawn with zero interval")
    }

    // --- Multi-group wave ---

    @Test
    fun `wave with multiple groups spawns from all groups`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val enemy1 = simpleEnemy(count = 1, interval = 1f).copy(tankType = "01")
        val enemy2 = simpleEnemy(count = 1, interval = 1f).copy(tankType = "02")
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(enemy1, enemy2)),
            Wave(delay = 100f, groups = listOf(simpleEnemy(count = 1)))
        ))
        val manager = WaveManager(level, spawns::add)

        manager.update(1.5f)
        // Both groups should spawn their first (and only) enemy
        assertEquals(2, spawns.size, "Both groups should spawn")
        val types = spawns.map { it.tankType }.toSet()
        assertEquals(setOf("01", "02"), types, "Both tank types should appear")
    }

    // --- Wave with empty groups ---

    @Test
    fun `wave with empty groups completes immediately`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = emptyList()),
            Wave(delay = 2f, groups = listOf(simpleEnemy(count = 1))),
            Wave(delay = 100f, groups = listOf(simpleEnemy(count = 1)))
        ))
        val manager = WaveManager(level, spawns::add)

        // Wave 1 (empty groups) should complete immediately after delay
        manager.update(1.5f)
        // Should have advanced to wave 2 and spawned if delay met
        // Wave 1 completes instantly (empty groups → allDone=true), advances to wave 2
        // elapsed=1.5, wave 2 delay=2 → not yet
        assertEquals(0, spawns.size)
        assertEquals(2, manager.currentWave)

        // Now trigger wave 2
        manager.update(1.0f) // elapsed=2.5 >= 2
        assertEquals(1, spawns.size)
    }

    // --- Enemy count zero ---

    @Test
    fun `enemy with count 0 completes group immediately`() {
        val spawns = mutableListOf<EnemySpawnInfo>()
        val level = LevelWaves(waves = listOf(
            Wave(delay = 1f, groups = listOf(simpleEnemy(count = 0))),
            Wave(delay = 2f, groups = listOf(simpleEnemy(count = 1))),
            Wave(delay = 100f, groups = listOf(simpleEnemy(count = 1)))
        ))
        val manager = WaveManager(level, spawns::add)

        manager.update(1.5f)
        // Group with count=0 should immediately be done (spawned=0 >= 0)
        assertTrue(spawns.isEmpty(), "Count 0 should not produce any spawns")
        assertEquals(2, manager.currentWave, "Should advance past wave with count=0 group")
    }
}
