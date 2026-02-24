package fr.mesabloo.heavymachdefense.data

import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.data.TurretKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildInfo(
    val cellCost: Int,
    val time: Float,
    @SerialName("max") val maxAllowed: Int,
    val hp: Int = 100,
    val attack: Int = 10,
    val attackSpeed: Float = 1.0f,
    val range: Float = 120f,
    val detectionRange: Float = 180f
)

@Serializable
data class Builds(
    val machines: HashMap<Pair<MachineKind, Int>, BuildInfo>,
    val turrets: HashMap<Pair<TurretKind, Int>, BuildInfo>
)