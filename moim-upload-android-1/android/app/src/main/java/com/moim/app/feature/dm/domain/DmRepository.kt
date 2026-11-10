package com.moim.app.feature.dm.domain

import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMessage
import kotlinx.coroutines.flow.Flow

/**
 * 채팅방 저장소. Supabase 키가 있으면 SupabaseDmRepository(Realtime), 없으면 FakeDmRepository(메모리).
 */
interface DmRepository {
    fun observeRooms(): Flow<List<ChatRoom>>

    fun observeRoom(roomId: String): Flow<ChatRoom?>

    fun observeMessages(roomId: String): Flow<List<RoomMessage>>

    /** 내가 글을 보낸다 */
    suspend fun sendMessage(roomId: String, text: String): Result<Unit>

    /** 눈송이가 방에 글/추천 카드를 남긴다 (모임 확정 안내 등) */
    suspend fun postBotMessages(roomId: String, messages: List<RoomMessage>): Result<Unit>

    suspend fun markRead(roomId: String)
}
