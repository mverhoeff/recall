package com.recall.ui.kiln

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.recall.ui.theme.ClayBody
import com.recall.ui.theme.KilnTheme
import com.recall.ui.theme.TypePairing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KilnComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setKiln(content: @Composable () -> Unit) = rule.setContent {
        KilnTheme(ClayBody.SLATE, dark = false, pairing = TypePairing.B, reducedMotion = true, content = content)
    }

    private fun hasToggleState(state: ToggleableState) =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

    @Test
    fun toggleFlipsOnClick() {
        setKiln {
            var on by remember { mutableStateOf(false) }
            KilnToggle(on, { on = it }, "Haptic feedback")
        }
        val toggle = rule.onNode(isToggleable())
        toggle.assert(hasToggleState(ToggleableState.Off))
        toggle.performClick()
        toggle.assert(hasToggleState(ToggleableState.On))
    }

    @Test
    fun tabsSelectOnClick() {
        setKiln {
            var selected by remember { mutableStateOf(0) }
            KilnTabs(listOf("System", "Light", "Dark"), selected, onSelect = { selected = it })
        }
        rule.onNodeWithText("System").assertIsSelected()
        rule.onNodeWithText("Dark").assertIsNotSelected().performClick()
        rule.onNodeWithText("Dark").assertIsSelected()
        rule.onNodeWithText("System").assertIsNotSelected()
    }

    @Test
    fun disabledButtonIgnoresClicks() {
        var clicks = 0
        setKiln { KilnButton("Start review", { clicks++ }, enabled = false) }
        rule.onNodeWithText("Start review").assertIsNotEnabled().performClick()
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun textFieldAcceptsTypedText() {
        var typed = ""
        setKiln {
            var value by remember { mutableStateOf("") }
            KilnTextField(value, { value = it; typed = it }, "Deck name")
        }
        rule.onNode(hasSetTextAction()).performTextInput("World capitals")
        rule.onNode(hasSetTextAction()).assertTextContains("World capitals")
        rule.runOnIdle { assertEquals("World capitals", typed) }
    }
}
