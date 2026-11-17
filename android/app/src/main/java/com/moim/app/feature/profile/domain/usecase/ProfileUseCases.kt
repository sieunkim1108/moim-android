package com.moim.app.feature.profile.domain.usecase

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
