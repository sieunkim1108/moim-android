package com.moim.app.feature.auth.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 회원가입 입력 규칙 단위 테스트 (에뮬레이터 없이 실행됨) */
class SignUpValidatorTest {

    @Test
    fun studentId_onlySevenDigitsPass() {
        assertTrue(SignUpValidator.isValidStudentId("2316916"))
        assertFalse(SignUpValidator.isValidStudentId("231691"))
        assertFalse(SignUpValidator.isValidStudentId("23169160"))
        assertFalse(SignUpValidator.isValidStudentId("23169a6"))
    }

    @Test
    fun password_needsLetterDigitAndEightChars() {
        assertTrue(SignUpValidator.isValidPassword("moim1234"))
        assertFalse(SignUpValidator.isValidPassword("moim123"))   // 7자
        assertFalse(SignUpValidator.isValidPassword("12345678"))  // 영문 없음
        assertFalse(SignUpValidator.isValidPassword("moimmoim"))  // 숫자 없음
        assertFalse(SignUpValidator.isValidPassword("눈송이눈송이12")) // 한글은 영문으로 안 침
    }

    @Test
    fun schoolEmail_onlySookmyungWebmail() {
        assertTrue(SignUpValidator.isValidSchoolEmail("snowflake@sookmyung.ac.kr"))
        assertTrue(SignUpValidator.isValidSchoolEmail("Snow.Flake@SOOKMYUNG.AC.KR")) // 대소문자 무시
        assertFalse(SignUpValidator.isValidSchoolEmail("snowflake@gmail.com"))
        assertFalse(SignUpValidator.isValidSchoolEmail("snowflake@sookmyung.ac.kr.evil.com"))
        assertFalse(SignUpValidator.isValidSchoolEmail("@sookmyung.ac.kr"))
    }

    @Test
    fun emailCode_sixDigits() {
        assertTrue(SignUpValidator.isValidEmailCode("482910"))
        assertFalse(SignUpValidator.isValidEmailCode("48291"))
        assertFalse(SignUpValidator.isValidEmailCode("48291a"))
    }
}
