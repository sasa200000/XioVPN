/**
 * XioVPN subscription bot as a Cloudflare Worker.
 *
 * Telegram pushes each update to this worker over a webhook, so replies are
 * instant and there is nothing to keep running — no polling loop, no server to
 * restart. State (the issued-code ledger) lives in Workers KV.
 *
 * Required bindings (Worker Settings -> Variables):
 *   KV namespace  SUBSCRIPTIONS   stores the ledger + webhook secret
 *   Secret        BOT_TOKEN       the BotFather token
 *   Secret        ADMIN_IDS       comma-separated numeric Telegram ids
 *   Secret        SUB_SECRET      HMAC secret for codes (same value as the app)
 */

const B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
const PAYLOAD_VERSION = 1;
const SERIAL_BYTES = 2;
const MAC_BYTES = 5;
const MAX_DAYS = 3650;
const UNLIMITED = 0;
const LEDGER_KEY = "ledger";

// ------------------------------------------------------------------ storage
// The ledger must survive between requests. A KV binding is best; when the
// account cannot create one, fall back to a JSON file in a GitHub repo, which
// only needs a normal repo token.
function ghHeaders(env) {
  return {
    authorization: `Bearer ${env.GITHUB_TOKEN}`,
    accept: "application/vnd.github+json",
    "user-agent": "xiovpn-bot",
  };
}
async function ghRead(env) {
  const res = await fetch(
    `https://api.github.com/repos/${env.GITHUB_REPO}/contents/${env.GITHUB_PATH}`,
    { headers: ghHeaders(env) },
  );
  if (res.status === 404) return { rows: [], sha: null };
  if (!res.ok) throw new Error(`github read ${res.status}`);
  const body = await res.json();
  let rows = [];
  try {
    rows = JSON.parse(atob(body.content.replace(/\n/g, ""))) || [];
  } catch { rows = []; }
  return { rows: Array.isArray(rows) ? rows : [], sha: body.sha };
}
async function ghWrite(env, rows, sha) {
  const payload = {
    message: "bot: update subscription ledger",
    content: btoa(unescape(encodeURIComponent(JSON.stringify(rows, null, 2)))),
    branch: env.GITHUB_BRANCH || "main",
  };
  if (sha) payload.sha = sha;
  const res = await fetch(
    `https://api.github.com/repos/${env.GITHUB_REPO}/contents/${env.GITHUB_PATH}`,
    { method: "PUT", headers: { ...ghHeaders(env), "content-type": "application/json" }, body: JSON.stringify(payload) },
  );
  if (!res.ok) throw new Error(`github write ${res.status}`);
}

const DURATIONS = [
  ["۳ روز", 3], ["۱۰ روز", 10],
  ["۱ ماه", 30], ["۲ ماه", 60],
  ["۳ ماه", 90], ["۶ ماه", 180],
  ["۱ سال", 365],
];

// ---------------------------------------------------------------- base32
function b32encode(bytes) {
  let out = "", buffer = 0, bits = 0;
  for (const b of bytes) {
    buffer = (buffer << 8) | b; bits += 8;
    while (bits >= 5) { out += B32[(buffer >> (bits - 5)) & 31]; bits -= 5; }
  }
  if (bits > 0) out += B32[(buffer << (5 - bits)) & 31];
  return out;
}
function b32decode(text) {
  let buffer = 0, bits = 0; const out = [];
  for (const ch of text.toUpperCase()) {
    const idx = B32.indexOf(ch);
    if (idx < 0) return null;
    buffer = (buffer << 5) | idx; bits += 5;
    if (bits >= 8) { out.push((buffer >> (bits - 8)) & 255); bits -= 8; }
  }
  return new Uint8Array(out);
}
async function hmac(secret, payload) {
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, payload);
  return new Uint8Array(sig).slice(0, MAC_BYTES);
}
async function generate(secret, days) {
  if (days !== UNLIMITED && (days < 1 || days > MAX_DAYS)) throw new Error("bad days");
  const serial = crypto.getRandomValues(new Uint16Array(1))[0];
  const payload = new Uint8Array(1 + 2 + SERIAL_BYTES);
  payload[0] = PAYLOAD_VERSION;
  payload[1] = (days >> 8) & 255; payload[2] = days & 255;
  payload[3] = (serial >> 8) & 255; payload[4] = serial & 255;
  const mac = await hmac(secret, payload);
  return `XIO-${b32encode(payload)}-${b32encode(mac)}`;
}
async function parse(secret, code) {
  let text = String(code || "").trim().toUpperCase().replace(/[-_\s]/g, "");
  if (text.startsWith("XIO")) text = text.slice(3);
  if (text.length < 10) return null;
  const payload = b32decode(text.slice(0, text.length - 8));
  const provided = b32decode(text.slice(text.length - 8));
  if (!payload || !provided || payload.length !== 1 + 2 + SERIAL_BYTES) return null;
  const expected = await hmac(secret, payload);
  let diff = 0;
  for (let i = 0; i < MAC_BYTES; i++) diff |= expected[i] ^ (provided[i] || 0);
  if (diff !== 0 || payload[0] !== PAYLOAD_VERSION) return null;
  const days = (payload[1] << 8) | payload[2];
  if (days !== UNLIMITED && (days < 1 || days > MAX_DAYS)) return null;
  return { days, unlimited: days === UNLIMITED };
}

