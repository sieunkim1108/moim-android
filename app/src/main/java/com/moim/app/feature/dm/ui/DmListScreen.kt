package com.moim.app.feature.dm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMember
import com.moim.app.feature.dm.domain.model.RoomType
import com.moim.app.feature.dm.ui.component.RoomListItem
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun DmListRoute(
    onBack: () -> Unit,
    onRoomClick: (ChatRoom) -> Unit,
    onProfileClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    viewModel: DmListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DmListScreen(
        uiState = uiState,
        onBack = onBack,
        onRoomClick = onRoomClick,
        onProfileClick = onProfileClick,
        onAddFriendClick = onAddFriendClick,
        onQueryChange = viewModel::onQueryChange,
        onFilterSelect = viewModel::onFilterSelect,
        onToggleUnread = viewModel::onToggleUnread,
    )
}

/**
 * 8. 채팅방 목록 (Stitch: moim_단톡방 목록)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmListScreen(
    uiState: DmListUiState,
    onBack: () -> Unit,
    onRoomClick: (ChatRoom) -> Unit,
    onProfileClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelect: (RoomFilter) -> Unit,
    onToggleUnread: () -> Unit,
    modifier: Modifier = Modifier,
    onAddFriendClick: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.main_coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Navy)
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.dm_list_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyDeep,
                    )
                },
                actions = {
                    IconButton(onClick = onAddFriendClick) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = stringResource(R.string.dm_add_friend), tint = Navy)
                    }
                    IconButton(onClick = showComingSoon) { // TODO: 새 채팅 만들기
                        Icon(Icons.Outlined.EditNote, contentDescription = stringResource(R.string.dm_new_chat), tint = Navy)
                    }
                    IconButton(
                        onClick = onProfileClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Navy, contentColor = White),
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.main_open_profile))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = showComingSoon, containerColor = Navy, contentColor = White, shape = CircleShape) {
                Icon(Icons.Outlined.PostAdd, contentDescription = stringResource(R.string.dm_new_chat))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = MoimDimens.ScreenPadding, end = MoimDimens.ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "network") { NetworkStrip(uiState.activeVotes) }
            item(key = "search") { SearchField(uiState.query, onQueryChange) }
            item(key = "filters") {
                FilterRow(
                    uiState = uiState,
                    onFilterSelect = onFilterSelect,
                    onToggleUnread = onToggleUnread,
                )
            }
            if (uiState.rooms.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.dm_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = MoimDimens.SpaceXl).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(uiState.rooms, key = { it.id }) { room ->
                RoomListItem(room = room, today = uiState.today, onClick = { onRoomClick(room) })
            }
        }
    }
}

/** ● 눈송이 캠퍼스 네트워크 ........ 진행 중인 모임 투표 2건 */
@Composable
private fun NetworkStrip(activeVotes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(White)
            .padding(horizontal = MoimDimens.SpaceMd, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Outline),
        )
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.dm_network), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        if (activeVotes > 0) {
            Text(
                text = stringResource(R.string.dm_active_votes, activeVotes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Navy,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, CircleShape, ambientColor = ShadowNavy, spotColor = ShadowNavy)
            .clip(CircleShape)
            .background(White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = TextPrimary)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.dm_search_placeholder), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Navy),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** [전체 6] [1:1 3] [단체 3] ........ ✓✓안읽음 */
@Composable
private fun FilterRow(
    uiState: DmListUiState,
    onFilterSelect: (RoomFilter) -> Unit,
    onToggleUnread: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            RoomFilter.ALL to R.string.dm_filter_all,
            RoomFilter.DIRECT to R.string.dm_filter_direct,
            RoomFilter.GROUP to R.string.dm_filter_group,
        ).forEach { (filter, label) ->
            val selected = uiState.filter == filter
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) Navy else White)
                    .clickable { onFilterSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) White else TextSecondary,
                )
                Text(
                    text = uiState.count(filter).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) White else TextSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) White.copy(alpha = 0.25f) else SecondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (uiState.unreadOnly) SecondaryContainer else White)
                .clickable(onClick = onToggleUnread)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.DoneAll, contentDescription = null, tint = if (uiState.unreadOnly) Navy else TextSecondary, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.dm_filter_unread),
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.unreadOnly) Navy else TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DmListScreenPreview() {
    val now = LocalDateTime.of(2026, 11, 3, 15, 12)
    val me = RoomMember("me", "나", true)
    MoimTheme {
        DmListScreen(
            uiState = DmListUiState(
                today = LocalDate.of(2026, 11, 3),
                allRooms = listOf(
                    ChatRoom(
                        "1", RoomType.GROUP, "SW 모바일 팀플방", members = listOf(me, RoomMember("a", "김시은"), RoomMember("b", "박지원")),
                        lastSenderName = "시은", lastMessage = "이번주 목요일 7시 회의 가능한 사람 투표해 줘!",
                        lastAt = now, unreadCount = 3, hasActiveVote = true,
                    ),
                    ChatRoom("2", RoomType.BOT, "AI 눈송이", members = listOf(me), lastMessage = "내일 과제 마감 1일 전이야!", lastAt = now.minusHours(2), unreadCount = 1),
                    ChatRoom(
                        "3", RoomType.DIRECT, "김시은", subtitle = "소프트웨어학부", members = listOf(me, RoomMember("a", "김시은")),
                        lastMessage = "오늘 학생식당 순헌관 앞에서 만나는 거 맞지?", lastAt = now.minusDays(1),
                    ),
                ),
            ),
            onBack = {},
            onRoomClick = {},
            onProfileClick = {},
            onQueryChange = {},
            onFilterSelect = {},
            onToggleUnread = {},
        )
    }
}
