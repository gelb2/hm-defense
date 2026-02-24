package fr.mesabloo.heavymachdefense.data

import kotlinx.serialization.Serializable

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
