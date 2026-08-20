package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.IdeaMemo
import com.example.ui.components.IdeaCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun idea_card_screenshot() {
    val sampleIdea = IdeaMemo(
      id = 1,
      title = "💡 스마트 음성 메모 & 태그 관리",
      content = "갑자기 떠오르는 아이디어를 음성과 텍스트로 빠르게 메모하고 태그별로 정리하는 앱",
      category = "💡 창작/기획",
      tags = listOf("아이디어", "음성메모", "태그"),
      colorHex = "#FEF3C7",
      importance = 5,
      isPinned = true,
      isFavorite = true,
      isVoiceRecorded = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        IdeaCard(
          idea = sampleIdea,
          searchQuery = "",
          onCardClick = {},
          onTogglePin = {},
          onToggleFavorite = {},
          onDelete = {},
          onDuplicate = {},
          onTagClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
