package fr.mesabloo.heavymachdefense.ai.machines

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import fr.mesabloo.heavymachdefense.ai.GameObject

class ShootTargetTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        // TODO: implement actual shooting logic
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
