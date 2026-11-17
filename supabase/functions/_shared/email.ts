// 학교 메일 인증 공통 코드 (Edge Function끼리 같이 쓴다)
import { createClient } from "npm:@supabase/supabase-js@2.45.4";
import nodemailer from "npm:nodemailer@6.9.16";

export const SCHOOL_EMAIL = /^[A-Za-z0-9._%+-]+@sookmyung\.ac\.kr$/;
export const CODE_VALID_SECONDS = 180; // 3분 (앱 타이머와 같음)
export const MAX_ATTEMPTS = 5;

// service_role 키는 Edge Function 환경변수에만 있다. 앱에는 절대 넣지 않는다.
export const admin = () =>
  createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

// 인증번호는 원문 대신 "메일+번호"의 SHA-256 해시로만 저장
export async function hashCode(email: string, code: string): Promise<string> {
  const data = new TextEncoder().encode(`${email}:${code}:${Deno.env.get("EMAIL_CODE_PEPPER") ?? ""}`);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

// ---------------------------------------------------------------------
// 메일 보내기 (Gmail SMTP 우선, 없으면 Resend)
// - Gmail: GMAIL_USER(보내는 Gmail 주소), GMAIL_APP_PASSWORD(앱 비밀번호 16자리)
// - Resend: RESEND_API_KEY, EMAIL_FROM (도메인 인증이 필요)
// Supabase Edge Function은 25/587 포트가 막혀 있어서 Gmail은 465(SSL)로 보낸다.
// ---------------------------------------------------------------------

export async function sendMail(to: string, subject: string, text: string): Promise<boolean> {
  const gmailUser = Deno.env.get("GMAIL_USER");
  const gmailPass = Deno.env.get("GMAIL_APP_PASSWORD");
  if (gmailUser && gmailPass) {
    try {
      const transport = nodemailer.createTransport({
        host: "smtp.gmail.com",
        port: 465,
        secure: true,
        auth: { user: gmailUser, pass: gmailPass.replaceAll(" ", "") },
      });
      await transport.sendMail({ from: `SnowChat <${gmailUser}>`, to, subject, text });
      return true;
    } catch (e) {
      console.error("gmail send failed", e);
      return false;
    }
  }

  const resendKey = Deno.env.get("RESEND_API_KEY");
  if (!resendKey) {
    console.error("메일 발송 비밀값이 없어요 (GMAIL_USER/GMAIL_APP_PASSWORD 또는 RESEND_API_KEY)");
    return false;
  }
  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${resendKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ from: Deno.env.get("EMAIL_FROM"), to, subject, text }),
  });
  if (!res.ok) console.error("resend failed", res.status, await res.text());
  return res.ok;
}
