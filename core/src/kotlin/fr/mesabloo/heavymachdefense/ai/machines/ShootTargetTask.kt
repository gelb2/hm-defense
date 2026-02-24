package fr.mesabloo.heavymachdefense.ai.machines

import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import fr.mesabloo.heavymachdefense.ai.GameObject

class ShootTargetTask : LeafTask<GameObject>() {
    override fun execute(): Status {
        val target = `object`.getTarget()
        `object`.aimAt(target)

        if (`object`.shootCooldownRemaining > 0f) return Status.SUCCEEDED

        `object`.shootCooldownRemaining = 1f / `object`.attackSpeed
        `object`.onShoot?.invoke(`object`, target)

        return Status.SUCCEEDED
    }

    override fun copyTo(task: Task<GameObject>): Task<GameObject> = task
}
