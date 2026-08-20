package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.SmartIdeaAssistant
import com.example.data.model.ColorOptions
import com.example.data.model.IdeaMemo
import com.example.data.model.PredefinedCategories
import com.example.speech.VoiceRecognitionManager
import com.example.speech.VoiceState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IdeaEditScreen(
    idea: IdeaMemo?,
    voiceRecognitionManager: VoiceRecognitionManager,
    onSave: (
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        colorHex: String,
        importance: Int,
        isPinned: Boolean,
        isFavorite: Boolean,
        id: Long
    ) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var title by remember { mutableStateOf(idea?.title ?: "") }
    var content by remember { mutableStateOf(idea?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(idea?.category ?: "💡 창작/기획") }
    var selectedColorHex by remember { mutableStateOf(idea?.colorHex ?: "#FFFFFF") }
    var importance by remember { mutableStateOf(idea?.importance ?: 3) }
    var isPinned by remember { mutableStateOf(idea?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(idea?.isFavorite ?: false) }

    val tags = remember { mutableStateListOf<String>().apply { idea?.tags?.let { addAll(it) } } }
    var newTagInput by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Voice dictation state
    val voiceState by voiceRecognitionManager.voiceState.collectAsState()
    var isVoiceDictating by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceDictating = true
            voiceRecognitionManager.startListening()
        }
    }

    // Append recognized voice text to content
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Success) {
            val spoken = (voiceState as VoiceState.Success).recognizedText
            if (spoken.isNotBlank()) {
                content = if (content.isBlank()) spoken else "$content\n$spoken"
                // Auto suggest tags/category if empty
                val suggestedTags = SmartIdeaAssistant.extractSuggestedTags(spoken)
                suggestedTags.forEach { if (!tags.contains(it)) tags.add(it) }
            }
            isVoiceDictating = false
            voiceRecognitionManager.reset()
        }
    }

    // Dynamic suggested tags based on title + content
    val suggestedTags = remember(title, content, tags) {
        val fullText = "$title $content"
        SmartIdeaAssistant.extractSuggestedTags(fullText).filter { !tags.contains(it) }
    }

    // Dynamic background color
    val colorOption = remember(selectedColorHex) {
        ColorOptions.find { it.lightHex.equals(selectedColorHex, ignoreCase = true) }
    }
    val screenBgColor = if (colorOption != null) {
        val hex = if (isDark) colorOption.darkHex else colorOption.lightHex
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.background }
    } else {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("idea_edit_screen"),
        containerColor = screenBgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (idea == null || idea.id == 0L) "새 아이디어" else "아이디어 편집",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("edit_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    // Color Picker Button
                    Box {
                        IconButton(
                            onClick = { showColorPicker = !showColorPicker },
                            modifier = Modifier.testTag("color_picker_button")
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "배경 색상 변경",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showColorPicker,
                            onDismissRequest = { showColorPicker = false }
                        ) {
                            Text(
                                text = "메모 색상 선택",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                            ColorOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(opt.lightHex)))
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(opt.name)
                                        }
                                    },
                                    onClick = {
                                        selectedColorHex = opt.lightHex
                                        showColorPicker = false
                                    }
                                )
                            }
                        }
                    }

                    // Pin toggle
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("edit_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "상단 고정",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Star toggle
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.testTag("edit_star_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share button
                    if (title.isNotBlank() || content.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "💡 [$title]\n\n$content\n\n#태그: ${tags.joinToString(" ")}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "아이디어 공유하기"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "공유")
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            onSave(
                                title,
                                content,
                                selectedCategory,
                                tags.toList(),
                                selectedColorHex,
                                importance,
                                isPinned,
                                isFavorite,
                                idea?.id ?: 0L
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_idea_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("저장")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Category & Importance Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { showCategoryMenu = true }
                            .testTag("category_dropdown_trigger")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "카테고리: $selectedCategory",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        PredefinedCategories.DEFAULT_CREATION_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Importance Stars Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("edit_importance_container")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "중요도:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        com.example.ui.components.ImportanceStarRating(
                            importance = importance,
                            onImportanceChanged = { newRating -> importance = newRating },
                            starSize = 22.dp,
                            isInteractive = true,
                            modifier = Modifier.testTag("edit_importance_stars")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "아이디어 제목을 입력하세요",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("idea_title_input"),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = false,
                maxLines = 3
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Quick Formatting & Voice Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {
                            content = if (content.isBlank()) "• " else "$content\n• "
                        },
                        label = { Text("• 글머리") },
                        leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    )

                    AssistChip(
                        onClick = {
                            val structured = SmartIdeaAssistant.structureIdea(content)
                            content = structured
                        },
                        label = { Text("AI 구조화") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    )
                }

                // Voice Dictation Button in Editor
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isVoiceDictating) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .clickable {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isVoiceDictating) {
                                    voiceRecognitionManager.stopListening()
                                    isVoiceDictating = false
                                } else {
                                    isVoiceDictating = true
                                    voiceRecognitionManager.startListening()
                                }
                            }
                        }
                        .testTag("editor_voice_dictate_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "음성 입력",
                            tint = if (isVoiceDictating) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isVoiceDictating) "듣는 중…" else "음성 입력",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVoiceDictating) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Live Voice Feedback Banner if dictating
            AnimatedVisibility(
                visible = isVoiceDictating && voiceState is VoiceState.Listening,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val partial = (voiceState as? VoiceState.Listening)?.partialText ?: ""
                        Text(
                            text = if (partial.isNotBlank()) partial else "말씀하시는 내용이 텍스트로 추가됩니다…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content TextField
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "아이디어 상세 내용, 생각의 흐름, 실행 방안을 자유롭게 기록하세요…\n(음성 입력 버튼을 눌러 말로도 작성할 수 있습니다)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("idea_content_input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Tags Management Section
            Text(
                text = "🏷️ 태그 관리",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Tags Chips
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text("#$tag", fontWeight = FontWeight.SemiBold) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "태그 삭제",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { tags.remove(tag) }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // New Tag Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("새 태그 입력 (예: 기획, 회의, AI)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_tag_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val clean = newTagInput.trim().removePrefix("#")
                            if (clean.isNotBlank() && !tags.contains(clean)) {
                                tags.add(clean)
                                newTagInput = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val clean = newTagInput.trim().removePrefix("#")
                        if (clean.isNotBlank() && !tags.contains(clean)) {
                            tags.add(clean)
                            newTagInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_tag_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "태그 추가")
                }
            }

            // AI Suggested Tags
            if (suggestedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 스마트 추천 태그 (터치하여 추가):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedTags.forEach { suggested ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                if (!tags.contains(suggested)) {
                                    tags.add(suggested)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "#$suggested",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
