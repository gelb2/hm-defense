package fr.mesabloo.heavymachdefense.managers

import fr.mesabloo.heavymachdefense.data.EnemySpawnInfo
import fr.mesabloo.heavymachdefense.data.LevelWaves

class WaveManager(
    private val levelWaves: LevelWaves,
    private val spawnCallback: (EnemySpawnInfo) -> Unit
) {
    val currentWave: Int get() = currentWaveIndex + 1
    val totalWaves: Int get() = levelWaves.waves.size

    private var elapsed = 0f
    private var currentWaveIndex = 0
    private var groupTrackers: List<GroupTracker>? = null
    private var waveStartTime = 0f
    private var loopPauseRemaining = 0f

    companion object {
        private const val LOOP_PAUSE_SECONDS = 5f
    }

    fun update(delta: Float) {
        if (levelWaves.waves.isEmpty()) return

        // Pause between loops
        if (loopPauseRemaining > 0f) {
            loopPauseRemaining -= delta
            return
        }

        elapsed += delta

        val wave = levelWaves.waves[currentWaveIndex]

        if (elapsed >= wave.delay) {
            if (groupTrackers == null) {
                waveStartTime = elapsed
                groupTrackers = wave.groups.map { GroupTracker(it) }
            }

            val allDone = groupTrackers!!.all { tracker ->
                tracker.update(elapsed - waveStartTime, spawnCallback)
            }

            if (allDone) {
                currentWaveIndex++
                groupTrackers = null
                // Loop back to first wave when all waves exhausted
                if (currentWaveIndex >= levelWaves.waves.size) {
                    currentWaveIndex = 0
                    elapsed = 0f
                    loopPauseRemaining = LOOP_PAUSE_SECONDS
                }
            }
        }
    }

    private class GroupTracker(private val info: EnemySpawnInfo) {
        private var spawned = 0
        private var lastSpawnTime = -999f

        fun update(timeSinceWaveStart: Float, callback: (EnemySpawnInfo) -> Unit): Boolean {
            if (spawned >= info.count) return true

            if (spawned == 0 || timeSinceWaveStart - lastSpawnTime >= info.interval) {
                callback(info)
                spawned++
                lastSpawnTime = timeSinceWaveStart
            }
            return spawned >= info.count
        }
    }
}
