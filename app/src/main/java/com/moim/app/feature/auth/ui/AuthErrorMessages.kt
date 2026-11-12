package com.moim.app.feature.auth.ui

import androidx.annotation.StringRes
import com.moim.app.R
import com.moim.app.feature.auth.domain.model.AuthException

/** 인증 에러 → 화면에 보여줄 문구(strings.xml id) */
@StringRes
fun Throwable.toAuthMessageRes(): Int = when (this) {
    AuthException.InvalidCredentials -> R.string.error_invalid_credentials
    AuthException.DuplicateStudentId -> R.string.error_duplicate_student_id
    AuthException.DuplicateEmail -> R.string.error_duplicate_email
    AuthException.EmailNotVerified -> R.string.signup1_email_need_verify
    AuthException.Network -> R.string.error_network
    else -> R.string.error_unknown
}
