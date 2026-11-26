package com.moim.app.feature.profile.domain.usecase

import com.moim.app.core.image.PhotoCompressor
import com.moim.app.feature.auth.domain.SignUpValidator
import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 프로필 수정 저장 (S-11).
 * 이름이 비면 막는다. 학번·학교 이메일은 ProfileUpdate에 아예 없어서 바꿀 수 없다.
 * 성향을 바꾸면 "이후 등록하는 일정부터" 새 알림 기본값이 적용된다 (이미 등록된 알림은 그대로).
 */
class UpdateProfileUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(update: ProfileUpdate): Result<UserProfile> {
        if (!SignUpValidator.isValidName(update.name)) {
            return Result.failure(IllegalArgumentException("name is blank"))
        }
        return repository.updateProfile(update.copy(name = update.name.trim()))
    }
}

/**
 * 프로필 사진 바꾸기.
 * 고른 사진을 512×512 JPEG로 줄인 다음 올린다 (원본 그대로 올리면 수 MB라 느리고 저장소 제한에 걸린다).
 */
class UpdateProfilePhotoUseCase @Inject constructor(
    private val compressor: PhotoCompressor,
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(photoUri: String): Result<UserProfile> {
        val jpeg = runCatching { compressor.toProfileJpeg(photoUri) }.getOrElse { return Result.failure(it) }
        return repository.updateProfilePhoto(jpeg)
    }
}
