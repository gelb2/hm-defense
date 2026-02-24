package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class UnlockTargetTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        `object`.forgetTarget()
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
