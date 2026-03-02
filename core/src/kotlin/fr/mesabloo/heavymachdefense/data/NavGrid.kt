package fr.mesabloo.heavymachdefense.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import fr.mesabloo.heavymachdefense.PPM
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.LinkedList

/**
 * Navigation grid for terrain obstacle avoidance.
 *
 * Each terrain is divided into 16×64 tiles (32px each).
 * [grid]\[row]\[col] = true means walkable, false means blocked.
 * Row 0 = top of terrain (enemy spawn side), row 63 = bottom (ally base side).
 *
 * The [flowField] precomputes a movement direction for every walkable tile
 * so enemies can navigate around obstacles in O(1) per frame.
 */
class NavGrid(private val data: NavGridJson) {

    companion object {
        const val TILE_SIZE = 32  // pixels
        const val COLS = 16
        const val ROWS = 64

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Load navgrid for a stage level (1-80).
         * Maps stage level → terrain background ID via [getBackgroundForLevel].
         */
        fun load(level: Int): NavGrid? {
            val terrainId = getBackgroundForLevel(level)
            val path = "data/navgrid/terrain-${terrainId.toString().padStart(2, '0')}.json"
            val file = Gdx.files.internal(path)
            if (!file.exists()) return null
            val data = json.decodeFromString<NavGridJson>(file.readString())
            return NavGrid(data)
        }
    }

    /** Flow toward ally base (row 63, bottom). Used by enemies. */
    val flowToBase: Array<Array<Vector2?>> = Array(ROWS) { Array(COLS) { null } }

    /** Flow toward enemy base (row 0, top). Used by ally machines. */
    val flowToTop: Array<Array<Vector2?>> = Array(ROWS) { Array(COLS) { null } }

    init {
        buildFlowField(seedRow = ROWS - 1, output = flowToBase, defaultDir = Vector2(0f, -1f))
        buildFlowField(seedRow = 0, output = flowToTop, defaultDir = Vector2(0f, 1f))
    }

    fun isWalkable(row: Int, col: Int): Boolean {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return false
        return data.grid[row][col]
    }

    /**
     * Get the flow direction toward the ally base (enemies use this).
     */
    fun getFlowToBase(worldX: Float, worldY: Float): Vector2? {
        val col = worldToCol(worldX)
        val row = worldToRow(worldY)
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null
        return flowToBase[row][col]
    }

    /**
     * Get the flow direction toward the enemy base (ally machines use this).
     */
    fun getFlowToTop(worldX: Float, worldY: Float): Vector2? {
        val col = worldToCol(worldX)
        val row = worldToRow(worldY)
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null
        return flowToTop[row][col]
    }

    /**
     * Check if a world position is on a blocked tile.
     */
    fun isBlockedAt(worldX: Float, worldY: Float): Boolean {
        val col = worldToCol(worldX)
        val row = worldToRow(worldY)
        return !isWalkable(row, col)
    }

    // --- Coordinate conversions ---
    // Terrain pixels: Y=0 at bottom (LibGDX), Y=2048 at top
    // NavGrid JSON:   row 0 = top of image (Y=2048px), row 63 = bottom (Y=0px)
    // World units:    pixels / PPM

    private fun worldToCol(worldX: Float): Int = ((worldX * PPM) / TILE_SIZE).toInt().coerceIn(0, COLS - 1)

    /** Convert world Y to grid row. Top of terrain = row 0, bottom = row 63. */
    private fun worldToRow(worldY: Float): Int {
        val pixelY = worldY * PPM  // 0 = bottom, 2048 = top
        // Invert: row 0 = top (pixelY=2048), row 63 = bottom (pixelY=0)
        return ((ROWS - 1) - (pixelY / TILE_SIZE).toInt()).coerceIn(0, ROWS - 1)
    }

    /**
     * BFS from all walkable tiles on [seedRow] to build a flow field.
     * Each tile stores a normalized direction pointing toward the nearest path to [seedRow].
     *
     * @param seedRow the target row (63 = ally base for enemies, 0 = enemy base for machines)
     * @param output the flow field array to populate
     * @param defaultDir fallback direction for seed row tiles
     */
    private fun buildFlowField(seedRow: Int, output: Array<Array<Vector2?>>, defaultDir: Vector2) {
        val dist = Array(ROWS) { IntArray(COLS) { Int.MAX_VALUE } }
        val queue = LinkedList<Pair<Int, Int>>()

        for (c in 0 until COLS) {
            if (isWalkable(seedRow, c)) {
                dist[seedRow][c] = 0
                queue.add(Pair(seedRow, c))
            }
        }

        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)
        while (queue.isNotEmpty()) {
            val (r, c) = queue.poll()
            for (d in 0..3) {
                val nr = r + dr[d]
                val nc = c + dc[d]
                if (nr in 0 until ROWS && nc in 0 until COLS
                    && isWalkable(nr, nc) && dist[nr][nc] > dist[r][c] + 1
                ) {
                    dist[nr][nc] = dist[r][c] + 1
                    queue.add(Pair(nr, nc))
                }
            }
        }

        val tmpVec = Vector2()
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (!isWalkable(r, c) || dist[r][c] == Int.MAX_VALUE) continue

                if (r == seedRow) {
                    output[r][c] = defaultDir.cpy()
                    continue
                }

                tmpVec.set(0f, 0f)
                var bestDist = dist[r][c]

                for (d in 0..3) {
                    val nr = r + dr[d]
                    val nc = c + dc[d]
                    if (nr in 0 until ROWS && nc in 0 until COLS && dist[nr][nc] < bestDist) {
                        bestDist = dist[nr][nc]
                        // Grid row+1 = down in image = lower Y in world → world Y = -1
                        tmpVec.set(dc[d].toFloat(), -dr[d].toFloat())
                    }
                }

                if (tmpVec.len2() > 0f) {
                    output[r][c] = tmpVec.cpy().nor()
                } else {
                    output[r][c] = defaultDir.cpy()
                }
            }
        }
    }
}

@Serializable
data class NavGridJson(
    val terrainId: Int,
    val cols: Int,
    val rows: Int,
    val tileSize: Int,
    val grid: List<List<Boolean>>
)
