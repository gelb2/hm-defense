package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class TargetLockedIsInRangeCondition : LeafTask<GameObject>() {
    override fun execute(): Status {
        val attackRange = `object`.range?.first ?: return Status.FAILED
        val distance = `object`.getTarget().getPosition().dst(`object`.getPosition())
        return if (distance <= attackRange) Status.SUCCEEDED else Status.FAILED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
