package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

class LockAnyTargetNearRangeTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        val detectionRange = `object`.range?.second ?: return Status.FAILED
        val myPos = `object`.getPosition()
        val enemies = `object`.objects.filter { obj ->
            obj !== `object` && obj.isAlive && obj.team != `object`.team &&
                obj.getPosition().dst(myPos) < detectionRange
        }
        // Prefer units over bases
        val nearby = enemies.lastOrNull { it !is BaseEntity }
            ?: enemies.lastOrNull()
            ?: return Status.FAILED

        `object`.target(nearby)
        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
