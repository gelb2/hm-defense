package fr.mesabloo.heavymachdefense.ui.stage.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor

class ExplosionEffect(
    regions: com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion>,
    frameDuration: Float = 0.05f,
    private val additive: Boolean = false
) : Actor() {

    private val animation = Animation(frameDuration, regions, Animation.PlayMode.NORMAL)
    private var stateTime = 0f

    init {
        val first = regions.first()
        setSize(first.regionWidth.toFloat(), first.regionHeight.toFloat())
        setOrigin(width / 2f, height / 2f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        stateTime += delta
        if (animation.isAnimationFinished(stateTime)) {
            remove()
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val frame: TextureRegion = animation.getKeyFrame(stateTime)
        if (additive) {
            batch.flush()
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        }
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        batch.draw(frame, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
        if (additive) {
            batch.flush()
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
    }
}
