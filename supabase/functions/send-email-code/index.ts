// 학교 웹메일로 6자리 인증번호를 보낸다 (회원가입 S-03)
// 필요한 환경변수: RESEND_API_KEY, EMAIL_FROM (예: "moim <no-reply@도메인>"), EMAIL_CODE_PEPPER
import { admin, CODE_VALID_SECONDS, hashCode, json, SCHOOL_EMAIL } from "../_shared/email.ts";

const RESEND_INTERVAL_SECONDS = 30; // 너무 자주 재발송하지 못하게

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method" }, 405);
  const { email: raw } = await req.json().catch(() => ({}));
  const email = String(raw ?? "").trim().toLowerCase();
  if (!SCHOOL_EMAIL.test(email)) return json({ error: "invalid_email" }, 400);

  const db = admin();

  // 이미 가입된 학교 메일이면 409
  const { count } = await db.from("profiles").select("id", { count: "exact", head: true }).eq("school_email", email);
  if ((count ?? 0) > 0) return json({ error: "duplicate_email" }, 409);

  const { data: prev } = await db.from("email_verifications").select("created_at").eq("email", email).maybeSingle();
  if (prev && Date.now() - new Date(prev.created_at).getTime() < RESEND_INTERVAL_SECONDS * 1000) {
    return json({ error: "too_soon" }, 429);
  }

  const code = String(crypto.getRandomValues(new Uint32Array(1))[0] % 1_000_000).padStart(6, "0");
  const now = new Date();
  const { error } = await db.from("email_verifications").upsert({
    email,
    code_hash: await hashCode(email, code),
    expires_at: new Date(now.getTime() + CODE_VALID_SECONDS * 1000).toISOString(),
    attempts: 0,
    verified_at: null,
    created_at: now.toISOString(),
  });
  if (error) return json({ error: "db" }, 500);

  const sent = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      from: Deno.env.get("EMAIL_FROM"),
      to: email,
      subject: "[moim] 학교 메일 인증번호",
      text: `moim 인증번호는 ${code} 입니다.\n3분 안에 앱에 입력해 주세요.`,
    }),
  });
  if (!sent.ok) return json({ error: "mail" }, 502);
  return json({ ok: true });
});
