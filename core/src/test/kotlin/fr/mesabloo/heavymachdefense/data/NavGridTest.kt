package fr.mesabloo.heavymachdefense.data

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.math.Vector2
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for NavGrid flow field computation.
 * Requires HeadlessApplication for LibGDX Vector2 math.
 */
class NavGridTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initGdx() {
            val config = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, config)
        }
    }

    /** Create a fully open grid (all walkable). */
    private fun openGrid(): NavGrid {
        val grid = List(NavGrid.ROWS) { List(NavGrid.COLS) { true } }
        return NavGrid(NavGridJson(terrainId = 0, cols = NavGrid.COLS, rows = NavGrid.ROWS, tileSize = NavGrid.TILE_SIZE, grid = grid))
    }

    /** Create a grid with a horizontal wall blocking row [wallRow], leaving a gap at [gapCol]. */
    private fun wallWithGapGrid(wallRow: Int, gapCol: Int): NavGrid {
        val grid = List(NavGrid.ROWS) { r ->
            List(NavGrid.COLS) { c ->
                if (r == wallRow && c != gapCol) false else true
            }
        }
        return NavGrid(NavGridJson(terrainId = 0, cols = NavGrid.COLS, rows = NavGrid.ROWS, tileSize = NavGrid.TILE_SIZE, grid = grid))
    }

    /** Create a grid that is fully blocked. */
    private fun blockedGrid(): NavGrid {
        val grid = List(NavGrid.ROWS) { List(NavGrid.COLS) { false } }
        return NavGrid(NavGridJson(terrainId = 0, cols = NavGrid.COLS, rows = NavGrid.ROWS, tileSize = NavGrid.TILE_SIZE, grid = grid))
    }

    // --- Open grid: straight-line flow ---

    @Test
    fun `open grid flowToBase points downward (negative Y in row space)`() {
        val nav = openGrid()
        // Middle tile, not on seed row
        val flow = nav.flowToBase[NavGrid.ROWS / 2][NavGrid.COLS / 2]
        assertNotNull(flow, "Flow should exist for walkable tile")
        // flowToBase seed = row 63 (bottom), so flow should point toward increasing row = -Y world
        assertTrue(flow.y < 0f, "flowToBase Y should be negative (pointing down in world), got ${flow.y}")
    }

    @Test
    fun `open grid flowToTop points upward (positive Y in row space)`() {
        val nav = openGrid()
        val flow = nav.flowToTop[NavGrid.ROWS / 2][NavGrid.COLS / 2]
        assertNotNull(flow, "Flow should exist for walkable tile")
        assertTrue(flow.y > 0f, "flowToTop Y should be positive (pointing up in world), got ${flow.y}")
    }

    @Test
    fun `open grid center column has near-zero X component`() {
        val nav = openGrid()
        val flow = nav.flowToBase[NavGrid.ROWS / 2][NavGrid.COLS / 2]
        assertNotNull(flow)
        // Center column should have minimal X drift (continuous gradient is symmetric)
        assertTrue(abs(flow.x) < 0.15f, "Center column X should be near zero, got ${flow.x}")
    }

    // --- Flow field vectors are normalized ---

    @Test
    fun `all flow vectors are unit length`() {
        val nav = openGrid()
        for (r in 0 until NavGrid.ROWS) {
            for (c in 0 until NavGrid.COLS) {
                val flow = nav.flowToBase[r][c] ?: continue
                val len = flow.len()
                assertTrue(abs(len - 1f) < 0.01f, "Flow at ($r,$c) has length $len, expected ~1.0")
            }
        }
    }

    // --- Blocked grid ---

    @Test
    fun `fully blocked grid has null flow everywhere`() {
        val nav = blockedGrid()
        for (r in 0 until NavGrid.ROWS) {
            for (c in 0 until NavGrid.COLS) {
                assertNull(nav.flowToBase[r][c], "Blocked tile ($r,$c) should have null flow")
            }
        }
    }

    // --- Walkability ---

    @Test
    fun `isWalkable returns true for open grid`() {
        val nav = openGrid()
        assertTrue(nav.isWalkable(0, 0))
        assertTrue(nav.isWalkable(NavGrid.ROWS - 1, NavGrid.COLS - 1))
    }

    @Test
    fun `isWalkable returns false for out of bounds`() {
        val nav = openGrid()
        assertFalse(nav.isWalkable(-1, 0))
        assertFalse(nav.isWalkable(NavGrid.ROWS, 0))
        assertFalse(nav.isWalkable(0, -1))
        assertFalse(nav.isWalkable(0, NavGrid.COLS))
    }

    @Test
    fun `isWalkable returns false for blocked tile in wall grid`() {
        val nav = wallWithGapGrid(wallRow = 32, gapCol = 8)
        assertFalse(nav.isWalkable(32, 0))
        assertFalse(nav.isWalkable(32, 7))
        assertTrue(nav.isWalkable(32, 8))  // gap
        assertFalse(nav.isWalkable(32, 9))
    }

    // --- Wall with gap: flow should route through gap ---

    @Test
    fun `flow routes through gap in wall`() {
        val nav = wallWithGapGrid(wallRow = 32, gapCol = 8)

        // Tile above wall, same column as gap → should still point downward
        val flowAtGap = nav.flowToBase[31][8]
        assertNotNull(flowAtGap, "Tile above gap should have flow")
        assertTrue(flowAtGap.y < 0f, "Flow above gap should point down, got Y=${flowAtGap.y}")

        // Tile above wall, far from gap → should have X component toward gap
        val flowFarLeft = nav.flowToBase[31][2]
        assertNotNull(flowFarLeft, "Tile far from gap should still have flow (reachable via other routes)")
        // Flow X should be positive (pointing right toward gap at col 8)
        assertTrue(flowFarLeft.x > 0f, "Flow left of gap should point right toward gap, got X=${flowFarLeft.x}")

        // Tile above wall, far right → should have X component toward gap (negative X)
        val flowFarRight = nav.flowToBase[31][14]
        assertNotNull(flowFarRight)
        assertTrue(flowFarRight.x < 0f, "Flow right of gap should point left toward gap, got X=${flowFarRight.x}")
    }

    // --- Unit movement simulation: no stuck/oscillation ---

    @Test
    fun `simulated unit reaches target row in open grid without oscillation`() {
        val nav = openGrid()
        val ppm = fr.mesabloo.heavymachdefense.PPM

        // Start at middle of terrain in world coords
        var worldX = (NavGrid.COLS / 2f * NavGrid.TILE_SIZE) / ppm
        var worldY = (NavGrid.ROWS / 2f * NavGrid.TILE_SIZE) / ppm
        val speed = 10f / ppm  // units per step
        val targetWorldY = (2f * NavGrid.TILE_SIZE) / ppm  // near bottom (ally base)

        var steps = 0
        val maxSteps = 2000
        val posHistory = mutableListOf<Pair<Float, Float>>()

        while (worldY > targetWorldY && steps < maxSteps) {
            val flow = nav.getFlowToBase(worldX, worldY) ?: break
            worldX += flow.x * speed
            worldY += flow.y * speed
            posHistory.add(worldX to worldY)
            steps++
        }

        assertTrue(steps < maxSteps, "Unit should reach target within $maxSteps steps, took $steps")
        assertTrue(worldY <= targetWorldY, "Unit should reach target Y, ended at $worldY")

        // Check for oscillation: Y should be monotonically decreasing (with small tolerance)
        var oscillations = 0
        for (i in 1 until posHistory.size) {
            if (posHistory[i].second > posHistory[i - 1].second + 0.01f) {
                oscillations++
            }
        }
        assertTrue(oscillations < 5, "Unit should not oscillate, found $oscillations reversals")
    }

    @Test
    fun `simulated unit navigates through wall gap without getting stuck`() {
        val nav = wallWithGapGrid(wallRow = 32, gapCol = 8)
        val ppm = fr.mesabloo.heavymachdefense.PPM

        // Start above the wall, far from gap (col 2)
        var worldX = (2.5f * NavGrid.TILE_SIZE) / ppm
        var worldY = ((NavGrid.ROWS - 20f) * NavGrid.TILE_SIZE) / ppm  // row ~20 from top
        val speed = 5f / ppm
        val targetWorldY = (2f * NavGrid.TILE_SIZE) / ppm

        var steps = 0
        val maxSteps = 5000

        while (worldY > targetWorldY && steps < maxSteps) {
            val flow = nav.getFlowToBase(worldX, worldY)
            if (flow == null) break
            worldX += flow.x * speed
            worldY += flow.y * speed
            steps++
        }

        assertTrue(steps < maxSteps, "Unit should navigate through gap within $maxSteps steps, took $steps")
        assertTrue(worldY <= targetWorldY, "Unit should reach target, ended at Y=$worldY")
    }
}
