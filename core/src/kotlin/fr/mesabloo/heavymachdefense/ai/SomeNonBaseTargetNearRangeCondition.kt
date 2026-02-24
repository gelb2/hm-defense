package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

/**
 * Returns SUCCEEDED if there is at least one non-base enemy
 * within detection range. Used to trigger target switching
 * from a base to a higher-priority unit.
 */
class SomeNonBaseTargetNearRangeCondition : LeafTask<GameObject>() {
    override fun execute(): Status {
        val detectionRange = `object`.range?.second ?: return Status.FAILED
        val hasNearby = `object`.objects.any { obj ->
            obj !== `object` && obj.isAlive && obj.team != `object`.team &&
                obj !is BaseEntity &&
                obj.getPosition().dst(`object`.getPosition()) < detectionRange
        }
        return if (hasNearby) Status.SUCCEEDED else Status.FAILED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
