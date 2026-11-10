package com.moim.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * moim 디자인 시스템 색상 (Stitch DESIGN.md 기준).
 * 화면 코드에서는 Color(0xFF...)를 직접 쓰지 말고 반드시 여기 값을 가져다 쓴다.
 */

// 브랜드 기본 색
val Navy = Color(0xFF0B2F7A)        // 버튼, 내 말풍선, 강조
val NavyDeep = Color(0xFF001B53)    // 로고 글자 등 가장 진한 남색
val SkyBlue = Color(0xFFDCEEFF)     // 배경 캔버스
val SkyBlueLight = Color(0xFFEDF5FF) // 시작 화면 그라데이션 중간색
val White = Color(0xFFFFFFFF)       // 카드, 눈송이 말풍선

// 글자 색
val TextPrimary = Color(0xFF191C21)
val TextSecondary = Color(0xFF444651)
val Outline = Color(0xFF747682)
val OutlineVariant = Color(0xFFC4C6D3)
val Secondary = Color(0xFF4F606E)
val Surface = Color(0xFFF8F9FF)
val ErrorRed = Color(0xFFBA1A1A)

// 보조 파스텔 (포인트 전용)
val PastelPink = Color(0xFFFFD1DC)
val PastelMint = Color(0xFFC1F0DC)
val PastelYellow = Color(0xFFFFF2B2)
val PastelPinkText = Color(0xFF851D34)
val PastelMintText = Color(0xFF105436)
val PastelYellowText = Color(0xFF614E00)

// 일정 유형 색: 시험 = 분홍/빨강, 과제 = 노랑, 개인 약속 = 파랑, 모임 = 초록, 수업 = 회색 계열
val ScheduleExam = Color(0xFFE7BBC6)  // 캘린더 첫페이지 디자인: 연분홍
val ScheduleAssignment = Color(0xFFFBBF24) // 노랑
val ScheduleAppointment = Color(0xFF0B2F7A) // 남색
val ScheduleMeetup = Color(0xFF10B981)  // 초록
val ScheduleClass = Color(0xFF9AA3B5)

// 모임 가능 여부 색: 초록 = 전원 가능 / 노랑 = 일부 가능 / 빨강 = 어려움 / 회색 = 정보 없음
val AvailabilityAll = Color(0xFF6FCF97)
val AvailabilitySome = Color(0xFFF7D774)
val AvailabilityNone = Color(0xFFEB7A7A)
val AvailabilityUnknown = Color(0xFFD8DAE1)

// 면/테두리 보조 색 (Stitch surface-container 계열)
val SurfaceContainerLow = Color(0xFFF2F3FA)   // 성향 알림 행, 동의 박스
val SurfaceContainer = Color(0xFFECEDF5)      // 세그먼트 버튼 바탕
val SurfaceContainerHigh = Color(0xFFE6E8EF)  // 진행 바 트랙
val SecondaryContainer = Color(0xFFD3E5F6)    // 캘린더 오늘 날짜 배지, 친구 추가 동그라미
val SelectedCard = Color(0xFFD6E6F8)          // 선택된 성향 카드 배경
val InfoBanner = Color(0xFFE3F0FC)            // 알림 안내 배너
val InfoBannerBorder = Color(0xFFC9DFF5)
val ShadowNavy = Color(0x1A0B2F7A)            // 남색 빛 그림자 (rgba(11,47,122,0.1))
