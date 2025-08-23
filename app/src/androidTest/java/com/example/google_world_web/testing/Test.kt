package com.example.google_world_web.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.google_world_web.MainActivity
import org.junit.Rule
import org.junit.Test

class Test {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testReportProblemSubmission() {
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        composeTestRule.onNodeWithText("Report Problem").assertIsDisplayed().performClick()

        composeTestRule.onNodeWithText("Describe your problem...").performClick().performTextInput("Test problem from Compose")

        composeTestRule.onNodeWithText("SUBMIT PROBLEM").performClick()

        composeTestRule.onNodeWithText("Problem submitted successfully!").assertIsDisplayed()
    }
}
