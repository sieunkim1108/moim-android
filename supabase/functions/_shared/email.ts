// 학교 메일 인증 공통 코드 (Edge Function끼리 같이 쓴다)
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

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
