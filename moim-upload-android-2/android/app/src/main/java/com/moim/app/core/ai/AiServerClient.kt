package com.moim.app.core.ai

import com.moim.app.BuildConfig
import com.moim.app.core.supabase.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 서버(ai-server, FastAPI) 호출 공통 코드.
 * - 주소: local.properties의 ai.serverUrl
 * - 인증: Supabase 로그인 토큰을 Authorization 헤더로 보낸다 (서버가 Supabase에 확인)
 * Claude API 키는 앱에 없다. 서버 환경변수에만 있다.
 */
@Singleton
class AiServerClient @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val http by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    /** POST JSON → 응답 본문(JSON 문자열). 실패하면 null */
    suspend fun postJson(path: String, jsonBody: String): String? {
        val request = request(path, jsonBody) ?: return null
        return http.preparePost(request).execute { response ->
            if (response.status.isSuccess()) response.bodyAsText() else null
        }
    }

    /** POST JSON → SSE 응답 줄 목록. 실패하면 null */
    suspend fun postSse(path: String, jsonBody: String): List<String>? {
        val request = request(path, jsonBody)?.apply { header(HttpHeaders.Accept, "text/event-stream") } ?: return null
        return http.preparePost(request).execute { response ->
            if (!response.status.isSuccess()) return@execute null
            val channel = response.bodyAsChannel()
            buildList { while (true) add(channel.readUTF8Line() ?: break) }
        }
    }

    private fun request(path: String, jsonBody: String): HttpRequestBuilder? {
        if (!isConfigured) return null
        val token = supabase.auth.currentSessionOrNull()?.accessToken ?: return null
        return HttpRequestBuilder().apply {
            url("${BuildConfig.AI_SERVER_URL.trimEnd('/')}$path")
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }
    }

    companion object {
        /** AI 서버 주소와 Supabase 키가 모두 있어야 쓸 수 있다 */
        val isConfigured: Boolean
            get() = BuildConfig.AI_SERVER_URL.isNotBlank() && SupabaseConfig.isConfigured
    }
}
