package fr.mesabloo.heavymachdefense.ai.machines

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import fr.mesabloo.heavymachdefense.ai.BaseEntity
import fr.mesabloo.heavymachdefense.ai.GameObject

class WalkForwardTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        // Do not advance if an enemy base is within attack range
        val attackRange = `object`.range?.first
        if (attackRange != null) {
            val myPos = `object`.getPosition()
            val baseInRange = `object`.objects.any { obj ->
                obj !== `object` && obj.isAlive && obj.team != `object`.team &&
                    obj is BaseEntity &&
                    obj.getPosition().dst(myPos) <= attackRange
            }
            if (baseInRange) {
                `object`.stopInPlace()
                return Status.SUCCEEDED
            }
        }
        `object`.walk()
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
