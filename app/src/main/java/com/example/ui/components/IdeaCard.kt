package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColorOptions
import com.example.data.model.IdeaMemo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeaCard(
    idea: IdeaMemo,
    searchQuery: String = "",
    onCardClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onImportanceChanged: (Int) -> Unit = {},
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    // Determine background color
    val colorOption = remember(idea.colorHex) {
        ColorOptions.find { it.lightHex.equals(idea.colorHex, ignoreCase = true) }
    }
    val cardBgColor = if (colorOption != null) {
        val hex = if (isDark) colorOption.darkHex else colorOption.lightHex
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.surface
        }
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("idea_card_${idea.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (idea.isPinned) 4.dp else 1.dp,
            pressedElevation = 6.dp
        ),
        border = BorderStroke(
            width = if (idea.isPinned) 1.5.dp else 1.dp,
            color = if (idea.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category & Action Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = idea.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Voice Badge
                    if (idea.isVoiceRecorded) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "음성 기록",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }

                // Header icons (Pin, Star, More)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp).testTag("pin_button_${idea.id}")
                    ) {
                        Icon(
                            imageVector = if (idea.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (idea.isPinned) "고정 해제" else "상단 고정",
                            tint = if (idea.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp).testTag("favorite_button_${idea.id}")
                    ) {
                        Icon(
                            imageVector = if (idea.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (idea.isFavorite) "즐겨찾기 해제" else "즐겨찾기",
                            tint = if (idea.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp).testTag("more_button_${idea.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("아이디어 공유") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, idea.title)
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "💡 [${idea.title}]\n\n${idea.content}\n\n#태그: ${idea.tags.joinToString(" ")}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "아이디어 공유하기"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("복제하기") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제하기", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title with search highlight
            val titleText = idea.displayTitle
            if (searchQuery.isNotBlank() && titleText.contains(searchQuery, ignoreCase = true)) {
                Text(
                    text = buildHighlightedString(titleText, searchQuery, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Content snippet
            if (idea.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                if (searchQuery.isNotBlank() && idea.content.contains(searchQuery, ignoreCase = true)) {
                    Text(
                        text = buildHighlightedString(idea.content, searchQuery, MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                } else {
                    Text(
                        text = idea.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }

            // Tags Row
            if (idea.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    idea.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onTagClick(tag) }
                        ) {
                            Text(
                                text = "#${tag.trim().removePrefix("#")}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Importance Star Rating (Interactive) & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1..5 Star Rating
                ImportanceStarRating(
                    importance = idea.importance,
                    onImportanceChanged = { newRating -> onImportanceChanged(newRating) },
                    starSize = 16.dp,
                    isInteractive = true,
                    modifier = Modifier.testTag("card_importance_${idea.id}")
                )

                Text(
                    text = formatRelativeTime(idea.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun buildHighlightedString(
    text: String,
    highlight: String,
    highlightColor: Color
) = buildAnnotatedString {
    val startIndex = text.indexOf(highlight, ignoreCase = true)
    if (startIndex < 0) {
        append(text)
        return@buildAnnotatedString
    }
    val endIndex = startIndex + highlight.length
    append(text.substring(0, startIndex))
    withStyle(SpanStyle(background = highlightColor.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)) {
        append(text.substring(startIndex, endIndex))
    }
    append(text.substring(endIndex))
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minute = 60 * 1000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        diff < minute -> "방금 전"
        diff < hour -> "${diff / minute}분 전"
        diff < day -> "${diff / hour}시간 전"
        diff < 7 * day -> "${diff / day}일 전"
        else -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(timestamp))
    }
}
