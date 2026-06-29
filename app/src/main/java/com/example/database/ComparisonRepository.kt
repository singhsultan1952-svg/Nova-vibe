package com.example.database

import kotlinx.coroutines.flow.Flow

class ComparisonRepository(private val comparisonDao: ComparisonDao) {
    val allComparisons: Flow<List<Comparison>> = comparisonDao.getAllComparisons()

    suspend fun getComparisonById(id: Long): Comparison? {
        return comparisonDao.getComparisonById(id)
    }

    suspend fun insertComparison(comparison: Comparison): Long {
        return comparisonDao.insertComparison(comparison)
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        comparisonDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteComparisonById(id: Long) {
        comparisonDao.deleteComparisonById(id)
    }

    suspend fun deleteAllComparisons() {
        comparisonDao.deleteAllComparisons()
    }
}
