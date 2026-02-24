package fr.mesabloo.heavymachdefense.ai.machines

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import fr.mesabloo.heavymachdefense.ai.GameObject

class WalkForwardTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        `object`.walk()
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
