package com.recall.ui.theme

import com.recall.ui.theme.ShadowRole.InnerLight
import com.recall.ui.theme.ShadowRole.InnerShade
import com.recall.ui.theme.ShadowRole.ShadowDark
import com.recall.ui.theme.ShadowRole.ShadowLight
import org.junit.Assert.assertEquals
import org.junit.Test

class KilnDepthTest {

    @Test
    fun mold2MatchesSpec() {
        assertEquals(
            DepthSpec(
                ShadowLayer(9f, 9f, 22f, 0f, ShadowDark),
                ShadowLayer(-8f, -8f, 20f, 0f, ShadowLight),
                ShadowLayer(0f, 0f, 0f, 0f, InnerLight),
                ShadowLayer(0f, 0f, 0f, 0f, InnerShade),
            ),
            Depth.Mold2.spec(),
        )
    }

    @Test
    fun float1MatchesSpec() {
        assertEquals(
            DepthSpec(
                ShadowLayer(0f, 4f, 10f, -3f, ShadowDark),
                ShadowLayer(0f, 1f, 2f, 0f, InnerShade),
                ShadowLayer(0f, 0f, 0f, 0f, InnerLight),
                ShadowLayer(0f, 0f, 0f, 0f, InnerShade),
            ),
            Depth.Float1.spec(),
        )
    }

    @Test
    fun dipSmMatchesSpec() {
        assertEquals(
            DepthSpec(
                ShadowLayer(0f, 0f, 0f, 0f, ShadowDark),
                ShadowLayer(0f, 0f, 0f, 0f, ShadowLight),
                ShadowLayer(3f, 3f, 7f, 0f, ShadowDark),
                ShadowLayer(-3f, -3f, 7f, 0f, ShadowLight),
            ),
            Depth.DipSm.spec(),
        )
    }

    @Test
    fun raisedDepthsHaveNoInnerRim() {
        val raised = listOf(Depth.Mold1, Depth.Mold2, Depth.Mold3, Depth.Float1, Depth.Float2, Depth.Float3)
        for (depth in raised) {
            val spec = depth.spec()
            for (layer in listOf(spec.s3, spec.s4)) {
                assertEquals("$depth", listOf(0f, 0f, 0f, 0f), listOf(layer.x, layer.y, layer.blur, layer.spread))
            }
        }
    }

    @Test
    fun floatSlotRoles() {
        for (depth in listOf(Depth.Float1, Depth.Float2, Depth.Float3)) {
            assertEquals("$depth", listOf(ShadowDark, InnerShade, InnerLight, InnerShade), depth.spec().layers.map { it.role })
        }
    }

    @Test
    fun lerpEndpointsMatchSpecs() {
        assertEquals(Depth.Mold1.spec(), lerp(Depth.Mold1.spec(), Depth.DipSm.spec(), 0f))
        assertEquals(Depth.DipSm.spec(), lerp(Depth.Mold1.spec(), Depth.DipSm.spec(), 1f))
    }

    @Test
    fun lerpTakesTargetRoleFromHalfway() {
        val a = Depth.Float1.spec()
        val b = Depth.Mold1.spec()
        assertEquals(InnerShade, lerp(a, b, 0.49f).s2.role)
        assertEquals(ShadowLight, lerp(a, b, 0.5f).s2.role)
    }

    @Test
    fun offGroundMoldsFloatAndDipsGoFlat() {
        assertEquals(Depth.Float1, Depth.Mold2.resolve(onGround = false))
        assertEquals(Depth.Flat, Depth.DipSm.resolve(onGround = false))
        assertEquals(Depth.Flat, Depth.Dip.resolve(onGround = false))
        assertEquals(Depth.Float2, Depth.Float2.resolve(onGround = false))
    }

    @Test
    fun onGroundResolveIsIdentity() {
        for (depth in Depth.entries) assertEquals(depth, depth.resolve(onGround = true))
    }
}
