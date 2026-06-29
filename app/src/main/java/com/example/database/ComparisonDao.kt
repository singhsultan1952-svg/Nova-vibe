package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparisonDao {
    @Query("SELECT * FROM comparisons ORDER BY timestamp DESC")
    fun getAllComparisons(): Flow<List<Comparison>>

    @Query("SELECT * FROM comparisons WHERE id = :id LIMIT 1")
    suspend fun getComparisonById(id: Long): Comparison?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComparison(comparison: Comparison): Long

    @Query("UPDATE comparisons SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM comparisons WHERE id = :id")
    suspend fun deleteComparisonById(id: Long)

    @Query("DELETE FROM comparisons")
    suspend fun deleteAllComparisons()
}
