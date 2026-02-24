package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class TargetLockedIsAliveCondition : LeafTask<GameObject>() {
    override fun execute(): Status =
        if (`object`.getTarget().isAlive) Status.SUCCEEDED else Status.FAILED

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
