package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.speech.VoiceState
import com.example.ui.components.TagManageDialog
import com.example.ui.components.VoiceRecordDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IdeaEditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IdeaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: IdeaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: IdeaViewModel) {
    val isEditSheetOpen by viewModel.isEditSheetOpen.collectAsState()
    val editingIdea by viewModel.editingIdea.collectAsState()
    val isVoiceDialogOpen by viewModel.isVoiceDialogOpen.collectAsState()
    val isTagManageOpen by viewModel.isTagManageOpen.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isEditSheetOpen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { editOpen ->
                if (editOpen) {
                    IdeaEditScreen(
                        idea = editingIdea,
                        voiceRecognitionManager = viewModel.voiceRecognitionManager,
                        onSave = { title, content, category, tags, colorHex, importance, isPinned, isFavorite, id ->
                            viewModel.saveIdea(
                                title = title,
                                content = content,
                                category = category,
                                tags = tags,
                                colorHex = colorHex,
                                importance = importance,
                                isPinned = isPinned,
                                isFavorite = isFavorite,
                                id = id
                            )
                        },
                        onBack = { viewModel.closeEditIdeaSheet() }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Voice Record Modal Dialog
            if (isVoiceDialogOpen) {
                VoiceRecordDialog(
                    voiceState = voiceState,
                    onStartListening = {
                        viewModel.voiceRecognitionManager.reset()
                        viewModel.voiceRecognitionManager.startListening()
                    },
                    onStopListening = {
                        viewModel.voiceRecognitionManager.stopListening()
                    },
                    onQuickSave = { text ->
                        viewModel.quickSaveVoiceResult(text)
                    },
                    onOpenInEditor = { text ->
                        viewModel.openVoiceResultInEditor(text)
                    },
                    onDismiss = { viewModel.closeVoiceDialog() }
                )
            }

            // Tag Manage Dialog
            if (isTagManageOpen) {
                TagManageDialog(
                    allTags = allTags,
                    selectedTags = selectedTags,
                    onToggleTag = { tag -> viewModel.toggleTagFilter(tag) },
                    onClearAllTags = { viewModel.clearTagFilters() },
                    onDismiss = { viewModel.closeTagManageDialog() }
                )
            }
        }
    }
}
