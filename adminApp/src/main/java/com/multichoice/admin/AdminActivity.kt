package com.multichoice.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF142D4E)
private val Blue = Color(0xFF2878C8)
private val Teal = Color(0xFF11998E)
private val Canvas = Color(0xFFF3F6FA)
private val Muted = Color(0xFF718096)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Teal, background = Canvas)) {
                    AdminApp()
                }
            }
        }
    }
}

private data class AdminTab(val title: String, val symbol: String)

@Composable
private fun AdminApp() {
    val tabs = remember {
        listOf(
            AdminTab("الرئيسية", "⌂"),
            AdminTab("الأسئلة", "☷"),
            AdminTab("المستخدمون", "♙"),
            AdminTab("التقارير", "▥"),
            AdminTab("الإعدادات", "⚙")
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.symbol, fontSize = 19.sp) },
                        label = { Text(tab.title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Navy, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("✚", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("MultiChoice", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ADMIN CONTROL CENTER", color = Teal, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Surface(color = Color(0xFFE7F5EF), shape = RoundedCornerShape(30.dp)) {
                    Text("وضع الإدارة", color = Color(0xFF18794E), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            when (selectedTab) {
                0 -> DashboardScreen(onOpenQuestions = { selectedTab = 1 }, onOpenUsers = { selectedTab = 2 })
                1 -> ManagementScreen("إدارة بنك الأسئلة", "إضافة الأسئلة وتصنيفها ومراجعة المحتوى ونشره للطلاب.")
                2 -> ManagementScreen("إدارة المستخدمين", "إدارة حسابات الطلاب والأدوار وحالة الاشتراكات.")
                3 -> ManagementScreen("التقارير والتحليلات", "متابعة التسجيلات والاختبارات ومعدلات النجاح والتفاعل.")
                else -> ManagementScreen("إعدادات المنصة", "إعدادات الهوية والاشتراكات والإشعارات وسياسات الوصول.")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun DashboardScreen(onOpenQuestions: () -> Unit, onOpenUsers: () -> Unit) {
    Text("مرحباً بك في لوحة الإدارة", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
    Text("نظرة عامة على منصة التعليم الطبي", color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(18.dp))
    Surface(color = Navy, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("مساحة الإدارة المركزية", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("أدر المحتوى والطلاب والاختبارات من تطبيق مستقل بصلاحيات مخصصة.", color = Color(0xFFD7E4F1), fontSize = 13.sp, textAlign = TextAlign.End)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color(0xFF294768), shape = RoundedCornerShape(10.dp)) {
                Text("الربط السحابي قيد التجهيز", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("مؤشرات المنصة", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("الطلاب", "—", "إجمالي الحسابات", Modifier.weight(1f), Blue)
        MetricCard("الأسئلة", "—", "بنك الأسئلة", Modifier.weight(1f), Teal)
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("الاختبارات", "—", "محاولات الطلاب", Modifier.weight(1f), Color(0xFF8756C5))
        MetricCard("الاشتراكات", "—", "الاشتراكات النشطة", Modifier.weight(1f), Color(0xFFCE8A2B))
    }
    Spacer(Modifier.height(20.dp))
    Text("إجراءات سريعة", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    ActionCard("إدارة بنك الأسئلة", "إضافة سؤال أو مراجعة الأسئلة الحالية", "＋", onOpenQuestions)
    ActionCard("إدارة المستخدمين", "الحسابات والأدوار والاشتراكات", "♙", onOpenUsers)
    Spacer(Modifier.height(16.dp))
    Text("تنبيه التطوير", color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text("هذه أول نسخة تأسيسية لواجهة الإدارة. الأرقام والعمليات ستظهر بعد ربط Supabase وتفعيل صلاحيات المدير؛ لا تعرض هذه النسخة بيانات إنتاجية بعد.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier, accent: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
            Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(9.dp)) {
                Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = Navy, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ActionCard(title: String, description: String, symbol: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Blue, fontSize = 25.sp)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(12.dp))
            Surface(color = Color(0xFFEAF2FC), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(symbol, color = Blue, fontSize = 21.sp) }
            }
        }
    }
}

@Composable
private fun ManagementScreen(title: String, description: String) {
    Text(title, color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
    Text(description, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(18.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End) {
            Text("هذه الوحدة ضمن المرحلة التالية", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("سيتم تفعيل الوظائف بعد إكمال المصادقة وربط واجهات API وقواعد الصلاحيات. لن يتم استخدام بيانات وهمية على أنها بيانات حقيقية.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.End)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color(0xFFEAF2FC), shape = RoundedCornerShape(10.dp)) {
                Text("قيد التطوير", color = Blue, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}
