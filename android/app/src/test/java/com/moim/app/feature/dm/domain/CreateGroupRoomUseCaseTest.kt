package com.moim.app.feature.dm.domain

import com.moim.app.core.model.Friend
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.usecase.CreateGroupRoomUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateGroupRoomUseCaseTest {

    /** createGroupRoom에 뭐가 넘어왔는지만 기억하는 가짜 저장소 */
    private class RecordingRepo : DmRepository {
        var name: String? = null
        var friends: List<Friend>? = null

        override suspend fun createGroupRoom(name: String, friends: List<Friend>): Result<String> {
            this.name = name
            this.friends = friends
            return Result.success("room-1")
        }

        override fun observeRooms(): Flow<List<ChatRoom>> = emptyFlow()
        override fun observeRoom(roomId: String): Flow<ChatRoom?> = emptyFlow()
        override fun observeMessages(roomId: String): Flow<List<RoomMessage>> = emptyFlow()
        override suspend fun sendMessage(roomId: String, text: String) = Result.success(Unit)
        override suspend fun postBotMessages(roomId: String, messages: List<RoomMessage>) = Result.success(Unit)
        override suspend fun markRead(roomId: String) = Unit
    }

    private val chaewon = Friend("1", "황채원")
    private val jiwon = Friend("2", "손지원")

    @Test
    fun createsRoomWithTrimmedName() = runBlocking {
        val repo = RecordingRepo()
        val id = CreateGroupRoomUseCase(repo)("  모바일 팀플  ", listOf(chaewon, jiwon)).getOrThrow()
        assertEquals("room-1", id)
        assertEquals("모바일 팀플", repo.name)
        assertEquals(listOf(chaewon, jiwon), repo.friends)
    }

    @Test
    fun blankNameUsesFriendNames() = runBlocking {
        val repo = RecordingRepo()
        CreateGroupRoomUseCase(repo)("   ", listOf(chaewon, jiwon)).getOrThrow()
        assertEquals("채원, 지원", repo.name)
    }

    @Test
    fun removesDuplicateFriends() = runBlocking {
        val repo = RecordingRepo()
        CreateGroupRoomUseCase(repo)("팀", listOf(chaewon, chaewon)).getOrThrow()
        assertEquals(listOf(chaewon), repo.friends)
    }

    @Test
    fun rejectsNoFriends() = runBlocking {
        val repo = RecordingRepo()
        val result = CreateGroupRoomUseCase(repo)("팀", emptyList())
        assertTrue(result.isFailure)
        assertNull(repo.name) // 저장소까지 가지 않는다
    }

    @Test
    fun longNamesAreCut() = runBlocking {
        val repo = RecordingRepo()
        CreateGroupRoomUseCase(repo)("가".repeat(50), listOf(chaewon)).getOrThrow()
        assertEquals(CreateGroupRoomUseCase.MAX_NAME_LENGTH, repo.name!!.length)

        val many = (1..20).map { Friend("$it", "김친구$it") }
        val auto = CreateGroupRoomUseCase.defaultName(many)
        assertEquals(CreateGroupRoomUseCase.MAX_NAME_LENGTH, auto.length)
        assertTrue(auto.endsWith("…"))
    }
}
