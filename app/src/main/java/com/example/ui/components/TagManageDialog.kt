package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TagStat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManageDialog(
    allTags: List<TagStat>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onClearAllTags: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchTagText by remember { mutableStateOf("") }

    val filteredTags = remember(allTags, searchTagText) {
        if (searchTagText.isBlank()) allTags
        else allTags.filter { it.name.contains(searchTagText.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "태그 관리 및 필터",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "태그를 선택하면 해당 태그가 포함된 아이디어만 모아볼 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search or add tag field
                OutlinedTextField(
                    value = searchTagText,
                    onValueChange = { searchTagText = it },
                    placeholder = { Text("태그 검색...") },
                    modifier = Modifier.fillMaxWidth().testTag("tag_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchTagText.isNotEmpty()) {
                            IconButton(onClick = { searchTagText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "지우기")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Active Info
                if (selectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "선택된 필터 (${selectedTags.size}개)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onClearAllTags,
                            modifier = Modifier.testTag("clear_tags_button")
                        ) {
                            Text("필터 초기화")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "전체 태그 목록 (${allTags.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allTags.isEmpty()) "등록된 태그가 없습니다.\n아이디어 작성 시 태그를 추가해보세요!" else "검색된 태그가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredTags.forEach { tagStat ->
                            val isSelected = selectedTags.contains(tagStat.name)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleTag(tagStat.name) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${tagStat.name}", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "${tagStat.count}",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("tag_chip_${tagStat.name}")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_tag_dialog_button")
            ) {
                Text("확인")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
