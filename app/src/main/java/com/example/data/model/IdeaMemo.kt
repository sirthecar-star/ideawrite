package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ideas")
data class IdeaMemo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "일반",
    val tags: List<String> = emptyList(),
    val colorHex: String = "#FFFFFF",
    val importance: Int = 3, // 1 ~ 5 stars (default 3)
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isVoiceRecorded: Boolean = false
) {
    val displayTitle: String
        get() = if (title.isNotBlank()) title else {
            val firstLine = content.lines().firstOrNull()?.trim() ?: ""
            if (firstLine.isNotBlank()) {
                if (firstLine.length > 25) firstLine.take(25) + "…" else firstLine
            } else "제목 없는 아이디어"
        }
}

object PredefinedCategories {
    val ALL = listOf(
        "전체",
        "💡 창작/기획",
        "💼 업무/비즈니스",
        "🚀 프로젝트",
        "🛠️ 개발/IT",
        "💭 생각/일상",
        "📚 학습/연구",
        "🎯 목표/습관",
        "기타"
    )

    val DEFAULT_CREATION_CATEGORIES = listOf(
        "💡 창작/기획",
        "💼 업무/비즈니스",
        "🚀 프로젝트",
        "🛠️ 개발/IT",
        "💭 생각/일상",
        "📚 학습/연구",
        "🎯 목표/습관",
        "기타"
    )
}

data class NoteColorOption(
    val name: String,
    val lightHex: String,
    val darkHex: String
)

val ColorOptions = listOf(
    NoteColorOption("기본 (화이트)", "#FFFFFF", "#1E293B"),
    NoteColorOption("앰버 (아이디어)", "#FEF3C7", "#3B2E15"),
    NoteColorOption("인디고 (기획)", "#EEF2FF", "#22254C"),
    NoteColorOption("에메랄드 (실행)", "#D1FAE5", "#143729"),
    NoteColorOption("스카이 (기술)", "#E0F2FE", "#173142"),
    NoteColorOption("로즈 (영감)", "#FFE4E6", "#3D1A22"),
    NoteColorOption("퍼플 (창의)", "#F3E8FF", "#2F1E44")
)
