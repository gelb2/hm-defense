package fr.mesabloo.heavymachdefense.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSaveTest {

    private fun freshSave() = GameSave(
        creationDate = Date(),
        lastAccessedDate = Date()
    )

    // --- checkValid() ---

    @Test
    fun `fresh save is valid`() {
        assertDoesNotThrow { freshSave().checkValid() }
    }

    @Test
    fun `negative credits rejected`() {
        val save = freshSave().apply { credits = -1 }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `zero credits accepted`() {
        val save = freshSave().apply { credits = 0 }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `lastStageCompleted boundary 0 accepted`() {
        val save = freshSave().apply { lastStageCompleted = 0 }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `lastStageCompleted boundary 79 accepted`() {
        val save = freshSave().apply { lastStageCompleted = 79 }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `lastStageCompleted 80 rejected`() {
        val save = freshSave().apply { lastStageCompleted = 80 }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `lastStageCompleted negative rejected`() {
        val save = freshSave().apply { lastStageCompleted = -1 }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `empty buildSlots rejected`() {
        val save = freshSave().apply { buildSlots = mutableListOf() }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `8 buildSlots rejected`() {
        val save = freshSave().apply {
            buildSlots = MutableList(8) { MachineSlot(MachineKind.RIFLE) }
        }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `7 buildSlots accepted`() {
        val save = freshSave().apply {
            buildSlots = MutableList(7) { MachineSlot(MachineKind.RIFLE) }
        }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `locked machine in buildSlots rejected`() {
        val save = freshSave().apply {
            machineUpgrades[MachineKind.PLASMA] = 0
            buildSlots = mutableListOf(MachineSlot(MachineKind.PLASMA))
        }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    // --- Serialization round-trip ---

    @Test
    fun `serialization round-trip preserves data`() {
        val original = freshSave().apply {
            credits = 12345L
            lastStageCompleted = 42
            name = "TestPlayer"
            machineUpgrades[MachineKind.RIFLE] = 5
            mainUpgrades[UpgradeKind.CR_RESEARCH] = 3
        }

        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<GameSave>(json)

        assertEquals(original.credits, restored.credits)
        assertEquals(original.lastStageCompleted, restored.lastStageCompleted)
        assertEquals(original.name, restored.name)
        assertEquals(original.machineUpgrades[MachineKind.RIFLE], restored.machineUpgrades[MachineKind.RIFLE])
        assertEquals(original.mainUpgrades[UpgradeKind.CR_RESEARCH], restored.mainUpgrades[UpgradeKind.CR_RESEARCH])
    }

    @Test
    fun `corrupted json throws on deserialization`() {
        assertThrows<Exception> {
            Json.decodeFromString<GameSave>("{invalid json content")
        }
    }

    // --- Default values ---

    @Test
    fun `fresh save has all 8 machine upgrades at level 1`() {
        val save = freshSave()
        assertEquals(8, save.machineUpgrades.size)
        assertTrue(save.machineUpgrades.values.all { it == 1 })
    }

    @Test
    fun `fresh save has all 6 main upgrades at level 1`() {
        val save = freshSave()
        assertEquals(6, save.mainUpgrades.size)
        assertTrue(save.mainUpgrades.values.all { it == 1 })
    }

    @Test
    fun `fresh save starts with 0 credits`() {
        assertEquals(0L, freshSave().credits)
    }

    @Test
    fun `fresh save has 1 build slot (RIFLE)`() {
        val save = freshSave()
        assertEquals(1, save.buildSlots.size)
        assertTrue(save.buildSlots[0] is MachineSlot)
        assertEquals(MachineKind.RIFLE, (save.buildSlots[0] as MachineSlot).kind)
    }

    @Test
    fun `fresh save has 5 special slots`() {
        val save = freshSave()
        assertEquals(5, save.specialSlots.size)
    }

    // --- TurretSlot validation ---

    @Test
    fun `locked turret in buildSlots rejected`() {
        val save = freshSave().apply {
            turretUpgrades[TurretKind.LASER] = 0
            buildSlots = mutableListOf(TurretSlot(TurretKind.LASER))
        }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `unlocked turret in buildSlots accepted`() {
        val save = freshSave().apply {
            turretUpgrades[TurretKind.RIFLE] = 1
            buildSlots = mutableListOf(TurretSlot(TurretKind.RIFLE))
        }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `mixed machine and turret slots validated`() {
        val save = freshSave().apply {
            buildSlots = mutableListOf(
                MachineSlot(MachineKind.RIFLE),
                TurretSlot(TurretKind.RIFLE)
            )
        }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `turret with missing upgrade key rejected`() {
        val save = freshSave().apply {
            turretUpgrades.clear()
            buildSlots = mutableListOf(TurretSlot(TurretKind.RIFLE))
        }
        // ?: 0 fallback → treated as locked
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    @Test
    fun `machine with missing upgrade key rejected`() {
        val save = freshSave().apply {
            machineUpgrades.clear()
            buildSlots = mutableListOf(MachineSlot(MachineKind.RIFLE))
        }
        assertThrows<InvalidSaveException> { save.checkValid() }
    }

    // --- Large value edge cases ---

    @Test
    fun `large credits accepted`() {
        val save = freshSave().apply { credits = Long.MAX_VALUE }
        assertDoesNotThrow { save.checkValid() }
    }

    @Test
    fun `large credits survive serialization round-trip`() {
        val save = freshSave().apply { credits = Long.MAX_VALUE }
        val json = Json.encodeToString(save)
        val restored = Json.decodeFromString<GameSave>(json)
        assertEquals(Long.MAX_VALUE, restored.credits)
    }

    // --- Partial JSON deserialization ---

    @Test
    fun `JSON with missing optional fields uses defaults`() {
        // Minimal JSON with only required fields
        val minimalJson = """{"creationDate":0,"lastAccessedDate":0}"""
        val lenient = Json { ignoreUnknownKeys = true }
        val save = lenient.decodeFromString<GameSave>(minimalJson)
        assertEquals(0L, save.credits)
        assertEquals(0, save.lastStageCompleted)
        assertEquals(8, save.machineUpgrades.size)
        assertEquals(6, save.mainUpgrades.size)
    }
}
