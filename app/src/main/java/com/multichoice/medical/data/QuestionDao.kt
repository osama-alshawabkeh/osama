package com.multichoice.medical.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id ASC")
    suspend fun getAll(): List<Question>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)
}