function json(body) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "content-type": "application/json", "cache-control": "no-store" },
  });
}

// ---------------------------------------------------------------- telegram
async function tg(env, method, payload) {
  const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  return res.json().catch(() => ({}));
}
function isAdmin(env, id) {
  return String(env.ADMIN_IDS || "").split(",").map((s) => s.trim()).filter(Boolean)
    .includes(String(id));
}
function menu() {
  const rows = [];
  let row = [];
  for (const [label, days] of DURATIONS) {
    row.push({ text: label, callback_data: `new:${days}` });
    if (row.length === 2) { rows.push(row); row = []; }
  }
  if (row.length) rows.push(row);
  rows.push([{ text: "♾ نامحدود", callback_data: `new:${UNLIMITED}` }]);
  rows.push([
    { text: "🗑 حذف کد", callback_data: "revoke:menu" },
    { text: "📋 لیست کدها", callback_data: "list" },
  ]);
  rows.push([{ text: "✅ بررسی کد", callback_data: "check" }]);
  return { inline_keyboard: rows };
}
async function ledger(env) {
  if (env.SUBSCRIPTIONS) {
    const raw = await env.SUBSCRIPTIONS.get(LEDGER_KEY);
    try { const v = raw ? JSON.parse(raw) : []; return Array.isArray(v) ? v : []; }
    catch { return []; }
  }
  try { return (await ghRead(env)).rows; } catch { return []; }
}
async function saveLedger(env, rows) {
  const trimmed = rows.slice(-500);
  if (env.SUBSCRIPTIONS) {
    await env.SUBSCRIPTIONS.put(LEDGER_KEY, JSON.stringify(trimmed));
    return;
  }
  const cur = await ghRead(env);
  await ghWrite(env, trimmed, cur.sha);
}
function durLabel(days) {
  return days === UNLIMITED ? "نامحدود" : (DURATIONS.find(([, d]) => d === days)?.[0] || `${days} روز`);
}

