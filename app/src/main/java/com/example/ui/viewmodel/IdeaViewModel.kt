package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.SmartIdeaAssistant
import com.example.data.local.AppDatabase
import com.example.data.model.IdeaMemo
import com.example.data.repository.CategoryStat
import com.example.data.repository.IdeaRepository
import com.example.data.repository.TagStat
import com.example.speech.VoiceRecognitionManager
import com.example.speech.VoiceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class IdeaTab(val label: String) {
    ALL("전체"),
    FAVORITES("즐겨찾기 ⭐"),
    PINNED("고정됨 📌")
}

enum class SortOption(val label: String) {
    NEWEST("최신순"),
    IMPORTANCE_DESC("중요도 높은순 ⭐"),
    IMPORTANCE_ASC("중요도 낮은순"),
    UPDATED("최근 수정순"),
    TITLE("제목 가나다순")
}

class IdeaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IdeaRepository = IdeaRepository(
        AppDatabase.getInstance(application).ideaDao()
    )

    val voiceRecognitionManager: VoiceRecognitionManager = VoiceRecognitionManager(application)
    val voiceState: StateFlow<VoiceState> = voiceRecognitionManager.voiceState

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("전체")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    // Importance Filter (null = all, 1..5 = specific star rating)
    private val _selectedImportance = MutableStateFlow<Int?>(null)
    val selectedImportance: StateFlow<Int?> = _selectedImportance.asStateFlow()

    private val _activeTab = MutableStateFlow(IdeaTab.ALL)
    val activeTab: StateFlow<IdeaTab> = _activeTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // UI Dialog States
    private val _isVoiceDialogOpen = MutableStateFlow(false)
    val isVoiceDialogOpen: StateFlow<Boolean> = _isVoiceDialogOpen.asStateFlow()

    private val _isTagManageOpen = MutableStateFlow(false)
    val isTagManageOpen: StateFlow<Boolean> = _isTagManageOpen.asStateFlow()

    private val _editingIdea = MutableStateFlow<IdeaMemo?>(null)
    val editingIdea: StateFlow<IdeaMemo?> = _editingIdea.asStateFlow()

    private val _isEditSheetOpen = MutableStateFlow(false)
    val isEditSheetOpen: StateFlow<Boolean> = _isEditSheetOpen.asStateFlow()

    // Messages
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Stats
    val allTags: StateFlow<List<TagStat>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryStat>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterParams(
        val query: String,
        val category: String,
        val tags: Set<String>,
        val importance: Int?,
        val tab: IdeaTab,
        val sort: SortOption
    )

    private val filterParams = combine(
        combine(_searchQuery, _selectedCategory, _selectedTags) { q, cat, tags -> Triple(q, cat, tags) },
        combine(_selectedImportance, _activeTab, _sortOption) { imp, tab, sort -> Triple(imp, tab, sort) }
    ) { (query, category, tags), (importance, tab, sort) ->
        FilterParams(query, category, tags, importance, tab, sort)
    }

    // Combined filtered ideas list
    val filteredIdeas: StateFlow<List<IdeaMemo>> = combine(
        repository.allIdeas,
        filterParams
    ) { rawIdeas, params ->
        var list = rawIdeas

        // Filter by Tab
        list = when (params.tab) {
            IdeaTab.ALL -> list
            IdeaTab.FAVORITES -> list.filter { it.isFavorite }
            IdeaTab.PINNED -> list.filter { it.isPinned }
        }

        // Filter by Category
        if (params.category != "전체" && params.category.isNotBlank()) {
            list = list.filter { it.category == params.category }
        }

        // Filter by Importance Star Rating
        if (params.importance != null) {
            list = list.filter { it.importance == params.importance }
        }

        // Filter by Tags (Must match ALL selected tags or any)
        if (params.tags.isNotEmpty()) {
            list = list.filter { memo ->
                val memoTagsClean = memo.tags.map { it.trim().removePrefix("#") }
                params.tags.any { filterTag -> memoTagsClean.contains(filterTag.trim().removePrefix("#")) }
            }
        }

        // Filter by Search Query (Title, Content, or Tags)
        if (params.query.isNotBlank()) {
            val q = params.query.trim().lowercase()
            list = list.filter { memo ->
                memo.title.lowercase().contains(q) ||
                        memo.content.lowercase().contains(q) ||
                        memo.category.lowercase().contains(q) ||
                        memo.tags.any { it.lowercase().contains(q) }
            }
        }

        // Sort (Pinned always on top in default view)
        when (params.sort) {
            SortOption.NEWEST -> list.sortedWith(
                compareByDescending<IdeaMemo> { it.isPinned }
                    .thenByDescending { it.createdAt }
            )
            SortOption.IMPORTANCE_DESC -> list.sortedWith(
                compareByDescending<IdeaMemo> { it.isPinned }
                    .thenByDescending { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            SortOption.IMPORTANCE_ASC -> list.sortedWith(
                compareByDescending<IdeaMemo> { it.isPinned }
                    .thenBy { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            SortOption.UPDATED -> list.sortedWith(
                compareByDescending<IdeaMemo> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            SortOption.TITLE -> list.sortedWith(
                compareByDescending<IdeaMemo> { it.isPinned }
                    .thenBy { it.title.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pinned ideas count
    val pinnedCount: StateFlow<Int> = repository.allIdeas.combine(_selectedCategory) { ideas, cat ->
        ideas.count { it.isPinned && (cat == "전체" || it.category == cat) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setImportanceFilter(importance: Int?) {
        _selectedImportance.value = if (_selectedImportance.value == importance) null else importance
    }

    fun clearImportanceFilter() {
        _selectedImportance.value = null
    }

    fun toggleTagFilter(tag: String) {
        val clean = tag.trim().removePrefix("#")
        val current = _selectedTags.value.toMutableSet()
        if (current.contains(clean)) {
            current.remove(clean)
        } else {
            current.add(clean)
        }
        _selectedTags.value = current
    }

    fun clearTagFilters() {
        _selectedTags.value = emptySet()
    }

    fun setActiveTab(tab: IdeaTab) {
        _activeTab.value = tab
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun openNewIdeaSheet(initialTitle: String = "", initialContent: String = "", isVoice: Boolean = false) {
        val suggestedCat = if (initialContent.isNotBlank()) SmartIdeaAssistant.recommendCategory(initialContent) else "💡 창작/기획"
        val suggestedTags = if (initialContent.isNotBlank()) SmartIdeaAssistant.extractSuggestedTags(initialContent) else emptyList()

        _editingIdea.value = IdeaMemo(
            id = 0,
            title = initialTitle,
            content = initialContent,
            category = suggestedCat,
            tags = suggestedTags,
            isVoiceRecorded = isVoice
        )
        _isEditSheetOpen.value = true
    }

    fun openEditIdeaSheet(idea: IdeaMemo) {
        _editingIdea.value = idea
        _isEditSheetOpen.value = true
    }

    fun closeEditIdeaSheet() {
        _isEditSheetOpen.value = false
        _editingIdea.value = null
    }

    fun openVoiceDialog() {
        _isVoiceDialogOpen.value = true
        voiceRecognitionManager.reset()
        voiceRecognitionManager.startListening()
    }

    fun closeVoiceDialog() {
        voiceRecognitionManager.stopListening()
        voiceRecognitionManager.reset()
        _isVoiceDialogOpen.value = false
    }

    fun openTagManageDialog() {
        _isTagManageOpen.value = true
    }

    fun closeTagManageDialog() {
        _isTagManageOpen.value = false
    }

    fun saveIdea(
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        colorHex: String,
        importance: Int = 3,
        isPinned: Boolean,
        isFavorite: Boolean,
        id: Long = 0,
        isVoiceRecorded: Boolean = false
    ) {
        if (title.isBlank() && content.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("내용이나 제목을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            val finalTitle = title.trim()
            val finalContent = content.trim()
            val finalTags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct()

            if (id == 0L) {
                // New Idea
                val newIdea = IdeaMemo(
                    title = finalTitle,
                    content = finalContent,
                    category = if (category.isBlank()) "💡 창작/기획" else category,
                    tags = finalTags,
                    colorHex = colorHex,
                    importance = importance.coerceIn(1, 5),
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isVoiceRecorded = isVoiceRecorded
                )
                repository.insertIdea(newIdea)
                _toastMessage.emit("새 아이디어가 저장되었습니다! ✨")
            } else {
                // Update Idea
                val existing = _editingIdea.value
                val updatedIdea = IdeaMemo(
                    id = id,
                    title = finalTitle,
                    content = finalContent,
                    category = if (category.isBlank()) (existing?.category ?: "💡 창작/기획") else category,
                    tags = finalTags,
                    colorHex = colorHex,
                    importance = importance.coerceIn(1, 5),
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isVoiceRecorded = existing?.isVoiceRecorded ?: isVoiceRecorded
                )
                repository.updateIdea(updatedIdea)
                _toastMessage.emit("아이디어가 수정되었습니다. ✏️")
            }
            closeEditIdeaSheet()
        }
    }

    fun updateImportance(idea: IdeaMemo, newImportance: Int) {
        viewModelScope.launch {
            val clamped = newImportance.coerceIn(1, 5)
            val updated = idea.copy(importance = clamped, updatedAt = System.currentTimeMillis())
            repository.updateIdea(updated)
            _toastMessage.emit("중요도가 별 ${clamped}개로 변경되었습니다. ⭐")
        }
    }

    fun quickSaveVoiceResult(spokenText: String) {
        if (spokenText.isBlank()) return
        val (parsedTitle, parsedContent) = SmartIdeaAssistant.parseSpeechToIdea(spokenText)
        val suggestedCategory = SmartIdeaAssistant.recommendCategory(spokenText)
        val suggestedTags = SmartIdeaAssistant.extractSuggestedTags(spokenText)

        viewModelScope.launch {
            val idea = IdeaMemo(
                title = parsedTitle,
                content = parsedContent,
                category = suggestedCategory,
                tags = suggestedTags,
                colorHex = "#FEF3C7", // amber
                isPinned = false,
                isFavorite = false,
                isVoiceRecorded = true
            )
            repository.insertIdea(idea)
            closeVoiceDialog()
            _toastMessage.emit("🎙️ 음성 아이디어가 즉시 저장되었습니다!")
        }
    }

    fun openVoiceResultInEditor(spokenText: String) {
        closeVoiceDialog()
        val (parsedTitle, parsedContent) = SmartIdeaAssistant.parseSpeechToIdea(spokenText)
        openNewIdeaSheet(initialTitle = parsedTitle, initialContent = parsedContent, isVoice = true)
    }

    fun togglePin(idea: IdeaMemo) {
        viewModelScope.launch {
            val newPin = !idea.isPinned
            repository.togglePin(idea.id, newPin)
            _toastMessage.emit(if (newPin) "상단에 고정되었습니다. 📌" else "고정이 해제되었습니다.")
        }
    }

    fun toggleFavorite(idea: IdeaMemo) {
        viewModelScope.launch {
            val newFav = !idea.isFavorite
            repository.toggleFavorite(idea.id, newFav)
            _toastMessage.emit(if (newFav) "즐겨찾기에 추가되었습니다. ⭐" else "즐겨찾기에서 제거되었습니다.")
        }
    }

    fun deleteIdea(idea: IdeaMemo) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
            _toastMessage.emit("아이디어가 삭제되었습니다. 🗑️")
        }
    }

    fun duplicateIdea(idea: IdeaMemo) {
        viewModelScope.launch {
            val copyIdea = idea.copy(
                id = 0,
                title = "${idea.title} (복사본)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertIdea(copyIdea)
            _toastMessage.emit("아이디어가 복제되었습니다.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager.destroy()
    }
}
