package com.multichoice.medical.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_results")
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationSeconds: Int,
    val category: String,
    val completedAt: Long = System.currentTimeMillis()
)

@androidx.room.Dao
interface ExamResultDao {
    @androidx.room.Insert
    suspend fun insert(result: ExamResult)

    @androidx.room.Query("SELECT * FROM exam_results ORDER BY completedAt DESC LIMIT 100")
    suspend fun latest(): List<ExamResult>

    @androidx.room.Query("SELECT COUNT(*) FROM exam_results")
    suspend fun count(): Int
}
