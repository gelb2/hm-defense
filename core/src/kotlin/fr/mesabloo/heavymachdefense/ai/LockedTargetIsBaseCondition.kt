package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task

/** Returns SUCCEEDED if the currently locked target is a [BaseEntity]. */
class LockedTargetIsBaseCondition : LeafTask<GameObject>() {
    override fun execute(): Status =
        if (`object`.hasTarget && `object`.getTarget() is BaseEntity) Status.SUCCEEDED
        else Status.FAILED

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
