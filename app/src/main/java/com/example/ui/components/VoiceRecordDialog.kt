package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.speech.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordDialog(
    voiceState: VoiceState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onQuickSave: (String) -> Unit,
    onOpenInEditor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            onStartListening()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("voice_record_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎙️ 음성으로 아이디어 기록",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_voice_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasAudioPermission) {
                // Permission Denied View
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "음성 인식을 위해 마이크 권한이 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("마이크 권한 허용하기")
                        }
                    }
                }
            } else {
                // Audio Wave Visualizer & Microphone
                val isListening = voiceState is VoiceState.Listening || voiceState is VoiceState.Ready

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isListening) 1.35f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = if (isListening) 0.15f else 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp)
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        if (isListening) Color(0xFF6366F1) else MaterialTheme.colorScheme.surfaceVariant,
                                        if (isListening) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "마이크",
                            tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Status Label
                val currentText = when (voiceState) {
                    is VoiceState.Idle -> "마이크 버튼을 눌러 말을 시작하세요"
                    is VoiceState.Ready -> "듣고 있습니다… 편하게 말씀해보세요"
                    is VoiceState.Listening -> if (voiceState.partialText.isNotBlank()) voiceState.partialText else "듣고 있습니다… (예: 새 프로젝트 아이디어 기획안)"
                    is VoiceState.Success -> voiceState.recognizedText
                    is VoiceState.Error -> "⚠️ ${voiceState.message}"
                }

                Text(
                    text = when (voiceState) {
                        is VoiceState.Listening, is VoiceState.Ready -> "실시간 음성 인식 중…"
                        is VoiceState.Success -> "인식 완료!"
                        is VoiceState.Error -> "인식 실패"
                        is VoiceState.Idle -> "대기 중"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recognized Transcript Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            color = when (voiceState) {
                                is VoiceState.Error -> MaterialTheme.colorScheme.error
                                is VoiceState.Success -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Row
                when (voiceState) {
                    is VoiceState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onStartListening,
                                modifier = Modifier.weight(1f).testTag("retry_voice_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("다시 말하기")
                            }

                            FilledTonalButton(
                                onClick = { onOpenInEditor(voiceState.recognizedText) },
                                modifier = Modifier.weight(1f).testTag("edit_voice_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("편집하기")
                            }

                            Button(
                                onClick = { onQuickSave(voiceState.recognizedText) },
                                modifier = Modifier.weight(1.2f).testTag("quick_save_voice_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("즉시 저장")
                            }
                        }
                    }
                    is VoiceState.Listening -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onStopListening,
                                modifier = Modifier.weight(1f).testTag("stop_listening_button")
                            ) {
                                Text("말하기 완료")
                            }

                            if (voiceState.partialText.isNotBlank()) {
                                Button(
                                    onClick = { onQuickSave(voiceState.partialText) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("지금까지 저장")
                                }
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onStartListening,
                                modifier = Modifier.fillMaxWidth().testTag("start_listening_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("음성 인식 시작하기")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
