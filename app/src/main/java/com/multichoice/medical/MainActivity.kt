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
import com.multichoice.medical.data.Question
import com.multichoice.medical.data.QuestionDatabase
import com.multichoice.medical.data.QuestionRepository
import kotlinx.coroutines.launch

private val Navy = Color(0xFF102A43)
private val Teal = Color(0xFF0F9D91)
private val Page = Color(0xFFF4F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = QuestionRepository(QuestionDatabase.get(this).questionDao())
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Teal, secondary = Navy, background = Page)
            ) { MedicalApp(repository) }
        }
    }
}

@Composable
private fun MedicalApp(repository: QuestionRepository) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("جاري تجهيز بنك الأسئلة…") }
    var current by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var answered by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { repository.questions() }
            .onSuccess { questions = it; message = "متاح دون إنترنت" }
            .onFailure { message = "تعذر تحميل الأسئلة: ${it.localizedMessage}" }
        loading = false
    }

    fun startExam() {
        current = 0; selected = -1; answered = false; score = 0
        started = true; finished = false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("MultiChoice", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("MEDICAL", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = Navy, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✚", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(26.dp))

        if (loading) {
            CircularProgressIndicator(color = Teal, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            return@Column
        }

        if (!started) {
            Surface(color = Navy, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.End) {
                    Text("مرحباً بك في بنك الأسئلة الطبية", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("تدرّب، اختبر معلوماتك، وراجع تفسير الإجابات.", color = Color(0xFFD8E5EE), textAlign = TextAlign.End)
                    Spacer(Modifier.height(18.dp))
                    Text("${questions.size} أسئلة متاحة", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("التخصصات", fontSize = 19.sp, color = Navy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ابدأ الاختبار", color = Teal, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { if (questions.isNotEmpty()) startExam() })
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("طب الأطفال", color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("أسئلة اختيار من متعدد", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    message = "جارٍ الاتصال بالمصدر السحابي…"
                    scope.launch {
                        repository.syncFromCloud()
                            .onSuccess { count ->
                                questions = repository.questions()
                                message = "تمت مزامنة $count سؤالاً"
                            }
                            .onFailure { message = it.localizedMessage ?: "فشلت المزامنة" }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("مزامنة الأسئلة من السحابة") }
            Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Text("ملاحظة: الأسئلة الحالية نموذجية للتجربة التعليمية وليست بديلاً عن المراجع الطبية المعتمدة.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.End)
        } else if (finished) {
            Surface(color = Color.White, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("انتهى الاختبار", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("$score / ${questions.size}", color = Teal, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("إجابات صحيحة", color = Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { started = false; finished = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("العودة للرئيسية")
                    }
                }
            }
        } else {
            val q = questions[current]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${current + 1} / ${questions.size}", color = Teal, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(q.category, color = Navy, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (current + 1).toFloat() / questions.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(), color = Teal
            )
            Spacer(Modifier.height(22.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.End) {
                    Text("السؤال", color = Teal, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(q.question, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.height(16.dp))
            q.options().forEachIndexed { index, option ->
                val bg = when {
                    answered && index == q.correctIndex -> Color(0xFFE1F5EC)
                    answered && index == selected -> Color(0xFFFFE8E6)
                    selected == index -> Color(0xFFDDF4F1)
                    else -> Color.White
                }
                Surface(
                    color = bg, shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable(enabled = !answered) { selected = index }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f), color = Navy, textAlign = TextAlign.End)
                        Spacer(Modifier.width(12.dp))
                        Text(listOf("أ", "ب", "ج", "د")[index], color = Teal, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (answered) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0xFFEAF2F8), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.End) {
                        Text(if (selected == q.correctIndex) "إجابة صحيحة ✓" else "الإجابة الصحيحة: ${q.options()[q.correctIndex]}", color = Teal, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(q.explanation, color = Navy, textAlign = TextAlign.End)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (!answered) {
                        if (selected >= 0) { answered = true; if (selected == q.correctIndex) score++ }
                    } else {
                        if (current < questions.lastIndex) {
                            current++; selected = -1; answered = false
                        } else finished = true
                    }
                },
                enabled = answered || selected >= 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (!answered) "تأكيد الإجابة" else if (current == questions.lastIndex) "عرض النتيجة" else "السؤال التالي")
            }
            TextButton(onClick = { started = false }) { Text("إنهاء والعودة للرئيسية") }
        }
    }
}
