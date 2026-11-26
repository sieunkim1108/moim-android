package com.moim.app.feature.profile

import com.moim.app.core.image.PhotoCompressor
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.repository.AuthRepository
import com.moim.app.feature.profile.domain.usecase.UpdateProfilePhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateProfilePhotoUseCaseTest {

    private val me = UserProfile(
        id = "u1", studentId = "2312345", name = "김시은", college = "소프트웨어융합대학",
        major = "데이터사이언스전공", planningType = PlanningType.EARLY, defaultVisibility = Visibility.FRIENDS,
    )

    /** 압축 결과를 정해 두는 가짜 압축기 */
    private class FakeCompressor(private val result: Result<ByteArray>) : PhotoCompressor {
        var lastUri: String? = null
        override suspend fun toProfileJpeg(uri: String): ByteArray {
            lastUri = uri
            return result.getOrThrow()
        }
    }

    /** updateProfilePhoto에 무엇이 넘어왔는지만 기억하는 가짜 저장소 */
    private inner class RecordingAuthRepo : AuthRepository {
        var uploaded: ByteArray? = null
        override val currentUser: StateFlow<UserProfile?> = MutableStateFlow(me)

        override suspend fun updateProfilePhoto(jpeg: ByteArray): Result<UserProfile> {
            uploaded = jpeg
            return Result.success(me.copy(avatarUrl = "https://example.com/avatars/u1/1.jpg"))
        }

        override suspend fun isStudentIdAvailable(studentId: String) = Result.success(true)
        override suspend fun requestEmailCode(email: String) = Result.success(Unit)
        override suspend fun verifyEmailCode(email: String, code: String) = Result.success(true)
        override suspend fun signUp(form: SignUpForm) = Result.success(me)
        override suspend fun login(studentId: String, password: String, keepSignedIn: Boolean) = Result.success(me)
        override suspend fun restoreSession(): UserProfile? = me
        override suspend fun updateProfile(update: ProfileUpdate) = Result.success(me)
        override suspend fun logout() = Unit
    }

    @Test
    fun compressesThenUploads() = runBlocking {
        val jpeg = byteArrayOf(1, 2, 3)
        val compressor = FakeCompressor(Result.success(jpeg))
        val repo = RecordingAuthRepo()

        val result = UpdateProfilePhotoUseCase(compressor, repo)("content://photo/1").getOrThrow()

        assertEquals("content://photo/1", compressor.lastUri)
        assertArrayEquals(jpeg, repo.uploaded) // 압축한 바이트가 그대로 올라간다
        assertEquals("https://example.com/avatars/u1/1.jpg", result.avatarUrl)
    }

    @Test
    fun stopsWhenPhotoCannotBeRead() = runBlocking {
        val compressor = FakeCompressor(Result.failure(IllegalArgumentException("사진을 읽지 못했어요")))
        val repo = RecordingAuthRepo()

        val result = UpdateProfilePhotoUseCase(compressor, repo)("content://broken")

        assertTrue(result.isFailure)
        assertNull(repo.uploaded) // 읽기에 실패하면 서버에 아무것도 안 올린다
    }
}
