package com.example.data.repository

import com.example.data.local.IdeaDao
import com.example.data.model.IdeaMemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class IdeaRepository(private val ideaDao: IdeaDao) {

    val allIdeas: Flow<List<IdeaMemo>> = ideaDao.getAllIdeas()

    val allTags: Flow<List<TagStat>> = ideaDao.getAllIdeas().map { ideas ->
        val tagCountMap = mutableMapOf<String, Int>()
        ideas.forEach { memo ->
            memo.tags.forEach { tag ->
                val cleanTag = tag.trim().removePrefix("#")
                if (cleanTag.isNotBlank()) {
                    tagCountMap[cleanTag] = (tagCountMap[cleanTag] ?: 0) + 1
                }
            }
        }
        tagCountMap.entries
            .map { TagStat(name = it.key, count = it.value) }
            .sortedByDescending { it.count }
    }

    val allCategories: Flow<List<CategoryStat>> = ideaDao.getAllIdeas().map { ideas ->
        val catMap = mutableMapOf<String, Int>()
        ideas.forEach { memo ->
            catMap[memo.category] = (catMap[memo.category] ?: 0) + 1
        }
        catMap.entries
            .map { CategoryStat(name = it.key, count = it.value) }
            .sortedByDescending { it.count }
    }

    fun getIdeaByIdFlow(id: Long): Flow<IdeaMemo?> = ideaDao.getIdeaByIdFlow(id)

    suspend fun getIdeaById(id: Long): IdeaMemo? = withContext(Dispatchers.IO) {
        ideaDao.getIdeaById(id)
    }

    suspend fun insertIdea(idea: IdeaMemo): Long = withContext(Dispatchers.IO) {
        ideaDao.insertIdea(idea)
    }

    suspend fun updateIdea(idea: IdeaMemo) = withContext(Dispatchers.IO) {
        ideaDao.updateIdea(idea.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteIdea(idea: IdeaMemo) = withContext(Dispatchers.IO) {
        ideaDao.deleteIdea(idea)
    }

    suspend fun deleteIdeaById(id: Long) = withContext(Dispatchers.IO) {
        ideaDao.deleteIdeaById(id)
    }

    suspend fun togglePin(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        ideaDao.updatePinStatus(id, isPinned)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        ideaDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun checkAndPrepopulateIfEmpty(initialIdeas: List<IdeaMemo>) = withContext(Dispatchers.IO) {
        if (ideaDao.getCount() == 0) {
            ideaDao.insertAll(initialIdeas)
        }
    }
}

data class TagStat(
    val name: String,
    val count: Int
)

data class CategoryStat(
    val name: String,
    val count: Int
)
