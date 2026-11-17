package com.moim.app.feature.friend.ui

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.FriendSearchResult
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.StudentProfile
import com.moim.app.core.model.Visibility
import com.moim.app.core.model.admissionYear
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.ui.component.InitialAvatar
import com.moim.app.ui.component.MoimBackTopBar
import com.moim.app.ui.component.MoimTextField
import com.moim.app.ui.theme.InfoBanner
import com.moim.app.ui.theme.InfoBannerBorder
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.PastelPink
import com.moim.app.ui.theme.PastelPinkText
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.SurfaceContainer
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

/**
 * S-12 친구 추가 (보고서 화면 설계서 기준)
 * 안내 배너 / 내 프로필 카드 / 학번으로 친구 찾기 / 검색 결과 / 받은 친구 요청
 */
@Composable
fun AddFriendRoute(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddFriendScreen(
        uiState = uiState,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onSearch = viewModel::onSearch,
        onSendRequest = viewModel::onSendRequest,
        onAccept = viewModel::onAccept,
        onDecline = viewModel::onDecline,
        onToggleShowAll = viewModel::onToggleShowAll,
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun AddFriendScreen(
    uiState: AddFriendUiState,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    onSendRequest: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val copiedText = stringResource(R.string.friend_copied)
    val qrSoon = stringResource(R.string.friend_qr_soon)
    val clipLabel = stringResource(R.string.friend_my_student_id)

    val messageText = uiState.message?.let {
        stringResource(
            when (it) {
                AddFriendMessage.INVALID_ID -> R.string.friend_invalid_id
                AddFriendMessage.REQUEST_SENT -> R.string.friend_request_sent
                AddFriendMessage.ACCEPTED -> R.string.friend_accepted
                AddFriendMessage.DECLINED -> R.string.friend_declined
                AddFriendMessage.FAILED -> R.string.error_unknown
            },
        )
    }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            onMessageShown()
            snackbarHostState.showSnackbar(messageText)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoimBackTopBar(
                title = stringResource(R.string.friend_title),
                onBack = onBack,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NetworkBanner()

            uiState.me?.let { me ->
                MyProfileCard(
                    me = me,
                    onCopy = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, me.studentId)))
                            snackbarHostState.showSnackbar(copiedText)
                        }
                    },
                    onQrClick = { scope.launch { snackbarHostState.showSnackbar(qrSoon) } },
                )
            }

            SearchSection(
                query = uiState.query,
                isSearching = uiState.isSearching,
                onQueryChange = onQueryChange,
                onClear = onClearQuery,
                onSearch = onSearch,
            )

            if (uiState.hasSearched) {
                SearchResultSection(
                    result = uiState.result,
                    isSending = uiState.isSending,
                    onSendRequest = onSendRequest,
                )
            }

            RequestsSection(
                uiState = uiState,
                onAccept = onAccept,
                onDecline = onDecline,
                onToggleShowAll = onToggleShowAll,
            )

            Spacer(Modifier.height(MoimDimens.SpaceLg))
        }
    }
}

/** "숙명인 연결 네트워크" 안내 배너 + 눈송이 */
@Composable
private fun NetworkBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(InfoBanner)
            .border(1.dp, InfoBannerBorder, RoundedCornerShape(MoimDimens.CardRadius))
            .padding(MoimDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Hub, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.friend_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                )
            }
            Text(
                text = stringResource(R.string.friend_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.width(MoimDimens.SpaceSm))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.img_nunsongi),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

/** 내 프로필 카드: 학번 · 이름(전공) + [학번 복사] + QR(향후) */
@Composable
private fun MyProfileCard(
    me: UserProfile,
    onCopy: () -> Unit,
    onQrClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .padding(MoimDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SecondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Badge, contentDescription = null, tint = Navy)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.friend_my_card), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = stringResource(R.string.friend_my_card_value, me.studentId, me.name, me.major),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainer)
                .clickable(onClick = onCopy)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.friend_copy_id), style = MaterialTheme.typography.labelMedium, color = Navy)
        }
        IconButton(onClick = onQrClick) {
            Icon(Icons.Outlined.QrCode2, contentDescription = stringResource(R.string.friend_qr), tint = Navy)
        }
    }
}

/** 학번으로 친구 찾기: 입력칸(지우기 버튼) + [검색] */
@Composable
private fun SearchSection(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.friend_search_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoimTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.friend_search_placeholder),
                leadingIcon = Icons.Outlined.PersonAdd,
                trailingContent = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.common_clear), tint = TextSecondary)
                        }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(MoimDimens.SpaceSm))
            Button(
                onClick = onSearch,
                enabled = query.isNotEmpty() && !isSearching,
                modifier = Modifier.height(MoimDimens.ButtonHeight),
                shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            ) {
                if (isSearching) {
                    CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.friend_search), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** 검색 결과 1건 또는 "결과 없음" */
@Composable
private fun SearchResultSection(
    result: FriendSearchResult?,
    isSending: Boolean,
    onSendRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.friend_result_count, if (result == null) 0 else 1),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (result != null) {
                Text(stringResource(R.string.friend_result_match), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
        if (result == null) {
            Text(
                text = stringResource(R.string.friend_result_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MoimDimens.CardRadius))
                    .background(White)
                    .padding(MoimDimens.SpaceMd),
            )
        } else {
            SearchResultCard(result = result, isSending = isSending, onSendRequest = onSendRequest)
        }
    }
}

