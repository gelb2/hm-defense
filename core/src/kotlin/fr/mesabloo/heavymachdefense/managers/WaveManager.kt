package fr.mesabloo.heavymachdefense.managers

import fr.mesabloo.heavymachdefense.data.EnemySpawnInfo
import fr.mesabloo.heavymachdefense.data.LevelWaves

class WaveManager(
    private val levelWaves: LevelWaves,
    private val spawnCallback: (EnemySpawnInfo) -> Unit
) {
    private var elapsed = 0f
    private var currentWaveIndex = 0
    private var groupTrackers: List<GroupTracker>? = null
    private var waveStartTime = 0f

    val isComplete: Boolean get() = currentWaveIndex >= levelWaves.waves.size

    fun update(delta: Float) {
        if (isComplete) return
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
