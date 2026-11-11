package com.multichoice.medical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multichoice.medical.data.ExamResult
import com.multichoice.medical.data.Question
import com.multichoice.medical.data.QuestionDatabase
import com.multichoice.medical.data.QuestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Navy = Color(0xFF102A43)
private val Teal = Color(0xFF0F9D91)
private val Page = Color(0xFFF4F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = QuestionDatabase.get(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Navy, background = Page)) {
                MedicalApp(QuestionRepository(db.questionDao()), db)
            }
        }
    }
}

@Composable
private fun MedicalApp(repository: QuestionRepository, db: QuestionDatabase) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var history by remember { mutableStateOf<List<ExamResult>>(emptyList()) }
    var message by remember { mutableStateOf("جاري تحميل الأسئلة…") }
    var loading by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("الكل") }
    var examSize by remember { mutableIntStateOf(10) }
    var timerMinutes by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var exam by remember { mutableStateOf<List<Question>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var historyScreen by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }

    suspend fun refreshHistory() { history = db.examResultDao().latest() }

    LaunchedEffect(Unit) {
        runCatching { repository.questions() }
            .onSuccess { questions = it; message = "جاهز للعمل دون إنترنت" }
            .onFailure { message = it.localizedMessage ?: "تعذر تحميل الأسئلة" }
        refreshHistory()
        loading = false
    }

    fun endExam() {
        if (finished) return
        finished = true
        running = false
        if (!saved && exam.isNotEmpty()) {
            saved = true
            val elapsed = ((System.currentTimeMillis() - startedAt) / 1000L).toInt().coerceAtLeast(0)
            scope.launch {
                db.examResultDao().insert(ExamResult(
                    totalQuestions = exam.size,
                    correctAnswers = score,
                    durationSeconds = elapsed,
                    category = category
                ))
                refreshHistory()
            }
        }
    }

    LaunchedEffect(running, finished, timerMinutes) {
        if (running && !finished && timerMinutes > 0) {
            while (secondsLeft > 0 && running && !finished) {
                delay(1000)
                secondsLeft -= 1
            }
            if (secondsLeft <= 0 && running && !finished) endExam()
        }
    }

    fun begin() {
        val pool = if (category == "الكل") questions else questions.filter { it.category == category }
        exam = pool.shuffled().take(examSize.coerceAtMost(pool.size))
        index = 0
        selected = -1
        answered = false
        score = 0
        finished = false
        saved = false
        secondsLeft = timerMinutes * 60
        startedAt = System.currentTimeMillis()
        historyScreen = false
        running = exam.isNotEmpty()
    }

    Column(
        Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("MultiChoice", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("MEDICAL  •  منصة التدريب الطبي", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Surface(color = Navy, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("✚", color = Color.White, fontSize = 27.sp) }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (loading) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            return@Column
        }

        if (historyScreen && !running) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { historyScreen = false }) { Text("رجوع") }
                Spacer(Modifier.weight(1f))
                Text("سجل النتائج", color = Navy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            if (history.isEmpty()) {
                Text("لا توجد اختبارات محفوظة حتى الآن.", modifier = Modifier.fillMaxWidth().padding(25.dp), textAlign = TextAlign.Center)
            }
            history.forEach { item ->
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                        Text(item.category, color = Navy, fontWeight = FontWeight.Bold)
                        Text("${item.correctAnswers} / ${item.totalQuestions}  •  ${if (item.totalQuestions > 0) item.correctAnswers * 100 / item.totalQuestions else 0}%", color = Teal, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("المدة: ${item.durationSeconds / 60} د ${item.durationSeconds % 60} ث", color = Color.Gray, fontSize = 12.sp)
                        Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.completedAt)), color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        } else if (!running && !finished) {
            Surface(color = Navy, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.End) {
                    Text("اختبر معلوماتك الطبية", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("أسئلة اختيار من متعدد مع تصحيح فوري وتفسير وحفظ سجل الأداء.", color = Color(0xFFD8E5EE), textAlign = TextAlign.End)
                    Spacer(Modifier.height(12.dp))
                    Text("${questions.size} سؤال محفوظ على الجهاز", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("إعداد الاختبار", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("التصنيف", color = Navy, fontWeight = FontWeight.SemiBold)
            val categories = listOf("الكل") + questions.map { it.category }.distinct()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                categories.forEach { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Text("عدد الأسئلة: ${examSize.coerceAtMost(questions.size)}", color = Navy, fontWeight = FontWeight.SemiBold)
            Slider(
                value = examSize.toFloat().coerceIn(1f, questions.size.coerceAtLeast(1).toFloat()),
                onValueChange = { examSize = it.toInt().coerceAtLeast(1) },
                valueRange = 1f..questions.size.coerceAtLeast(1).toFloat()
            )
            Text("مدة الاختبار", color = Navy, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                listOf(0, 15, 30, 60).forEach { mins ->
                    FilterChip(selected = timerMinutes == mins, onClick = { timerMinutes = mins }, label = { Text(if (mins == 0) "بدون مؤقت" else "$mins دقيقة") }, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { begin() }, enabled = questions.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("ابدأ الاختبار") }
            OutlinedButton(onClick = { historyScreen = true }, modifier = Modifier.fillMaxWidth()) { Text("سجل النتائج (${history.size})") }
            OutlinedButton(onClick = {
                message = "جارٍ الاتصال بالسحابة…"
                scope.launch {
                    repository.syncFromCloud()
                        .onSuccess { count -> questions = repository.questions(); message = "تمت مزامنة $count سؤالاً" }
                        .onFailure { message = it.localizedMessage ?: "فشلت المزامنة" }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("مزامنة الأسئلة من السحابة") }
            Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("المحتوى الحالي تجريبي ومحدود؛ يجب تدقيق الأسئلة والإجابات من مراجع طبية حديثة قبل استخدامه في التدريب الرسمي.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.End)
        } else if (finished) {
            Surface(color = Color.White, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("انتهى الاختبار", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("$score / ${exam.size}", color = Teal, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("نسبة النجاح: ${if (exam.isNotEmpty()) score * 100 / exam.size else 0}%", color = Navy, fontSize = 19.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { finished = false; historyScreen = false }, modifier = Modifier.fillMaxWidth()) { Text("الرئيسية") }
                    OutlinedButton(onClick = { historyScreen = true; finished = false }, modifier = Modifier.fillMaxWidth()) { Text("سجل النتائج") }
                }
            }
        } else {
            val q = exam[index]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1} / ${exam.size}", color = Teal, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(q.category, color = Navy, fontWeight = FontWeight.Bold)
            }
            if (timerMinutes > 0) {
                Text("الوقت المتبقي: ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}", color = if (secondsLeft < 60) Color.Red else Navy, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { (index + 1).toFloat() / exam.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth(), color = Teal)
            Spacer(Modifier.height(16.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.End) {
                    Text("السؤال", color = Teal, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(q.question, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                }
            }
            q.options().forEachIndexed { optionIndex, option ->
                val bg = when {
                    answered && optionIndex == q.correctIndex -> Color(0xFFE1F5EC)
                    answered && optionIndex == selected -> Color(0xFFFFE8E6)
                    selected == optionIndex -> Color(0xFFDDF4F1)
                    else -> Color.White
                }
                Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !answered) { selected = optionIndex }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f), color = Navy, textAlign = TextAlign.End)
                        Spacer(Modifier.width(10.dp))
                        Text(listOf("أ", "ب", "ج", "د")[optionIndex], color = Teal, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (answered) {
                Surface(color = Color(0xFFEAF2F8), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                        Text(if (selected == q.correctIndex) "إجابة صحيحة ✓" else "الإجابة الصحيحة: ${q.options()[q.correctIndex]}", color = Teal, fontWeight = FontWeight.Bold)
                        Text(q.explanation, color = Navy, textAlign = TextAlign.End)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (!answered) {
                    if (selected >= 0) { answered = true; if (selected == q.correctIndex) score++ }
                } else if (index < exam.lastIndex) {
                    index++; selected = -1; answered = false
                } else endExam()
            }, enabled = answered || selected >= 0, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (!answered) "تأكيد الإجابة" else if (index == exam.lastIndex) "إنهاء وعرض النتيجة" else "السؤال التالي")
            }
            TextButton(onClick = { endExam() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("إنهاء الاختبار") }
        }
    }
}
