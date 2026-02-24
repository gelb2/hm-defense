package fr.mesabloo.heavymachdefense.ui.stage.game

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Array as GdxArray

class Bullet(
    region: TextureRegion,
    private val startPos: Vector2,
    private val targetPos: Vector2,
    private val speed: Float,
    private val damage: Int,
    private val targetAliveCheck: () -> Boolean,
    private val onHit: (Vector2) -> Unit,
    private val onMiss: () -> Unit,
    private val trailRegions: GdxArray<TextureAtlas.AtlasRegion>? = null,
    private val trailInterval: Float = 0.04f,
    private val trailScale: Float = 0.5f
) : Image(region) {

    init {
        setPosition(startPos.x - width / 2f, startPos.y - height / 2f)
        setOrigin(width / 2f, height / 2f)

        val angle = MathUtils.atan2(targetPos.y - startPos.y, targetPos.x - startPos.x)
        rotation = angle * MathUtils.radiansToDegrees

        addAction(object : Action() {
            private var elapsed = 0f
            private val totalDistance = startPos.dst(targetPos).coerceAtLeast(1f)
            private val totalTime = totalDistance / speed
            private var trailTimer = 0f

            override fun act(delta: Float): Boolean {
                elapsed += delta
                val progress = (elapsed / totalTime).coerceIn(0f, 1f)

                val currentX = MathUtils.lerp(startPos.x, targetPos.x, progress)
                val currentY = MathUtils.lerp(startPos.y, targetPos.y, progress)
                actor.setPosition(currentX - actor.width / 2f, currentY - actor.height / 2f)

                // Spawn smoke trail
                if (trailRegions != null && progress < 1f) {
                    trailTimer += delta
                    while (trailTimer >= trailInterval) {
                        trailTimer -= trailInterval
                        val smoke = ExplosionEffect(trailRegions, 0.04f)
                        smoke.setScale(trailScale)
                        smoke.setPosition(
                            currentX - smoke.width * trailScale / 2f,
                            currentY - smoke.height * trailScale / 2f
                        )
                        actor.parent?.addActor(smoke)
                    }
                }

                if (progress >= 1f) {
                    if (targetAliveCheck()) {
                        onHit(Vector2(currentX, currentY))
                    } else {
                        onMiss()
                    }
                    actor.remove()
                    return true
                }
                return false
            }
        })
    }
}
