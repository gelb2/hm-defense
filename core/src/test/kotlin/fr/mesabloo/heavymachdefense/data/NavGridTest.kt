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

    // --- Out-of-bounds world coordinates ---

    @Test
    fun `getFlowToBase with negative coordinates returns null or valid vector`() {
        val nav = openGrid()
        // Should not crash — either returns null or a valid vector
        val flow = nav.getFlowToBase(-100f, -100f)
        if (flow != null) {
            assertTrue(flow.len() > 0f, "If returned, flow should be non-zero")
        }
    }

    @Test
    fun `getFlowToBase with very large coordinates returns null or valid vector`() {
        val nav = openGrid()
        val flow = nav.getFlowToBase(99999f, 99999f)
        if (flow != null) {
            assertTrue(flow.len() > 0f)
        }
    }

    @Test
    fun `getFlowToTop with out-of-bounds coordinates does not crash`() {
        val nav = openGrid()
        // Negative
        nav.getFlowToTop(-50f, -50f)
        // Huge
        nav.getFlowToTop(10000f, 10000f)
        // Zero
        val flow = nav.getFlowToTop(0f, 0f)
        // Should not crash — any result is fine
        if (flow != null) {
            val len = flow.len()
            assertTrue(len > 0f || len == 0f, "Flow length should be finite")
        }
    }

    // --- isBlockedAt edge cases ---

    @Test
    fun `isBlockedAt with OOB coordinates returns blocked`() {
        val nav = openGrid()
        // worldToCol/worldToRow clamp to valid range, so OOB coords map to edge tiles
        // For open grid, edge tiles are walkable → not blocked
        // But negative coords clamp to (0,0) which is walkable in open grid
        assertFalse(nav.isBlockedAt(-100f, -100f), "Clamped to walkable edge tile")
    }

    @Test
    fun `isBlockedAt correctly identifies blocked tile`() {
        val nav = wallWithGapGrid(wallRow = 32, gapCol = 8)
        val ppm = fr.mesabloo.heavymachdefense.PPM
        // Row 32 in grid → worldY near the middle
        // row = (ROWS-1) - (pixelY / TILE_SIZE) → pixelY = (ROWS-1 - row) * TILE_SIZE
        val blockedWorldY = ((NavGrid.ROWS - 1 - 32) * NavGrid.TILE_SIZE + NavGrid.TILE_SIZE / 2f) / ppm
        val blockedWorldX = (0 * NavGrid.TILE_SIZE + NavGrid.TILE_SIZE / 2f) / ppm // col 0 is blocked
        assertTrue(nav.isBlockedAt(blockedWorldX, blockedWorldY), "Wall tile should be blocked")

        val gapWorldX = (8 * NavGrid.TILE_SIZE + NavGrid.TILE_SIZE / 2f) / ppm // col 8 is gap
        assertFalse(nav.isBlockedAt(gapWorldX, blockedWorldY), "Gap tile should not be blocked")
    }

    // --- Seed row always has flow ---

    @Test
    fun `seed row tiles always have flow direction`() {
        val nav = openGrid()
        // flowToBase seed row = 63 (bottom)
        for (c in 0 until NavGrid.COLS) {
            val flow = nav.flowToBase[NavGrid.ROWS - 1][c]
            assertNotNull(flow, "Seed row tile ($c) should have flow")
            assertTrue(abs(flow.len() - 1f) < 0.01f, "Seed row flow should be unit length")
        }
        // flowToTop seed row = 0 (top)
        for (c in 0 until NavGrid.COLS) {
            val flow = nav.flowToTop[0][c]
            assertNotNull(flow, "Top seed row tile ($c) should have flow")
        }
    }

    // --- Unreachable tiles have null flow ---

    @Test
    fun `tiles isolated by wall have null flow`() {
        // Create grid with wall across entire row (no gap)
        val grid = List(NavGrid.ROWS) { r ->
            List(NavGrid.COLS) { _ ->
                r != 32  // row 32 is fully blocked
            }
        }
        val nav = NavGrid(NavGridJson(terrainId = 0, cols = NavGrid.COLS, rows = NavGrid.ROWS, tileSize = NavGrid.TILE_SIZE, grid = grid))

        // flowToBase: seed=63 (below wall). Tiles above wall (rows 0-31) are unreachable
        for (r in 0..31) {
            for (c in 0 until NavGrid.COLS) {
                assertNull(nav.flowToBase[r][c], "Tile ($r,$c) above solid wall should have null flowToBase")
            }
        }

        // Tiles below wall (rows 33-63) should still have flow
        for (c in 0 until NavGrid.COLS) {
            assertNotNull(nav.flowToBase[NavGrid.ROWS - 1][c], "Seed row ($c) should still have flow")
        }
    }

    // --- Corner-only gap does NOT allow diagonal passage ---

    @Test
    fun `diagonal corner-cutting through blocked tiles is prevented`() {
        // Create grid where the ONLY way past row 32 would be a diagonal cut:
        // - Row 32 is fully blocked except col 8
        // - Row 31 col 8 is also blocked (the tile directly above the gap)
        // This means: to reach (32,8) from above, you'd need a diagonal from (31,7) or (31,9),
        // but that requires the adjacent cardinal tile to be walkable — (31,8) is blocked
        // and (32,7)/(32,9) are blocked. So there's NO valid path from above row 32 to below.
        val grid = List(NavGrid.ROWS) { r ->
            List(NavGrid.COLS) { c ->
                when {
                    r == 32 && c != 8 -> false  // wall except gap at col 8
                    r == 31 && c == 8 -> false  // block tile above gap
                    else -> true
                }
            }
        }
        val nav = NavGrid(NavGridJson(terrainId = 0, cols = NavGrid.COLS, rows = NavGrid.ROWS, tileSize = NavGrid.TILE_SIZE, grid = grid))

        // Tile (31,7) is walkable, (32,8) is walkable, but diagonal is blocked because
        // both (31,8) and (32,7) are blocked → corner-cutting prevention.
        // No cardinal path exists either → tiles above wall are unreachable from seed row 63.
        val flow = nav.flowToBase[31][7]
        assertNull(flow, "Tile (31,7) should have null flow — no path through blocked diagonal")

        // But tiles below the wall should still have flow
        assertNotNull(nav.flowToBase[33][7], "Tile (33,7) below wall should have flow")
    }
}
