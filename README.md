# MultiChoice Medical

تطبيق أندرويد عربي للتدريب على أسئلة الاختيار من متعدد الطبية، مع بنك أسئلة محلي وإمكانية مزامنته مع Supabase.

## المزايا في النسخة الأولية
- واجهة عربية RTL باستخدام Jetpack Compose.
- أسئلة طبية تجريبية (طب الأطفال) مع تصحيح وشرح.
- تخزين محلي باستخدام Room.
- مزامنة اختيارية للأسئلة من Supabase REST.
- بناء APK تلقائياً عبر GitHub Actions.

## البناء
يتطلب JDK 17 وGradle 8.9:
```bash
gradle assembleDebug
```
الناتج: `app/build/outputs/apk/debug/app-debug.apk`

## إعداد Supabase (اختياري)
أنشئ جدولاً باسم `questions` بالأعمدة: `id` (bigint primary key), `category`, `question`, `option_a`, `option_b`, `option_c`, `option_d`, `correct_index` (0–3), `explanation`.

أضف أسرار GitHub `SUPABASE_URL` و`SUPABASE_ANON_KEY` من Settings → Secrets and variables → Actions. لا تضع مفتاح service_role داخل التطبيق، وفعّل RLS وسياسات القراءة المناسبة.

> المحتوى الطبي تجريبي تعليمي ويجب مراجعته علمياً قبل استخدامه للتحضير الرسمي أو اتخاذ قرارات سريرية.
