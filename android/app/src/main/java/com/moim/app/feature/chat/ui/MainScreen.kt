package com.moim.app.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.model.CardStatus
import com.moim.app.feature.chat.domain.model.ChatMessage
import com.moim.app.feature.chat.domain.model.QuickReply
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import com.moim.app.feature.chat.ui.component.BotBubble
import com.moim.app.feature.chat.ui.component.BotMessageRow
import com.moim.app.feature.chat.ui.component.ChatInputBar
import com.moim.app.feature.chat.ui.component.DateChip
import com.moim.app.feature.chat.ui.component.QuickReplyRow
import com.moim.app.feature.chat.ui.component.ScheduleConfirmCard
import com.moim.app.feature.chat.ui.component.TypingBubble
import com.moim.app.feature.chat.ui.component.UserBubble
import com.moim.app.ui.component.MoimAppTopBar
import com.moim.app.ui.component.MoimLogoTitle
import com.moim.app.ui.component.NotificationPermissionEffect
import com.moim.app.feature.chat.ui.component.toBubbleTime
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.SkyBlueLight
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 메인 화면 진입점: ViewModel 상태를 받아 화면에 넘긴다 */
@Composable
fun MainRoute(
    onCalendarClick: () -> Unit,
    onDmClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationPermissionEffect() // 일정 알림을 보내려면 알림 권한이 필요하다
    MainScreen(
        uiState = uiState,
        onCalendarClick = onCalendarClick,
        onDmClick = onDmClick,
        onProfileClick = onProfileClick,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onQuickReply = { viewModel.onQuickReply(it.text) },
        onRegisterCard = viewModel::onRegisterCard,
        onEditCard = viewModel::onEditCard,
        onRegisterErrorShown = viewModel::onRegisterErrorShown,
    )
}

/**
 * 6. 메인: 눈송이 챗봇 화면.
 * 상단: 캘린더 / moim 로고 / 단체 DM + 프로필 (하단 탭바 없음)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onCalendarClick: () -> Unit,
    onDmClick: () -> Unit,
    onProfileClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReply: (QuickReply) -> Unit,
    onRegisterCard: (String) -> Unit,
    onEditCard: (String) -> Unit,
    onRegisterErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.main_coming_soon)
    val registerFailed = stringResource(R.string.main_register_failed)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    LaunchedEffect(uiState.showRegisterError) {
        if (uiState.showRegisterError) {
            snackbarHostState.showSnackbar(registerFailed)
            onRegisterErrorShown()
        }
    }

    // 새 메시지가 오면 맨 아래로 스크롤 (+1은 날짜 칩, 타이핑 말풍선 몫)
    val listState = rememberLazyListState()
    val itemCount = uiState.messages.size + 1 + if (uiState.isBotTyping) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    // 빠른 답장은 "마지막 메시지가 눈송이 말"이고 답 기다리는 중이 아닐 때만 보여 준다
    val lastBotWithReplies = (uiState.messages.lastOrNull() as? ChatMessage.Bot)
        ?.takeIf { it.quickReplies.isNotEmpty() && !uiState.isBotTyping }

    Scaffold(
        modifier = modifier,
        containerColor = SkyBlueLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoimAppTopBar(
                title = { MoimLogoTitle() },
                onCalendarClick = onCalendarClick,
                onDmClick = onDmClick,
                onProfileClick = onProfileClick,
            )
        },
        bottomBar = {
            ChatInputBar(
                text = uiState.input,
                onTextChange = onInputChange,
                onSend = onSend,
                onAttachClick = showComingSoon, // TODO: 시간표 캡처 이미지 인식 (5단계 이후)
                enabled = !uiState.isBotTyping,
                modifier = Modifier.imePadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "date") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DateChip(uiState.today)
                }
            }

            items(uiState.messages.size, key = { uiState.messages[it].id }) { index ->
                val message = uiState.messages[index]
                // 바로 앞도 눈송이 메시지면 프로필·이름을 생략 (카톡처럼 묶어서 보이기)
                val prev = uiState.messages.getOrNull(index - 1)
                val showHeader = prev == null || prev is ChatMessage.User

                when (message) {
                    is ChatMessage.User -> UserBubble(message.text, message.sentAt.toBubbleTime())

                    is ChatMessage.Bot -> BotMessageRow(showHeader = showHeader) {
                        BotBubble(message.text, message.sentAt.toBubbleTime())
                        if (message == lastBotWithReplies) {
                            QuickReplyRow(
                                replies = message.quickReplies,
                                onClick = onQuickReply,
                                modifier = Modifier.padding(top = MoimDimens.SpaceXs),
                            )
                        }
                    }

                    is ChatMessage.ScheduleCard -> BotMessageRow(showHeader = showHeader) {
                        ScheduleConfirmCard(
                            drafts = message.drafts,
                            status = message.status,
                            onRegister = { onRegisterCard(message.id) },
                            onEdit = { onEditCard(message.id) },
                        )
                    }
                }
            }

            if (uiState.isBotTyping) {
                item(key = "typing") {
                    val last = uiState.messages.lastOrNull()
                    BotMessageRow(showHeader = last == null || last is ChatMessage.User) { TypingBubble() }
                }
            }
        }
    }
}

// ---------- 미리보기 ----------

private fun previewState(): MainUiState {
    val today = LocalDate.of(2026, 11, 3)
    val t = today.atTime(9, 0)
    return MainUiState(
        today = today,
        messages = listOf(
            ChatMessage.Bot("1", t, "안녕 채원아! 오늘 일정은 2개야 ☀️"),
            ChatMessage.User("2", t.plusMinutes(1), "다음 주 월요일 10시에 데이터 분석 시험이야ㅠㅠ 과제도 금요일까지고"),
            ChatMessage.Bot("3", t.plusMinutes(1), "헉 한꺼번에 몰렸구나, 힘내자! 💙\n이렇게 등록해 둘게"),
            ChatMessage.ScheduleCard(
                "4",
                t.plusMinutes(1),
                listOf(
                    ScheduleDraft("a", "데이터 분석 시험", ScheduleType.EXAM, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0)),
                    ScheduleDraft("b", "과제 제출", ScheduleType.ASSIGNMENT, LocalDate.of(2026, 11, 6), LocalTime.of(23, 59), true),
                ),
                CardStatus.REGISTERED,
            ),
            ChatMessage.Bot(
                "5",
                LocalDateTime.of(2026, 11, 3, 9, 2),
                "평소에 시험 3일 전부터 공부하는 편이지? 11/6(금)부터 공부 알림 보내줄까?",
                listOf(QuickReply("좋아! ⏰"), QuickReply("하루 전만"), QuickReply("괜찮아")),
            ),
        ),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenPreview() {
    MoimTheme {
        MainScreen(
            uiState = previewState(),
            onCalendarClick = {},
            onDmClick = {},
            onProfileClick = {},
            onInputChange = {},
            onSend = {},
            onQuickReply = {},
            onRegisterCard = {},
            onEditCard = {},
            onRegisterErrorShown = {},
        )
    }
}
