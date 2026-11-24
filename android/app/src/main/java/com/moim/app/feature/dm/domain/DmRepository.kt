package com.moim.app.feature.dm.domain

import com.moim.app.core.model.Friend
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

    /**
     * 단체 채팅방을 만든다. 나는 자동으로 들어가고 friends를 초대한다.
     * 성공하면 새 방 id를 돌려준다.
     */
    suspend fun createGroupRoom(name: String, friends: List<Friend>): Result<String>
}
