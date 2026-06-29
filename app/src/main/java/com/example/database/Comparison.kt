package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.api.GeminiCategoryScore
import com.example.api.GeminiProsCons
import com.example.api.RetrofitClient
import com.squareup.moshi.Types

@Entity(tableName = "comparisons")
@TypeConverters(Converters::class)
data class Comparison(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemA: String,
    val itemB: String,
    val context: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scoreA: Int,
    val scoreB: Int,
    val winner: String, // "A", "B", or "TIE"
    val verdict: String,
    val criteriaScores: List<GeminiCategoryScore>,
    val prosConsA: GeminiProsCons,
    val prosConsB: GeminiProsCons,
    val isFavorite: Boolean = false
)

class Converters {
    private val moshi = RetrofitClient.moshi

    @TypeConverter
    fun fromCategoryScoreList(value: List<GeminiCategoryScore>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, GeminiCategoryScore::class.java)
        val adapter = moshi.adapter<List<GeminiCategoryScore>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toCategoryScoreList(value: String?): List<GeminiCategoryScore>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, GeminiCategoryScore::class.java)
        val adapter = moshi.adapter<List<GeminiCategoryScore>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun fromProsCons(value: GeminiProsCons?): String? {
        if (value == null) return null
        val adapter = moshi.adapter(GeminiProsCons::class.java)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toProsCons(value: String?): GeminiProsCons? {
        if (value == null) return null
        val adapter = moshi.adapter(GeminiProsCons::class.java)
        return adapter.fromJson(value)
    }
}
