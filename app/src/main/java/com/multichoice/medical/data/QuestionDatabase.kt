package com.multichoice.medical.data
import android.content.Context
import androidx.room.*
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id") suspend fun getAll(): List<Question>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Question>)
    @Query("SELECT COUNT(*) FROM questions") suspend fun count(): Int
}
@Database(entities = [Question::class], version = 1, exportSchema = false)
abstract class QuestionDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    companion object {
        @Volatile private var INSTANCE: QuestionDatabase? = null
        fun get(context: Context): QuestionDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, QuestionDatabase::class.java, "medical_questions.db").build().also { INSTANCE = it }
        }
    }
}
