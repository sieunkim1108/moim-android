package com.moim.app.feature.chat.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.feature.chat.domain.model.QuickReply
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.SkyBlueLight
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- 날짜·시간 표시 형식 ----------

private val DateChipFormat = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
private val BubbleTimeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

fun LocalDateTime.toBubbleTime(): String = format(BubbleTimeFormat)

/** 채팅 맨 위의 오늘 날짜 알약 (예: 11월 3일 화요일) */
@Composable
fun DateChip(date: LocalDate, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(White)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = Navy,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = date.format(DateChipFormat),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

/** 눈송이 프로필 동그라미 */
@Composable
fun BotAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MoimDimens.ChatAvatarSize)
            .shadow(2.dp, CircleShape, ambientColor = ShadowNavy, spotColor = ShadowNavy)
            .clip(CircleShape)
            .background(White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_nunsongi),
            contentDescription = null,
            modifier = Modifier.size(MoimDimens.ChatAvatarSize - 8.dp),
        )
    }
}

/**
 * 눈송이 줄: 왼쪽 프로필 + 이름 + 내용(말풍선이나 카드).
 * showHeader=false면 연속 메시지라 프로필·이름을 숨기고 들여쓰기만 맞춘다.
 */
@Composable
fun BotMessageRow(
    showHeader: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
    ) {
        if (showHeader) BotAvatar() else Spacer(Modifier.size(MoimDimens.ChatAvatarSize))
        Column(verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceXs)) {
            if (showHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.main_bot_name),
                        style = MaterialTheme.typography.labelLarge,
                        color = Navy,
                    )
                    Icon(
                        imageVector = Icons.Outlined.AcUnit,
                        contentDescription = null,
                        tint = Navy,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            content()
        }
    }
}

/** 눈송이 말풍선 (흰색, 왼쪽 위 모서리가 꼬리) + 옆에 보낸 시각 */
@Composable
fun BotBubble(text: String, time: String?, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Surface(
            color = White,
            shape = RoundedCornerShape(
                topStart = MoimDimens.ChatBubbleTail,
                topEnd = MoimDimens.ChatBubbleRadius,
                bottomEnd = MoimDimens.ChatBubbleRadius,
                bottomStart = MoimDimens.ChatBubbleRadius,
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = MoimDimens.ChatBubbleMaxWidth),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        if (time != null) BubbleTime(time, Modifier.padding(start = 6.dp))
    }
}

/** 내 말풍선 (남색, 오른쪽 정렬, 오른쪽 위 모서리가 꼬리) + 왼쪽에 보낸 시각 */
@Composable
fun UserBubble(text: String, time: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        BubbleTime(time, Modifier.padding(end = 6.dp))
        Surface(
            color = Navy,
            shape = RoundedCornerShape(
                topStart = MoimDimens.ChatBubbleRadius,
                topEnd = MoimDimens.ChatBubbleTail,
                bottomEnd = MoimDimens.ChatBubbleRadius,
                bottomStart = MoimDimens.ChatBubbleRadius,
            ),
            modifier = Modifier.widthIn(max = MoimDimens.ChatBubbleMaxWidth),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun BubbleTime(time: String, modifier: Modifier = Modifier) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = modifier,
    )
}

/** 눈송이가 답을 준비하는 동안 보이는 말풍선 */
@Composable
fun TypingBubble(modifier: Modifier = Modifier) {
    BotBubble(text = stringResource(R.string.main_typing), time = null, modifier = modifier)
}

/** 빠른 답장 칩 묶음 (흰 알약 버튼). 누르면 그 문장이 내 메시지로 보내진다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickReplyRow(
    replies: List<QuickReply>,
    onClick: (QuickReply) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
    ) {
        replies.forEach { reply ->
            Text(
                text = reply.label,
                style = MaterialTheme.typography.labelLarge,
                color = Navy,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(White)
                    .border(1.dp, OutlineVariant, CircleShape)
                    .clickable { onClick(reply) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDF5FF)
@Composable
private fun ChatBubblesPreview() {
    MoimTheme {
        Column(
            modifier = Modifier
                .background(SkyBlueLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateChip(LocalDate.of(2026, 11, 3))
            BotMessageRow(showHeader = true) {
                BotBubble("안녕 채원아! 오늘 일정은 2개야 ☀️", "오전 9:00")
                QuickReplyRow(
                    replies = listOf(QuickReply("오늘 일정 알려줘"), QuickReply("내일 3시에 팀플 회의 있어")),
                    onClick = {},
                )
            }
            UserBubble("다음 주 월요일 10시에 데이터 분석 시험이야ㅠㅠ", "오전 9:01")
            BotMessageRow(showHeader = true) { TypingBubble() }
        }
    }
}