@Composable
private fun SearchResultCard(
    result: FriendSearchResult,
    isSending: Boolean,
    onSendRequest: () -> Unit,
) {
    val profile = result.profile
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .padding(MoimDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(name = profile.name, id = profile.id, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = profile.studentId,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.friend_major_year, profile.major, profile.admissionYear),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    if (profile.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.friend_verified),
                            tint = Navy,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            RelationButton(relation = result.relation, isSending = isSending, onSendRequest = onSendRequest)
        }
        if (result.commonRoomNames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.friend_common_rooms,
                        result.commonRoomNames.size,
                        result.commonRoomNames.joinToString(", "),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Navy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 관계에 따라: [친구 요청] / 요청됨 / 이미 친구 / 받은 요청 확인 / 나 */
@Composable
private fun RelationButton(
    relation: FriendRelation,
    isSending: Boolean,
    onSendRequest: () -> Unit,
) {
    if (relation == FriendRelation.NONE) {
        Button(
            onClick = onSendRequest,
            enabled = !isSending,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
        ) {
            if (isSending) {
                CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.friend_request), style = MaterialTheme.typography.labelLarge)
            }
        }
    } else {
        val label = when (relation) {
            FriendRelation.REQUESTED -> R.string.friend_requested
            FriendRelation.FRIEND -> R.string.friend_already
            FriendRelation.INCOMING -> R.string.friend_incoming
            FriendRelation.ME -> R.string.friend_me
            FriendRelation.NONE -> R.string.friend_request // 위에서 처리됨
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (relation == FriendRelation.REQUESTED || relation == FriendRelation.FRIEND) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

/** 받은 친구 요청 + 개수 배지 + [전체보기] */
@Composable
private fun RequestsSection(
    uiState: AddFriendUiState,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onToggleShowAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.friend_requests_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            if (uiState.requests.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = uiState.requests.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PastelPinkText,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PastelPink)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (uiState.requests.size > AddFriendUiState.COLLAPSED_REQUEST_COUNT) {
                TextButton(onClick = onToggleShowAll) {
                    Text(
                        text = stringResource(if (uiState.showAllRequests) R.string.friend_fold else R.string.friend_view_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
        Text(stringResource(R.string.friend_requests_sub), style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        if (uiState.requests.isEmpty()) {
            Text(
                text = stringResource(R.string.friend_requests_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MoimDimens.CardRadius))
                    .background(White)
                    .padding(MoimDimens.SpaceMd),
            )
        } else {
            uiState.visibleRequests.forEach { request ->
                RequestCard(
                    request = request,
                    timeLabel = relativeTimeLabel(request.requestedAt, uiState.now),
                    onAccept = { onAccept(request.id) },
                    onDecline = { onDecline(request.id) },
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendRequest,
    timeLabel: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val from = request.from
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .padding(MoimDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(name = from.name, id = from.id, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(from.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(
                    text = stringResource(R.string.friend_major_year, from.major, from.admissionYear),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            ) {
                Text(stringResource(R.string.friend_accept), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = onDecline,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainer, contentColor = TextSecondary),
            ) {
                Text(stringResource(R.string.friend_decline), style = MaterialTheme.typography.labelLarge)
            }
        }
        val (icon, line) = when {
            request.message != null ->
                Icons.Outlined.ChatBubbleOutline to stringResource(R.string.friend_request_message, request.message)
            request.viaRoomName != null ->
                Icons.Outlined.Groups to stringResource(R.string.friend_request_via, request.viaRoomName)
            else -> null to null
        }
        if (icon != null && line != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(line, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
            }
        }
    }
}

/** "방금", "3시간 전", "어제", "3일 전" */
@Composable
private fun relativeTimeLabel(time: LocalDateTime, now: LocalDateTime): String {
    val days = Duration.between(time.toLocalDate().atStartOfDay(), now.toLocalDate().atStartOfDay()).toDays()
    val hours = Duration.between(time, now).toHours()
    return when {
        days >= 2 -> stringResource(R.string.friend_days_ago, days.toInt())
        days == 1L -> stringResource(R.string.friend_yesterday)
        hours >= 1 -> stringResource(R.string.friend_hours_ago, hours.toInt())
        else -> stringResource(R.string.friend_just_now)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun AddFriendScreenPreview() {
    val now = LocalDateTime.of(2026, 11, 5, 14, 0)
    val minseo = StudentProfile("kms", "김민서", "2314582", "컴퓨터과학전공")
    MoimTheme {
        AddFriendScreen(
            uiState = AddFriendUiState(
                now = now,
                me = UserProfile(
                    id = "me", studentId = "2316916", name = "황채원", college = "소프트웨어학부",
                    major = "소프트웨어학부", planningType = PlanningType.EARLY, defaultVisibility = Visibility.FRIENDS,
                ),
                query = "2314582",
                hasSearched = true,
                result = FriendSearchResult(minseo, FriendRelation.NONE, listOf("데분 팀플방", "SW 동아리")),
                requests = listOf(
                    FriendRequest(
                        "r1", StudentProfile("ljw", "이지원", "2418820", "컴퓨터과학전공"),
                        now.minusDays(1), message = "빅데이터분석론 같은 조 화이팅해요!",
                    ),
                    FriendRequest(
                        "r2", StudentProfile("ksu", "김시은", "2412345", "데이터사이언스전공"),
                        now.minusDays(3), viaRoomName = "눈송이 해커톤 24기",
                    ),
                ),
            ),
            onBack = {},
            onProfileClick = {},
            onQueryChange = {},
            onClearQuery = {},
            onSearch = {},
            onSendRequest = {},
            onAccept = {},
            onDecline = {},
            onToggleShowAll = {},
            onMessageShown = {},
        )
    }
}