async function handleNew(env, chatId, days, userId) {
  const code = await generate(env.SUB_SECRET, days);
  const rows = await ledger(env);
  rows.push({ code, days, note: `bot:${userId}`, at: new Date().toISOString(), revoked: false });
  await saveLedger(env, rows);
  await tg(env, "sendMessage", {
    chat_id: chatId,
    text: `✅ کد اشتراک ${durLabel(days)}:\n\n${code}`,
    reply_markup: menu(),
  });
}
async function handleList(env, chatId) {
  const rows = await ledger(env);
  if (!rows.length) {
    return tg(env, "sendMessage", { chat_id: chatId, text: "هنوز کدی ساخته نشده.", reply_markup: menu() });
  }
  const tail = rows.slice(-12);
  const body = tail.map((r) => `${r.revoked ? "🗑" : "✅"} ${r.code}  ${r.days === 0 ? "∞" : r.days + "d"}`).join("\n");
  await tg(env, "sendMessage", {
    chat_id: chatId,
    text: `آخرین ${tail.length} کد از ${rows.length}:\n\n${body}`,
    reply_markup: menu(),
  });
}
async function handleRevokeMenu(env, chatId) {
  const rows = (await ledger(env)).filter((r) => !r.revoked);
  if (!rows.length) {
    return tg(env, "sendMessage", { chat_id: chatId, text: "کد فعالی برای حذف نیست.", reply_markup: menu() });
  }
  const buttons = rows.slice(-10).map((r) => ([{
    text: `🗑 ${r.code} (${r.days === 0 ? "∞" : r.days + "d"})`,
    callback_data: `revoke:do:${r.code}`,
  }]));
  buttons.push([{ text: "‹ بازگشت", callback_data: "menu" }]);
  await tg(env, "sendMessage", {
    chat_id: chatId, text: "کدام کد حذف شود؟", reply_markup: { inline_keyboard: buttons },
  });
}
async function handleRevoke(env, chatId, code) {
  const rows = await ledger(env);
  let hit = false;
  for (const r of rows) {
    if (r.code === code) { r.revoked = true; r.revokedAt = new Date().toISOString(); hit = true; }
  }
  if (hit) await saveLedger(env, rows);
  await tg(env, "sendMessage", {
    chat_id: chatId,
    text: hit
      ? `🗑 حذف شد (از لیست فعال):\n${code}\n\nاگر روی گوشی فعال شده باشد تا پایان اعتبارش باز میماند — برای قفل فوری، کاربر باید برنامه را پاک کند.`
      : "این کد در لیست نبود.",
    reply_markup: menu(),
  });
}
async function handleCallback(env, cb) {
  const chatId = cb.message?.chat?.id;
  const userId = cb.from?.id;
  if (chatId == null || userId == null) return;
  if (!isAdmin(env, userId)) {
    return tg(env, "answerCallbackQuery", { callback_query_id: cb.id, text: "فقط ادمین" });
  }
  await tg(env, "answerCallbackQuery", { callback_query_id: cb.id });
  const data = cb.data || "";
  if (data === "menu") {
    return tg(env, "sendMessage", { chat_id: chatId, text: "یک بازه را انتخاب کنید:", reply_markup: menu() });
  }
  if (data.startsWith("new:")) return handleNew(env, chatId, parseInt(data.slice(4), 10), userId);
  if (data === "list") return handleList(env, chatId);
  if (data === "check") {
    return tg(env, "sendMessage", { chat_id: chatId, text: "کد را بفرستید: /check XIO-XXXXXXXX-XXXXXXXX", reply_markup: menu() });
  }
  if (data === "revoke:menu") return handleRevokeMenu(env, chatId);
  if (data.startsWith("revoke:do:")) return handleRevoke(env, chatId, data.slice("revoke:do:".length));
}
async function handleMessage(env, msg) {
  const chatId = msg.chat?.id;
  const userId = msg.from?.id;
  const text = (msg.text || "").trim();
  if (!chatId || !userId || !text) return;
  if (!isAdmin(env, userId)) {
    return tg(env, "sendMessage", { chat_id: chatId, text: "این ربات فقط برای ادمین است." });
  }
  const [rawCmd, ...args] = text.split(/\s+/);
  const cmd = (rawCmd || "").toLowerCase().split("@")[0];
  if (cmd === "/start" || cmd === "/menu" || cmd === "/help") {
    const rows = await ledger(env);
    return tg(env, "sendMessage", {
      chat_id: chatId,
      text: `XioVPN — پنل کد اشتراک\n\nصادرشده: ${rows.length} | حذفشده: ${rows.filter((r) => r.revoked).length}\n\nیک بازه را انتخاب کنید:`,
      reply_markup: menu(),
    });
  }
  if (cmd === "/new") {
    const days = parseInt(args[0], 10);
    const count = Math.min(Math.max(parseInt(args[1] || "1", 10) || 1, 1), 20);
    if (!Number.isFinite(days)) {
      return tg(env, "sendMessage", { chat_id: chatId, text: "usage: /new <days> [count]", reply_markup: menu() });
    }
    for (let i = 0; i < count; i++) await handleNew(env, chatId, days, userId);
    return;
  }
  if (cmd === "/list") return handleList(env, chatId);
  if (cmd === "/revoke") return handleRevokeMenu(env, chatId);
  if (cmd === "/check") {
    const info = await parse(env.SUB_SECRET, args[0] || "");
    return tg(env, "sendMessage", {
      chat_id: chatId,
      text: info ? `✅ معتبر — ${info.unlimited ? "نامحدود" : info.days + " روز"}` : "❌ نامعتبر",
      reply_markup: menu(),
    });
  }
  return tg(env, "sendMessage", { chat_id: chatId, text: "از دکمه‌ها استفاده کنید.", reply_markup: menu() });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    // Health check / install helper.
    // Health check only on the root path; other GETs have their own routes.
    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "")) {
      const secret = env.WEBHOOK_SECRET || (env.SUBSCRIPTIONS ? await env.SUBSCRIPTIONS.get("webhook_secret") : null);
      return new Response(
        `XioVPN bot worker is running.\n` +
        (secret ? `Webhook secret is set.\n` : `No webhook secret stored yet.\n`),
        { status: 200 },
      );
    }
    // Revocation check for the app: GET /status?code=XIO-...
    // Returns only that code's state, never the whole ledger.
    if (url.pathname === "/status") {
      const code = (url.searchParams.get("code") || "").trim().toUpperCase();
      if (!code) return json({ valid: false, reason: "missing" });
      const info = await parse(env.SUB_SECRET, code);
      if (!info) return json({ valid: false, reason: "signature" });
      let rows = [];
      try { rows = await ledger(env); } catch { rows = []; }
      const row = rows.find((r) => String(r.code).toUpperCase() === code);
      // A code that was never issued by this ledger is still valid if its
      // signature checks out (it may predate the ledger), so only an explicit
      // revocation locks the app.
      const revoked = !!(row && row.revoked);
      return json({
        valid: !revoked,
        revoked,
        unlimited: !!info.unlimited,
        days: info.days,
        checkedAt: new Date().toISOString(),
      });
    }

    if (request.method !== "POST") return new Response("method not allowed", { status: 405 });

    // Telegram lets us require a secret header so random callers cannot post.
    const expected = env.WEBHOOK_SECRET || (env.SUBSCRIPTIONS ? await env.SUBSCRIPTIONS.get("webhook_secret") : null);
    if (expected) {
      const got = request.headers.get("X-Telegram-Bot-Api-Secret-Token");
      if (got !== expected) return new Response("forbidden", { status: 403 });
    }

    let update;
    try { update = await request.json(); }
    catch { return new Response("bad json", { status: 400 }); }

    try {
      if (update.callback_query) await handleCallback(env, update.callback_query);
      else if (update.message) await handleMessage(env, update.message);
    } catch (err) {
      // Answer Telegram fast even when something throws, so it does not retry.
      return new Response("handled with error", { status: 200 });
    }
    return new Response("ok", { status: 200 });
  },
};
