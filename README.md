# MultiChoice Medical

منصة تعليم طبي تجارية قيد التطوير، تتكون من تطبيقين Android وخادم مستقل.

## التطبيقات
| التطبيق | Package ID | الوظيفة |
|---|---|---|
| MultiChoice Medical | com.multichoice.medical | تطبيق الطالب |
| MultiChoice Admin | com.multichoice.admin | تطبيق الإدارة |

## Backend مستقل على هاتف Android
- Go + Fiber للـ API.
- PostgreSQL لقاعدة البيانات.
- التشغيل مباشرة عبر Termux دون Docker.
- مجلد المشروع: `backend/`.
- افتراضياً يستمع الخادم على `127.0.0.1:8080` وقاعدة البيانات على `127.0.0.1:5432`.
- نقاط الفحص: `GET /health` و `GET /ready`.

ابدأ من [backend/README.md](backend/README.md) لتعليمات إعداد Termux وPostgreSQL وبناء الخادم.

**تنبيه:** الخادم الحالي تأسيسي ويحتوي على health/readiness فقط. تسجيل الدخول وإدارة الأسئلة والتصحيح والاشتراكات لم تكتمل بعد. لا تفتح المنفذ للإنترنت قبل إضافة المصادقة وTLS والحماية.

## بناء تطبيقات Android
المتطلبات: JDK 17 وAndroid SDK 35 وGradle 8.9.

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :adminApp:assembleDebug
```

GitHub Actions ينشر:
- `MultiChoiceMedical-client-debug-apk`
- `MultiChoiceAdmin-debug-apk`

هذه نسخ Debug للاختبار وليست إصدارات Release للنشر في Google Play.

## المعمارية
راجع [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) لخطة النظام الكاملة. ملف `supabase/migrations/` مسودة سابقة لخيار Supabase ولا يستخدمه Backend الهاتف الحالي؛ مصدر قاعدة البيانات المعتمد هو `backend/migrations/`.

## السلامة الطبية
المحتوى التجريبي لا يمثل بنك أسئلة طبياً مكتملاً أو معتمداً. يجب مراجعة كل سؤال وإجابة وشرح ومصدر بواسطة مختصين وبالرجوع إلى مراجع طبية حديثة قبل استخدامه للتحضير الرسمي أو اتخاذ قرار سريري.
