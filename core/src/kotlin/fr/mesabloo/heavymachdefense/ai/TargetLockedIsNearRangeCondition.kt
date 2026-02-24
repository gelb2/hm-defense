package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class TargetLockedIsNearRangeCondition : LeafTask<GameObject>() {
    override fun execute(): Status {
        val detectionRange = `object`.range?.second ?: return Status.FAILED
        val distance = `object`.getTarget().getPosition().dst(`object`.getPosition())
        return if (distance <= detectionRange) Status.SUCCEEDED else Status.FAILED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
