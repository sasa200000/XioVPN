# XioVPN subscription bot — Cloudflare Worker

This is the **recommended** host for the admin bot: Telegram pushes each update
to the worker over a webhook, so replies are instant and nothing has to stay
running. No polling loop, no VPS.

## Live deployment (already done)

- Worker name: `xiovpn-bot`
- URL: https://xiovpn-bot.nimasivi.workers.dev
- Telegram webhook: set to that URL with a `secret_token`

## Bindings (set in the Worker's Settings -> Variables)

| Name | Type | Value |
|---|---|---|
| `BOT_TOKEN` | Secret | your BotFather token |
| `ADMIN_IDS` | Plain | your numeric Telegram id(s), comma-separated |
| `SUB_SECRET` | Secret | HMAC secret — **must equal the app's** `DEFAULT_SECRET` or codes will be rejected |
| `WEBHOOK_SECRET` | Secret | random string; also passed as Telegram's `secret_token` |
| `GITHUB_REPO` | Plain | `owner/repo` holding the ledger |
| `GITHUB_PATH` | Plain | e.g. `bot-data/subscriptions.json` |
| `GITHUB_BRANCH` | Plain | `main` |
| `GITHUB_TOKEN` | Secret | fine-grained PAT with **Contents: read & write** on that repo |
| `SUBSCRIPTIONS` | KV (optional) | if present, used instead of GitHub for the ledger |

The ledger needs *some* persistent store. A KV binding is cleanest; an account
without KV permission can use the GitHub file instead (the worker supports both).

## Deploying an update

```bash
cd worker
# upload worker.js + metadata with your own token
```

Or use `wrangler` if you prefer a config file.

## Endpoints

- `GET /` → health check; says whether a webhook secret is configured.
- `POST /` → Telegram updates (rejects a wrong/missing `X-Telegram-Bot-Api-Secret-Token`).

## Commands

Buttons: ۳ روز · ۱۰ روز · ۱ ماه · ۲ ماه · ۳ ماه · ۶ ماه · ۱ سال · ♾ نامحدود ·
🗑 حذف کد · 📋 لیست کدها · ✅ بررسی کد

Text: `/new <days> [count]`, `/list`, `/revoke`, `/check <code>`

## Important: who may talk to the bot

`ADMIN_IDS` is enforced — anyone else gets "این ربات فقط برای ادمین است." Keep
that list short; the bot can mint unlimited codes.
