package com.moim.app.feature.dm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMember
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.model.RoomType
import com.moim.app.feature.dm.ui.component.MeetupSuggestionCard
import com.moim.app.ui.component.InitialAvatar
import com.moim.app.ui.component.NunsongiAvatar
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.SkyBlueLight
import com.moim.app.ui.theme.SurfaceContainer
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val DateChipFormat = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN)

@Composable
fun DmRoomRoute(
    onBack: () -> Unit,
    onMeetupClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: DmRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DmRoomScreen(
        uiState = uiState,
        onBack = onBack,
        onMeetupClick = { onMeetupClick(viewModel.roomId) },
        onProfileClick = onProfileClick,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onSuggestionSelect = viewModel::onSuggestionSelect,
    )
}

/**
 * 9. 단체 채팅방 (Stitch: moim_단체채팅방)
 * 상단 [모임] 버튼 → 모임 일정 잡기. 모임 얘기가 나오면 눈송이가 추천 시간 카드를 띄운다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmRoomScreen(
    uiState: DmRoomUiState,
    onBack: () -> Unit,
    onMeetupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = uiState.room
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.main_coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    val listState = rememberLazyListState()
    val itemCount = uiState.messages.size + 1
    LaunchedEffect(itemCount) { listState.animateScrollToItem(itemCount - 1) }

    // 추천 카드가 여러 개면 마지막 것만 보여 준다 (예전 카드는 같은 내용이라)
    val lastSuggestionId = uiState.messages.lastOrNull { it is RoomMessage.MeetupSuggestion }?.id

    Scaffold(
        modifier = modifier,
        containerColor = SkyBlueLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = room?.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep,
                        )
                        if (room != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Navy),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.room_member_count, room.members.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (room?.type == RoomType.GROUP) {
                        Button(
                            onClick = onMeetupClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.room_meetup_button), fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = onProfileClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Navy, contentColor = White),
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.main_open_profile))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White),
            )
        },
        bottomBar = {
            RoomInputBar(
                text = uiState.input,
                onTextChange = onInputChange,
                onSend = onSend,
                onAttach = showComingSoon,
                onEmoji = showComingSoon,
                modifier = Modifier.imePadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(MoimDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "date") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Text(uiState.today.format(DateChipFormat), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                }
            }
            items(uiState.messages.size, key = { uiState.messages[it].id }) { index ->
                when (val message = uiState.messages[index]) {
                    is RoomMessage.Text -> {
                        if (message.isMine) {
                            MyBubble(message.text, message.sentAt)
                        } else {
                            OtherBubble(message.senderId, message.senderName, message.text, message.sentAt)
                        }
                    }
                    is RoomMessage.Bot -> BotBubble(message.text, message.sentAt)
                    is RoomMessage.MeetupSuggestion -> {
                        if (message.id == lastSuggestionId) {
                            MeetupSuggestionCard(
                                memberCount = room?.members?.size ?: 0,
                                suggestions = uiState.suggestions,
                                selectedIndex = uiState.selectedSuggestion,
                                onSelect = onSuggestionSelect,
                                onMakeVote = showComingSoon, // TODO: 투표 기능
                                onViewCalendar = onMeetupClick,
                                modifier = Modifier.padding(start = 44.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherBubble(senderId: String, name: String, text: String, sentAt: LocalDateTime) {
    Row {
        InitialAvatar(name, senderId, size = 40.dp)
        Spacer(Modifier.width(MoimDimens.SpaceSm))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            BubbleWithTime(text, sentAt, mine = false)
        }
    }
}

@Composable
private fun BotBubble(text: String, sentAt: LocalDateTime) {
    Row {
        NunsongiAvatar(size = 40.dp)
        Spacer(Modifier.width(MoimDimens.SpaceSm))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.main_bot_name), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Navy, modifier = Modifier.size(12.dp))
                    Text(stringResource(R.string.room_bot_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Navy)
                }
            }
            BubbleWithTime(text, sentAt, mine = false)
        }
    }
}

@Composable
private fun MyBubble(text: String, sentAt: LocalDateTime) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        BubbleWithTime(text, sentAt, mine = true)
    }
}

@Composable
private fun BubbleWithTime(text: String, sentAt: LocalDateTime, mine: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        if (mine) {
            Text(sentAt.format(TimeFormat), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.width(6.dp))
        }
        Surface(
            color = if (mine) Navy else White,
            shape = RoundedCornerShape(
                topStart = if (mine) 22.dp else 6.dp,
                topEnd = if (mine) 6.dp else 22.dp,
                bottomEnd = 22.dp,
                bottomStart = 22.dp,
            ),
            shadowElevation = if (mine) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (mine) White else TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (!mine) {
            Spacer(Modifier.width(6.dp))
            Text(sentAt.format(TimeFormat), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

/** [+] [메시지 입력... 😊] [▶] */
@Composable
private fun RoomInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmoji: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SkyBlueLight)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
    ) {
        IconButton(
            onClick = onAttach,
            colors = IconButtonDefaults.iconButtonColors(containerColor = White, contentColor = TextSecondary),
            modifier = Modifier.size(48.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.room_attach)) }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .clip(CircleShape)
                .background(White)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(stringResource(R.string.room_input_placeholder), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Navy),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onEmoji) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = stringResource(R.string.room_emoji), tint = TextSecondary)
            }
        }
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Navy,
                contentColor = White,
                disabledContainerColor = Navy.copy(alpha = 0.4f),
                disabledContentColor = White,
            ),
            modifier = Modifier.size(52.dp),
        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.main_send)) }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DmRoomScreenPreview() {
    val now = LocalDateTime.of(2026, 11, 2, 14, 15)
    MoimTheme {
        DmRoomScreen(
            uiState = DmRoomUiState(
                today = LocalDate.of(2026, 11, 2),
                room = ChatRoom(
                    "r", RoomType.GROUP, "SW 모바일 팀플방",
                    members = listOf(RoomMember("me", "나", true), RoomMember("a", "김시은"), RoomMember("b", "박지원")),
                    lastMessage = "", lastAt = now,
                ),
                messages = listOf(
                    RoomMessage.Text("1", now, "a", "시은", "이번 달에 한번 놀자!! 🌸", false),
                    RoomMessage.Text("2", now.plusMinutes(1), "b", "지원", "좋아 언제 돼?", false),
                    RoomMessage.Text("3", now.plusMinutes(3), "me", "나", "나 시험 끝나고 아무 때나!", true),
                    RoomMessage.Bot("4", now.plusMinutes(3), "내가 셋 일정 비교해 볼게! 👀"),
                    RoomMessage.MeetupSuggestion("5", now.plusMinutes(3)),
                ),
                suggestions = emptyList(),
            ),
            onBack = {},
            onMeetupClick = {},
            onProfileClick = {},
            onInputChange = {},
            onSend = {},
            onSuggestionSelect = {},
        )
    }
}
