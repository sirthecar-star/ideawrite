package com.example.ai

import com.example.data.model.PredefinedCategories
import java.util.Locale

object SmartIdeaAssistant {

    private val categoryKeywordMap = mapOf(
        "💡 창작/기획" to listOf("아이디어", "기획", "창작", "디자인", "스토리", "콘텐츠", "유튜브", "글쓰기", "영감", "브레인스토밍", "컨셉", "아이템"),
        "💼 업무/비즈니스" to listOf("업무", "회의", "미팅", "비즈니스", "고객", "매출", "영업", "보고서", "제안서", "팀", "계약", "협업", "발표", "마케팅"),
        "🚀 프로젝트" to listOf("프로젝트", "출시", "런칭", "스프린트", "마일스톤", "데드라인", "일정", "로드맵", "피드백", "테스트"),
        "🛠️ 개발/IT" to listOf("개발", "코딩", "서버", "안드로이드", "kotlin", "앱", "api", "버그", "깃허브", "db", "배포", "데이터", "프론트", "백엔드"),
        "💭 생각/일상" to listOf("생각", "일상", "일기", "오늘", "기분", "느낌", "메모", "카페", "쇼핑", "산책", "감사", "여행", "음식"),
        "📚 학습/연구" to listOf("공부", "학습", "독서", "책", "강의", "논문", "연구", "요약", "자격증", "영어", "외국어"),
        "🎯 목표/습관" to listOf("목표", "습관", "루틴", "운동", "다이어트", "계획", "도전", "건강", "기상", "명상", "투두", "실천")
    )

    private val commonTagKeywords = listOf(
        "아이디어", "기획", "개발", "디자인", "회의", "비즈니스", "프로젝트",
        "루틴", "목표", "독서", "학습", "운동", "일상", "영감", "AI",
        "Android", "마케팅", "창작", "급한일", "할일", "체크리스트", "여행", "쇼핑"
    )

    /**
     * Extracts existing hashtags (e.g. #기획) from text, as well as smart inferred tags.
     */
    fun extractSuggestedTags(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableSetOf<String>()

        // 1. Explicit hashtags in text
        val hashRegex = Regex("""#([가-힣a-zA-Z0-9_]+)""")
        hashRegex.findAll(text).forEach { match ->
            val tag = match.groupValues[1].trim()
            if (tag.isNotEmpty()) result.add(tag)
        }

        // 2. Keyword matching
        val lowerText = text.lowercase(Locale.getDefault())
        for (kw in commonTagKeywords) {
            if (lowerText.contains(kw.lowercase(Locale.getDefault()))) {
                result.add(kw)
            }
        }

        return result.take(8).toList()
    }

    /**
     * Recommends the best category for the given content.
     */
    fun recommendCategory(text: String): String {
        if (text.isBlank()) return "💡 창작/기획"
        val lower = text.lowercase(Locale.getDefault())

        var bestCategory = "💡 창작/기획"
        var maxMatches = 0

        for ((category, keywords) in categoryKeywordMap) {
            val matches = keywords.count { kw -> lower.contains(kw.lowercase(Locale.getDefault())) }
            if (matches > maxMatches) {
                maxMatches = matches
                bestCategory = category
            }
        }

        return if (maxMatches > 0) bestCategory else "💡 창작/기획"
    }

    /**
     * Parses raw speech input and splits into a concise title and formatted body.
     */
    fun parseSpeechToIdea(spokenText: String): Pair<String, String> {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) return Pair("", "")

        val lines = trimmed.split("\n", ". ").filter { it.isNotBlank() }
        if (lines.isEmpty()) return Pair(trimmed, "")

        val firstLine = lines[0].trim().removeSuffix(".")
        val title = if (firstLine.length <= 30) {
            firstLine
        } else {
            firstLine.take(30) + "…"
        }

        val remainingContent = if (lines.size > 1) {
            lines.drop(1).joinToString("\n- ") { it.trim().removeSuffix(".") }
                .let { if (it.isNotBlank()) "- $it" else "" }
        } else {
            trimmed
        }

        return Pair(title, remainingContent)
    }

    /**
     * Formats raw idea thoughts into neat structured bullets.
     */
    fun structureIdea(content: String): String {
        if (content.isBlank()) return content
        val items = content.split(Regex("[\n,.]")).map { it.trim() }.filter { it.length > 2 }
        if (items.isEmpty()) return content
        return items.joinToString("\n") { "• $it" }
    }
}
