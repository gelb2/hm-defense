package fr.mesabloo.heavymachdefense.data

import java.io.Serializable

/**
 * An enumeration containing all the available machine kinds, mapped to their low-level names.
 *
 * @property machineName The low-level of each machine kind.
 */
enum class MachineKind(val machineName: String, val speed: Float) : Serializable {
    RIFLE("rifle", 14.4f),
    MISSILE("missile", 11.0f),
    HEAVY_MISSILE("heavy-missile", 11.0f),
    ION("ion", 17.5f),
    HMG("hmg", 14.4f),
    PLASMA("plasma", 12.5f),
    SHOTGUN("shotgun", 16.0f),
    TANKER("tanker", 9.5f);
}