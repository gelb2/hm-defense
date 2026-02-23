package fr.mesabloo.heavymachdefense.ai

import com.badlogic.gdx.ai.steer.Steerable
import com.badlogic.gdx.math.Vector2

/**
 * Abstract base class for all game objects that participate in the AI system.
 * Implements target tracking, health, and range-based awareness.
 *
 * Ported from the Scala rework-scala branch.
 */
abstract class GameObject : Steerable<Vector2> {
    abstract val objects: MutableList<GameObject>

    /** Pair of (attack range, detection range), or null if this object has no range. */
    abstract val range: Pair<Float, Float>?

    private var _target: GameObject? = null

    val hasTarget: Boolean get() = _target != null

    fun getTarget(): GameObject = _target
        ?: throw IllegalStateException("No target locked")

    open fun target(newTarget: GameObject) {
        if (_target != null) {
            throw IllegalArgumentException("Cannot target new object without forgetting the old one")
        }
        _target = newTarget
    }

    open fun forgetTarget() {
        _target = null
    }

    abstract val isAlive: Boolean

    abstract fun walk()

    abstract fun stopInPlace()
}
