package fr.mesabloo.heavymachdefense.ai.machines

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import fr.mesabloo.heavymachdefense.ai.GameObject

class StopMovementTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        `object`.stopInPlace()
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
