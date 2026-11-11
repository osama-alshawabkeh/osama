# MultiChoice Medical

تطبيق Android عربي للتدرب على أسئلة الاختيار من متعدد الطبية (MCQ)، يبدأ ببنك تجريبي لطب الأطفال.

## ما يعمل في النسخة الحالية
- واجهة عربية RTL مبنية بـ Kotlin وJetpack Compose.
- اختيار الإجابة ثم إظهار التصحيح والتفسير.
- احتساب نتيجة الاختبار.
- حفظ بنك الأسئلة محلياً باستخدام Room، والعمل دون اتصال بعد أول تشغيل.
- مزامنة تنزيلية اختيارية من Supabase REST.
- بناء APK تجريبي تلقائياً بواسطة GitHub Actions.

## تشغيل محلي
- Android Studio حديث.
- JDK 17.
- Gradle 8.9.

```bash
gradle assembleDebug
```

ملف APK الناتج: `app/build/outputs/apk/debug/app-debug.apk`

## تنزيل APK من GitHub
افتح تبويب Actions، ثم آخر تشغيل ناجح لسير عمل **Android APK**، ثم نزّل Artifact باسم `MultiChoiceMedical-debug-apk`. هذا APK تجريبي موقّع بمفتاح debug، وليس إصدار متجر Google Play.

## إعداد Supabase (اختياري)
1. أنشئ مشروع Supabase.
2. نفّذ ملف `supabase/schema.sql` في SQL Editor.
3. نفّذ ملف `supabase/seed.sql` لإضافة الأسئلة التجريبية.
4. أضف GitHub Actions Secrets باسم `SUPABASE_URL` و`SUPABASE_ANON_KEY`.
5. أعد تشغيل workflow أو ادفع commit جديداً.

التطبيق يجلب الأسئلة من جدول `public.questions` ويحفظها محلياً. المزامنة الحالية تنزيلية فقط؛ لا توجد واجهة إدارة أسئلة أو مزامنة رفع من التطبيق حتى الآن. لا تضع مفتاح `service_role` داخل التطبيق. راجع سياسات RLS قبل نشر بنك أسئلة تجاري أو خاص.

## تنبيه طبي
الأسئلة الموجودة أمثلة أولية لأغراض اختبار التطبيق فقط، وليست بنكاً معتمداً لامتحان البورد ولا مرجعاً لاتخاذ القرار السريري. يجب مراجعة المحتوى والإجابات والتفسيرات من مصادر طبية حديثة قبل الاستخدام التعليمي الرسمي.

## الحزمة
`com.multichoice.medical`
