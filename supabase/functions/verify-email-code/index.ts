// 인증번호 확인. 맞으면 verified_at을 기록하고, 가입 트리거가 30분 안에 인증된 메일만 받아준다.
import { admin, hashCode, json, MAX_ATTEMPTS, SCHOOL_EMAIL } from "../_shared/email.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method" }, 405);
  const body = await req.json().catch(() => ({}));
  const email = String(body.email ?? "").trim().toLowerCase();
  const code = String(body.code ?? "").trim();
  if (!SCHOOL_EMAIL.test(email) || !/^\d{6}$/.test(code)) return json({ verified: false });

  const db = admin();
  const { data: row } = await db.from("email_verifications").select("*").eq("email", email).maybeSingle();
  if (!row || row.attempts >= MAX_ATTEMPTS || new Date(row.expires_at).getTime() < Date.now()) {
    return json({ verified: false });
  }

  if (row.code_hash !== (await hashCode(email, code))) {
    await db.from("email_verifications").update({ attempts: row.attempts + 1 }).eq("email", email);
    return json({ verified: false });
  }

  await db.from("email_verifications").update({ verified_at: new Date().toISOString() }).eq("email", email);
  return json({ verified: true });
});
