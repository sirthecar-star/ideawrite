package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IdeaMemo
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllIdeas(): Flow<List<IdeaMemo>>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getIdeaById(id: Long): IdeaMemo?

    @Query("SELECT * FROM ideas WHERE id = :id")
    fun getIdeaByIdFlow(id: Long): Flow<IdeaMemo?>

    @Query("SELECT * FROM ideas WHERE isFavorite = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoriteIdeas(): Flow<List<IdeaMemo>>

    @Query("SELECT * FROM ideas WHERE category = :category ORDER BY isPinned DESC, updatedAt DESC")
    fun getIdeasByCategory(category: String): Flow<List<IdeaMemo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaMemo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ideas: List<IdeaMemo>)

    @Update
    suspend fun updateIdea(idea: IdeaMemo)

    @Delete
    suspend fun deleteIdea(idea: IdeaMemo)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdeaById(id: Long)

    @Query("UPDATE ideas SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinStatus(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ideas SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun getCount(): Int
}
