package fr.mesabloo.heavymachdefense.entities

import fr.mesabloo.heavymachdefense.data.MachineKind
import fr.mesabloo.heavymachdefense.ui.stage.Machine

fun buildMachineTemplate(kind: MachineKind, level: Int) = Machine(kind, level)