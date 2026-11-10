package com.moim.app.feature.auth.domain

/**
 * 회원가입 입력값 검사 규칙. 안드로이드에 의존하지 않는 순수 함수라 단위 테스트하기 쉽다.
 */
object SignUpValidator {
    private val studentIdRegex = Regex("^\\d{7}$")
    private val schoolEmailRegex = Regex("^[A-Za-z0-9._%+-]+@sookmyung\\.ac\\.kr$", RegexOption.IGNORE_CASE)
    private val emailCodeRegex = Regex("^\\d{6}$")

    /** 인증번호 유효 시간 (3분) */
    const val EMAIL_CODE_VALID_SECONDS = 180

    /** 학번: 숫자 7자리 */
    fun isValidStudentId(studentId: String): Boolean = studentIdRegex.matches(studentId)

    /** 비밀번호: 8자 이상, 영문과 숫자를 각각 하나 이상 포함 */
    fun isValidPassword(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isLetter() && it.code < 128 } &&
            password.any { it.isDigit() }

    fun isValidName(name: String): Boolean = name.isNotBlank()

    /** 학교 웹메일: 아이디@sookmyung.ac.kr */
    fun isValidSchoolEmail(email: String): Boolean = schoolEmailRegex.matches(email.trim())

    /** 인증번호: 숫자 6자리 */
    fun isValidEmailCode(code: String): Boolean = emailCodeRegex.matches(code)
}
