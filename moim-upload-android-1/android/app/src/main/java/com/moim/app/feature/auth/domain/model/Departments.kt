package com.moim.app.feature.auth.domain.model

/**
 * 학부 → 학과/전공 목록.
 * TODO: Stitch 디자인에 있던 예시 목록이라, 숙명여대 실제 학부·전공 목록으로 바꿔야 한다.
 */
object Departments {
    val byCollege: Map<String, List<String>> = linkedMapOf(
        "소프트웨어학부" to listOf("컴퓨터과학전공", "인공지능공학전공", "데이터사이언스전공"),
        "공과대학" to listOf("전자공학전공", "기계시스템학부", "화공생명공학부"),
        "이과대학" to listOf("화학과", "생명시스템학부", "수학과", "통계학과"),
        "문과대학" to listOf("한국어문학부", "역사문화학과", "문헌정보학과"),
    )

    val colleges: List<String> get() = byCollege.keys.toList()

    fun majorsOf(college: String?): List<String> = byCollege[college].orEmpty()
}
