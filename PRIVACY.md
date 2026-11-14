# XioVPN — Privacy Policy / سیاست حریم خصوصی

Last updated: 2026-11-12

## English

XioVPN is a network client (VPN / proxy). It does not collect, store, sell or
transmit personal data to the developer.

**What stays on your device**
- Configurations you add, saved routes, bookmarks, app settings, traffic
  counters and the subscription code are stored only in the app's private
  storage on your device.
- The subscription code is validated **offline** with a cryptographic signature.
  No account, email address or phone number is required or transmitted.

**What leaves your device**
- Traffic you route through the tunnel goes to the proxy/VPN server named in the
  configuration **you** chose. The developer does not operate that server and
  never sees your traffic.
- If you use the in-app update check, the app requests the public release list
  from GitHub (`api.github.com`). That request contains no personal data beyond
  what any HTTPS request carries (IP address, user agent).
- Diagnostic/log files are written locally. They are shared only if you export
  them yourself.

**Permissions and why they are needed**
- `INTERNET` — establish the tunnel and check for updates.
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`,
  `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` — keep the connection
  alive and show its status notification.
- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` — detect connectivity changes to
  reconnect or restore the system proxy.
- `RECEIVE_BOOT_COMPLETED` — optional auto-connect after reboot, only if you
  enable it.
- `ACCESS_MOCK_LOCATION` — only used by the optional developer GPS feature.
  It is not used to track you.

**Children**: not directed at children under 13.
**Data deletion**: uninstalling the app removes all locally stored data.

Contact: <your support channel here>

---

## فارسی

XioVPN یک کلاینت شبکه (VPN / پروکسی) است. هیچ داده شخصی را جمعآوری، ذخیره،
فروش یا به توسعهدهنده ارسال نمیکند.

**چه چیزی روی گوشی شما میماند**
- کانفیگهایی که اضافه میکنید، مسیرهای ذخیرهشده، تنظیمات، شمارنده ترافیک و
  کد اشتراک فقط در حافظه خصوصی برنامه روی گوشی شما ذخیره میشوند.
- کد اشتراک **آفلاین** با امضای رمزنگاریشده بررسی میشود. هیچ حساب کاربری،
  ایمیل یا شماره تلفنی لازم نیست و ارسال نمیشود.

**چه چیزی از گوشی شما خارج میشود**
- ترافیکی که از تونل عبور میدهید به همان سروری میرود که **خودتان** در کانفیگ
  انتخاب کردهاید. توسعهدهنده آن سرور را اداره نمیکند و ترافیک شما را نمیبیند.
- اگر بررسی بهروزرسانی را بزنید، برنامه فهرست نسخههای عمومی را از GitHub
  میگیرد. این درخواست جز همان چیزهایی که هر درخواست HTTPS دارد (آیپی و
  user agent) داده دیگری ندارد.
- فایلهای لاگ محلیاند و فقط اگر خودتان خروجی بگیرید به اشتراک گذاشته میشوند.

**مجوزها و دلیلشان** — اینترنت، سرویس پیشزمینه و نوتیفیکیشن برای زنده نگه
داشتن اتصال؛ وضعیت شبکه/WiFi برای تشخیص تغییر اتصال؛ اجرا پس از بوت اختیاری؛
موقعیت مکانی جعلی فقط برای قابلیت اختیاری توسعهدهنده.

**اطفال**: برای زیر ۱۳ سال ساخته نشده است.
**حذف داده**: با حذف برنامه، همه دادههای محلی پاک میشوند.
