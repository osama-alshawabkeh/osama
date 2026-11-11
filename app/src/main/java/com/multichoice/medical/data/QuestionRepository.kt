package com.multichoice.medical.data

import com.multichoice.medical.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class QuestionRepository(private val dao: QuestionDao) {
    suspend fun questions(): List<Question> {
        if (dao.count() == 0) dao.insertAll(sampleQuestions)
        return dao.getAll()
    }

    suspend fun syncFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val base = BuildConfig.SUPABASE_URL.trimEnd('/')
            val key = BuildConfig.SUPABASE_ANON_KEY
            require(base.isNotBlank() && key.isNotBlank()) {
                "الاتصال السحابي غير مضبوط. أضف SUPABASE_URL وSUPABASE_ANON_KEY إلى إعدادات البناء."
            }
            val connection = URL("$base/rest/v1/questions?select=*&order=id.asc").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("apikey", key)
                connection.setRequestProperty("Authorization", "Bearer $key")
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                check(code in 200..299) { "فشل Supabase (HTTP $code): $body" }
                val json = JSONArray(body)
                val rows = buildList {
                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)
                        val question = item.getString("question").trim()
                        val options = listOf(
                            item.getString("option_a").trim(),
                            item.getString("option_b").trim(),
                            item.getString("option_c").trim(),
                            item.getString("option_d").trim()
                        )
                        val answer = item.getInt("correct_index")
                        require(question.isNotEmpty() && options.all { it.isNotEmpty() }) {
                            "يوجد سؤال سحابي أو خيار فارغ (السجل رقم ${item.optLong("id")})"
                        }
                        require(answer in 0..3) { "قيمة correct_index يجب أن تكون من 0 إلى 3" }
                        add(
                            Question(
                                id = item.getLong("id"),
                                category = item.optString("category", "عام"),
                                question = question,
                                optionA = options[0],
                                optionB = options[1],
                                optionC = options[2],
                                optionD = options[3],
                                correctIndex = answer,
                                explanation = item.optString("explanation", "")
                            )
                        )
                    }
                }
                if (rows.isNotEmpty()) dao.insertAll(rows)
                rows.size
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        val sampleQuestions = listOf(
            Question(1, "طب الأطفال", "ما العلاج الأول لنوبة الربو الحادة الخفيفة إلى المتوسطة لدى الطفل؟", "سالبوتامول مستنشق", "مضاد حيوي فموي", "مدر بول", "مضاد هيستامين فقط", 0, "موسع قصبي قصير المفعول مستنشق مثل السالبوتامول هو العلاج الأول لتخفيف التشنج القصبي الحاد."),
            Question(2, "طب الأطفال", "أي علامة تُعد من علامات الجفاف الشديد عند الرضع؟", "دموع طبيعية", "خمول أو انخفاض مستوى الوعي", "تبول متكرر", "أغشية مخاطية رطبة", 1, "الخمول أو تغير الوعي علامة إنذار تستدعي تقييماً عاجلاً مع تقييم التروية والعلامات الأخرى."),
            Question(3, "طب الأطفال", "ما الاختبار المخبري الذي يعكس مخزون الحديد؟", "مستوى الصوديوم", "الفيريتين", "إنزيمات الكبد", "الكالسيوم", 1, "الفيريتين يعكس مخزون الحديد، لكنه قد يرتفع مع الالتهاب لذا يجب تفسيره ضمن السياق السريري."),
            Question(4, "طب الأطفال", "ما معدل ضغطات الصدر الموصى به في إنعاش الرضع والأطفال؟", "40–60 ضغطة/دقيقة", "60–80 ضغطة/دقيقة", "100–120 ضغطة/دقيقة", "140–160 ضغطة/دقيقة", 2, "معدل الضغطات في إنعاش الرضع والأطفال هو 100–120 ضغطة في الدقيقة وفق إرشادات الإنعاش المعتمدة.")
        )
    }
}
