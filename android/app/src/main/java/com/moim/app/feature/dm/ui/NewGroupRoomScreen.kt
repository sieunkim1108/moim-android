package com.moim.app.feature.dm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.Friend
import com.moim.app.ui.component.ProfileAvatar
import com.moim.app.ui.component.MoimBackTopBar
import com.moim.app.ui.component.MoimOutlinedButton
import com.moim.app.ui.component.MoimPrimaryButton
import com.moim.app.ui.component.MoimTextField
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White

@Composable
/**ViewModel이랑 화면을 연결하는 입구**/
fun NewGroupRoomRoute(
    onBack: () -> Unit,
    onCreated: (roomId: String) -> Unit,
    onAddFriendClick: () -> Unit,
    viewModel: NewGroupRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 방이 만들어지면 그 방으로 한 번만 이동
    LaunchedEffect(uiState.createdRoomId) {
        uiState.createdRoomId?.let { roomId ->
            viewModel.onNavigated()
            onCreated(roomId)
        }
    }
/**실제 화면(ViewModel 모름)
 * 상태(uistate) 와 버튼 함수만 받아서 그리기만 함.
 * **/
    NewGroupRoomScreen(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onFriendToggle = viewModel::onFriendToggle,
        onCreate = viewModel::onCreate,
        onAddFriendClick = onAddFriendClick,
        onErrorShown = viewModel::onErrorShown,
    )
}

/**
 * 새 단체 채팅방 만들기.
 * 방 이름(선택) / 친구 고르기(체크) / [채팅방 만들기 (n명)]
 */
@Composable
/**Scaffold 로 화면 뼈대를 잡음. 본문은 LazyColumn임**/
fun NewGroupRoomScreen(
    uiState: NewGroupRoomUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onFriendToggle: (String) -> Unit,
    //버튼 눌렸을 때 실행할 함수를 받을게
    onCreate: () -> Unit,
    onAddFriendClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.new_room_failed)
    LaunchedEffect(uiState.showError) {
        if (uiState.showError) {
            onErrorShown()
            snackbarHostState.showSnackbar(errorText)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoimBackTopBar(title = stringResource(R.string.new_room_title), onBack = onBack, onProfileClick = null)
        },
        bottomBar = {
            if (uiState.friends.isNotEmpty()) {
                MoimPrimaryButton(
                    text = stringResource(R.string.new_room_create, uiState.selectedIds.size),
                    //이 버튼이 눌리면 onCreate 실행해라
                    onClick = onCreate,
                    enabled = uiState.canCreate,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(MoimDimens.ScreenPadding),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
        ) {
            item(key = "name") {
                MoimTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.new_room_name_label),
                    // 비워 두면 고른 친구 이름으로 지어진다
                    placeholder = uiState.defaultName.ifEmpty { stringResource(R.string.new_room_name_placeholder) },
                )
            }
            item(key = "friends-label") {
                Text(
                    text = stringResource(R.string.new_room_friends_label, uiState.selectedIds.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = MoimDimens.SpaceMd, bottom = MoimDimens.SpaceXs),
                )
            }

            if (!uiState.isLoading && uiState.friends.isEmpty()) {
                item(key = "empty") { NoFriends(onAddFriendClick) }
            } else {
                items(uiState.friends, key = { it.id }) { friend ->
                    FriendCheckRow(
                        friend = friend,
                        checked = friend.id in uiState.selectedIds,
                        onToggle = { onFriendToggle(friend.id) },
                    )
                }
            }
        }
    }
}

/** [아바타] 이름 / 전공 ........ [체크] (줄 전체를 눌러도 선택) */
@Composable
/**친구 한 줄**/
private fun FriendCheckRow(friend: Friend, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(if (checked) SecondaryContainer else White)
            .clickable(onClick = onToggle)
            .padding(horizontal = MoimDimens.SpaceMd, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(name = friend.name, id = friend.id, avatarUrl = friend.avatarUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            friend.major?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Navy, uncheckedColor = Outline),
        )
    }
}

/** 친구가 없으면 단체방을 만들 수 없으니 친구 추가로 안내 */
@Composable
private fun NoFriends(onAddFriendClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MoimDimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceMd),
    ) {
        Text(
            text = stringResource(R.string.new_room_no_friends),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        MoimOutlinedButton(text = stringResource(R.string.new_room_add_friend), onClick = onAddFriendClick)
    }
}

// ---------- 미리보기 ----------

@Preview(showBackground = true)
@Composable
private fun NewGroupRoomScreenPreview() {
    MoimTheme {
        NewGroupRoomScreen(
            uiState = NewGroupRoomUiState(
                friends = listOf(
                    Friend("1", "황채원", "소프트웨어학부"),
                    Friend("2", "손지원", "데이터사이언스전공"),
                ),
                isLoading = false,
                selectedIds = setOf("1"),
            ),
            onBack = {},
            onNameChange = {},
            onFriendToggle = {},
            onCreate = {},
            onAddFriendClick = {},
            onErrorShown = {},
        )
    }
}
