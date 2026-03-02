package fr.mesabloo.heavymachdefense.data.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader

/**
 * A class to hold very low-level construction-oriented sizes, offsets etc. to correctly create machines dynamically.
 * The specification format is a JSON file of this type:
 *
 * ```
 * {
 *   "body": {
 *     "size": float[2]
 *   },
 *   "weapons": {
 *     "left": {
 *       "size": float[2],
 *       "offset": float[2]
 *     },
 *     "right": {
 *       "size": float[2],
 *       "offset": float[2]
 *     }
 *   },
 *   "feet": {
 *     "regions": string[],  // region names in feet.atlas for animation frames
 *     "offset": float[2]    // offset from center of body for left foot (right foot is Y-mirrored)
 *   }
 * }
 * ```
 *
 * Where:
 * - the `size` fields are pairs of floats representing the size on the X and Y axes.
 * - the `offset` fields are pairs of floats representing the distance from the center of the body on the X and Y axes.
 *
 * This JSON file must be named `${name}-${level%02d}.json` and must be located in the
 * `data/models/machines` internal folder.
 *
 * @param name The machine name, obtained from the
 * [fr.mesabloo.heavymachdefense.data.MachineKind.machineName] property
 * @param level Which level (between the lowest and the highest, most likely to be 0 to 10) to construct the machine for
 */
class MachineModel(name: String, level: Int) {
    /**
     * The position of the main body on the X and Y axes, in pixels.
     */
    val bodySize: Pair<Float, Float>

    /**
     * The size of the left weapon on the X and Y axes, in pixels.
     */
    val leftWeaponSize: Pair<Float, Float>

    /**
     * The offset from the center of the main body, in pixels.
     */
    val leftWeaponOffset: Pair<Float, Float>

    /**
     * The size of the left weapon on the X and Y axes, in pixels.
     */
    val rightWeaponSize: Pair<Float, Float>

    /**
     * The offset from the center of the main body, in pixels.
     */
    val rightWeaponOffset: Pair<Float, Float>

    /**
     * Region names in feet.atlas for walking animation frames, or null if no feet data.
     */
    val feetRegions: List<String>?

    /**
     * The offset from the center of the main body for the left foot, in pixels.
     * The right foot is positioned with Y-mirrored offset.
     */
    val feetOffset: Pair<Float, Float>?

    init {
        val handle = Gdx.files.internal("data/models/machines/${name}-${level.toString().padStart(2, '0')}.json")
        val data = JsonReader().parse(handle.readString())

        assert(data.isObject)

        val body = data.get("body")
        val weapons = data.get("weapons")
        val feet = data.get("feet")

        ////////

        val bodySize = body.get("size").asFloatArray()
        this.bodySize = Pair(bodySize[0], bodySize[1])

        val leftWeapon = weapons.get("left")
        val leftWeaponSize = leftWeapon.get("size").asFloatArray()
        val leftWeaponOffset = leftWeapon.get("offset").asFloatArray()
        this.leftWeaponSize = Pair(leftWeaponSize[0], leftWeaponSize[1])
        this.leftWeaponOffset = Pair(leftWeaponOffset[0], leftWeaponOffset[1])

        val rightWeapon = weapons.get("right")
        val rightWeaponSize = rightWeapon.get("size").asFloatArray()
        val rightWeaponOffset = rightWeapon.get("offset").asFloatArray()
        this.rightWeaponSize = Pair(rightWeaponSize[0], rightWeaponSize[1])
        this.rightWeaponOffset = Pair(rightWeaponOffset[0], rightWeaponOffset[1])

        if (feet != null && !feet.isArray) {
            val regions = feet.get("regions").asStringArray().toList()
            val offset = feet.get("offset").asFloatArray()
            this.feetRegions = regions
            this.feetOffset = Pair(offset[0], offset[1])
        } else {
            this.feetRegions = null
            this.feetOffset = null
        }
    }

}