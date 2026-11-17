package com.moim.app.feature.auth.domain.model

/**
 * 학부 → 학과/전공 목록.
 * TODO: Stitch 디자인에 있던 예시 목록이라, 숙명여대 실제 학부·전공 목록으로 바꿔야 한다.
 */
object Departments {
    val byCollege: Map<String, List<String>> = linkedMapOf(
        "생활과학대학" to listOf(" "),
        "사회과학대학" to listOf(" "),
        "법과대학" to listOf(" "),
        "경상대학" to listOf(" "),
        "음악대학" to listOf(" "),
        "약학대학" to listOf(" "),
        "미술대학" to listOf(" "),
        "글로벌융합대학" to listOf(" "),
        "글로벌서비스학부" to listOf(" "),
        "영어영문학부" to listOf(" "),
        "미디어학부" to listOf(" "),
        "공과대학" to listOf(" "),
        "이과대학" to listOf(" ") ,
        "문과대학" to listOf(" "),
    )

    val colleges: List<String> get() = byCollege.keys.toList()

    fun majorsOf(college: String?): List<String> = byCollege[college].orEmpty()
}
