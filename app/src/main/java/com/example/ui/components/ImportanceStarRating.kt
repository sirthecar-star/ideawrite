package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1~5개 별점 중요도 표시 및 선택 컴포넌트
 */
@Composable
fun ImportanceStarRating(
    importance: Int, // 1 ~ 5
    onImportanceChanged: ((Int) -> Unit)? = null,
    starSize: Dp = 20.dp,
    activeColor: Color = Color(0xFFF59E0B), // 따뜻한 앰버 골드
    inactiveColor: Color = Color(0xFFCBD5E1), // 부드러운 회색
    modifier: Modifier = Modifier,
    isInteractive: Boolean = onImportanceChanged != null
) {
    val currentImportance = importance.coerceIn(1, 5)

    Row(
        modifier = modifier.semantics {
            contentDescription = "중요도 ${currentImportance}점"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (starIndex in 1..5) {
            val isFilled = starIndex <= currentImportance
            val icon = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline
            val tint = if (isFilled) activeColor else inactiveColor

            val starModifier = if (isInteractive && onImportanceChanged != null) {
                Modifier
                    .size(starSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = starSize)
                    ) {
                        onImportanceChanged(starIndex)
                    }
                    .testTag("star_rating_$starIndex")
            } else {
                Modifier.size(starSize)
            }

            Icon(
                imageVector = icon,
                contentDescription = if (isInteractive) "중요도 $starIndex 별점" else null,
                tint = tint,
                modifier = starModifier
            )
        }
    }
}
