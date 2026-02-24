package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class HasTargetLockedCondition : LeafTask<GameObject>() {
    override fun execute(): Status =
        if (`object`.hasTarget) Status.SUCCEEDED else Status.FAILED

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
