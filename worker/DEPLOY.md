# استقرار ربات کد اشتراک (راهنمای کامل)

این فایل همه چیز لازم برای بازسازی ربات از صفر را دارد. اگر Worker حذف شد،
با این مراحل دقیقاً همان ربات برمی‌گردد.

## ۱) مقادیر ثابت (نباید تغییر کنند)

```
SUB_SECRET      = <ROTATED — مقدار قدیمی باطل شد>
WEBHOOK_SECRET  = xiovpn-wh-2f8a41c07d9e5b36
GITHUB_REPO     = mohmdnini26/XioVPN-Android
GITHUB_PATH     = bot-data/subscriptions.json
GITHUB_BRANCH   = main
BOT_TOKEN       = <توکن BotFather>
ADMIN_IDS       = <آیدی عددی ادمین>
```

**مهم‌ترین نکته:** `SUB_SECRET` باید **بیت‌به‌بیت** برابر `DEFAULT_SECRET` در
`app/src/main/java/com/xiovpn/mobile/core/Subscription.kt` باشد، وگرنه هیچ کدی
که ربات می‌سازد در اپ پذیرفته نمی‌شود.

## ۲) ساخت Worker

```bash
# via dashboard: Workers & Pages -> Create -> Worker -> نام: xiovpn-bot
# سپس worker.js این پوشه را جای‌گذاری کنید
```

یا با API:

```bash
CF=<cloudflare-token>
ACC=<account-id>
# metadata.json بسازید (bindings در README.md)
curl -X PUT "https://api.cloudflare.com/client/v4/accounts/$ACC/workers/scripts/xiovpn-bot" \
  -H "Authorization: Bearer $CF" \
  -F "metadata=@metadata.json;type=application/json" \
  -F "worker.js=@worker.js;type=application/javascript+module"
```

## ۳) فعال‌کردن آدرس عمومی

```bash
curl -X POST "https://api.cloudflare.com/client/v4/accounts/$ACC/workers/scripts/xiovpn-bot/subdomain" \
  -H "Authorization: Bearer $CF" -H "Content-Type: application/json" \
  -d '{"enabled":true}'
```

پس از آن آدرس: `https://xiovpn-bot.<account-subdomain>.workers.dev`

> بدون این گام، آدرس با خطای `error code: 1042` پاسخ می‌دهد.

## ۴) اتصال به تلگرام

```bash
BOT=<توکن>
URL=https://xiovpn-bot.<subdomain>.workers.dev
curl -X POST "https://api.telegram.org/bot$BOT/setWebhook" \
  --data-urlencode "url=$URL" \
  --data-urlencode "secret_token=xiovpn-wh-2f8a41c07d9e5b36" \
  --data-urlencode 'allowed_updates=["message","callback_query"]'
```

## ۵) بررسی سالم بودن

```bash
curl "$URL"                      # باید بنویسد: XioVPN bot worker is running.
curl "$URL/status?code=XIO-..."  # باید JSON برگرداند
curl -X POST "$URL" -d '{}'      # باید 403 بدهد (هدر راز ندارد)
```

## ۶) دام‌هایی که یک‌بار ما را زمین زد

- **webhook و getUpdates با هم کار نمی‌کنند.** اگر webhook ست باشد، هر
  `getUpdates` خطای Conflict می‌دهد. یکی را انتخاب کنید و دیگری را خاموش کنید.
- **توکن Cloudflare ممکن است KV نسازد** (`Authentication error`). به همین دلیل
  ledger روی فایل گیت‌هاب ذخیره می‌شود، نه KV.
- **health check باید فقط مسیر `/` را بگیرد**، وگرنه `/status` را می‌بلعد.
- **آپلود ماژول worker:** بخش multipart باید `filename="worker.js"` و
  `Content-Type: application/javascript+module` داشته باشد.

## ۷) منابع

- `worker.js` — کد ربات (وب‌هوک، بدون polling)
