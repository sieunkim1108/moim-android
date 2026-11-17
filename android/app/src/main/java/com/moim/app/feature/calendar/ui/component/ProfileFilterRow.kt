package com.moim.app.feature.calendar.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.Friend
import com.moim.app.core.model.givenName
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelPink
import com.moim.app.ui.theme.PastelYellow
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.SurfaceContainerHigh
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White

/** 프로필 사진이 생기기 전까지 쓰는 동그라미 배경색 (순서대로 돌려 씀) */
private val AvatarColors = listOf(PastelPink, PastelMint, PastelYellow, SecondaryContainer)

/**
 * 캘린더 위 프로필 줄: [나] [친구들...] [친구 추가]
 * 누른 사람의 캘린더가 아래에 보인다.
 */
@Composable
fun ProfileFilterRow(
    myName: String,
    friends: List<Friend>,
    selectedFriendId: String?,
    onSelect: (String?) -> Unit,
    onAddFriendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = MoimDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceMd),
    ) {
        item(key = "me") {
            ProfileItem(
                label = stringResource(R.string.calendar_me),
                initial = myName.lastOrNull()?.toString().orEmpty(),
                avatarColor = SecondaryContainer,
                selected = selectedFriendId == null,
                onClick = { onSelect(null) },
            )
        }
        items(friends, key = { it.id }) { friend ->
            val index = friends.indexOf(friend)
            ProfileItem(
                label = friend.givenName,
                initial = friend.givenName.take(1),
                avatarColor = AvatarColors[index % AvatarColors.size],
                selected = selectedFriendId == friend.id,
                onClick = { onSelect(friend.id) },
            )
        }
        item(key = "add") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onAddFriendClick),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SecondaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Navy)
                }
                Text(
                    text = stringResource(R.string.calendar_add_friend),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ProfileItem(
    label: String,
    initial: String,
    avatarColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(if (selected) 2.5.dp else 2.dp, if (selected) Navy else SurfaceContainerHigh, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.calendar_selected),
                        tint = White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Navy else TextSecondary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF)
@Composable
private fun ProfileFilterRowPreview() {
    MoimTheme {
        ProfileFilterRow(
            myName = "김눈송",
            friends = listOf(Friend("1", "박시은"), Friend("2", "이지원")),
            selectedFriendId = null,
            onSelect = {},
            onAddFriendClick = {},
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
