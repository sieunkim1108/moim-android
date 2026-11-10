package com.moim.app.feature.dm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMember
import com.moim.app.feature.dm.domain.model.RoomType
import com.moim.app.ui.component.InitialAvatar
import com.moim.app.ui.component.NunsongiAvatar
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TodayFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val DateFormat = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

/** 채팅 목록 카드 한 개 (Stitch: moim_단톡방 목록) */
@Composable
fun RoomListItem(
    room: ChatRoom,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBot = room.type == RoomType.BOT
    Surface(
        color = White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(MoimDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoomAvatar(room)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 제목 줄
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    when {
                        room.type == RoomType.GROUP -> Text(
                            text = room.members.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        isBot -> SmallBadge(Icons.Outlined.Verified, stringResource(R.string.dm_bot_badge))
                        room.subtitle != null -> Text(
                            text = room.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = room.lastAt.toListTime(today),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                if (room.hasActiveVote) SmallBadge(Icons.Outlined.HowToVote, stringResource(R.string.dm_vote_badge))
                // 마지막 메시지 줄
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            room.lastSenderName?.let {
                                withStyle(SpanStyle(color = Navy, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string.dm_sender_prefix, it))
                                }
                            }
                            append(room.lastMessage)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isBot) Navy else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (room.unreadCount > 0) {
                        Spacer(Modifier.width(MoimDimens.SpaceSm))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Navy),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = room.unreadCount.coerceAtMost(99).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = White,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 단체방은 두 명 얼굴을 겹쳐서, 눈송이는 반짝이 배지와 함께 */
@Composable
private fun RoomAvatar(room: ChatRoom) {
    Box(Modifier.size(52.dp)) {
        when (room.type) {
            RoomType.BOT -> {
                NunsongiAvatar(size = 52.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Navy)
                        .border(2.dp, White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = White, modifier = Modifier.size(11.dp))
                }
            }
            RoomType.DIRECT -> {
                val other = room.members.firstOrNull { !it.isMe } ?: room.members.first()
                InitialAvatar(other.name, other.id, size = 52.dp)
            }
            RoomType.GROUP -> {
                val others = room.members.filter { !it.isMe }.take(2)
                others.getOrNull(0)?.let { InitialAvatar(it.name, it.id, size = 36.dp) }
                others.getOrNull(1)?.let {
                    InitialAvatar(
                        it.name,
                        it.id,
                        size = 36.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .border(2.dp, White, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallBadge(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SecondaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Navy)
    }
}

/** 오늘이면 "오후 3:12", 어제면 "어제", 그 전이면 "3월 28일" */
@Composable
private fun LocalDateTime.toListTime(today: LocalDate): String {
    val date = toLocalDate()
    return when (date) {
        today -> format(TodayFormat)
        today.minusDays(1) -> stringResource(R.string.dm_yesterday)
        else -> format(DateFormat)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF)
@Composable
private fun RoomListItemPreview() {
    MoimTheme {
        val now = LocalDateTime.of(2026, 11, 3, 15, 12)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RoomListItem(
                room = ChatRoom(
                    id = "1", type = RoomType.GROUP, title = "SW 모바일 팀플방",
                    members = listOf(RoomMember("me", "나", true), RoomMember("a", "김시은"), RoomMember("b", "박지원")),
                    lastSenderName = "시은", lastMessage = "이번주 목요일 7시 회의 가능한 사람 투표해 줘!",
                    lastAt = now, unreadCount = 3, hasActiveVote = true,
                ),
                today = now.toLocalDate(),
                onClick = {},
            )
            RoomListItem(
                room = ChatRoom(
                    id = "2", type = RoomType.BOT, title = "AI 눈송이", members = emptyList(),
                    lastMessage = "내일 과제 마감 1일 전이야!", lastAt = now.minusHours(2), unreadCount = 1,
                ),
                today = now.toLocalDate(),
                onClick = {},
            )
        }
    }
}
