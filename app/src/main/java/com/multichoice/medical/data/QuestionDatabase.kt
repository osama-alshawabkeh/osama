package com.multichoice.medical.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Question::class, ExamResult::class], version = 2, exportSchema = false)
abstract class QuestionDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun examResultDao(): ExamResultDao

    companion object {
        @Volatile private var INSTANCE: QuestionDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS exam_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, totalQuestions INTEGER NOT NULL, correctAnswers INTEGER NOT NULL, durationSeconds INTEGER NOT NULL, category TEXT NOT NULL, completedAt INTEGER NOT NULL)")
            }
        }
        fun get(context: Context): QuestionDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, QuestionDatabase::class.java, "medical_questions.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
