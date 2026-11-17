package com.moim.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.SkyBlue
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.White

/**
 * 앱 공통 상단바 (하단 탭바 대신 사용).
 * 왼쪽: 캘린더 / 가운데: 제목 / 오른쪽: 단체 DM + 프로필
 * 지금 화면으로 가는 버튼은 null을 넘기면 눌러도 아무 일 없음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoimAppTopBar(
    title: @Composable () -> Unit,
    onCalendarClick: (() -> Unit)?,
    onDmClick: (() -> Unit)?,
    onProfileClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onCalendarClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.main_open_calendar),
                    tint = Navy,
                )
            }
        },
        title = title,
        actions = {
            IconButton(onClick = { onDmClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Outlined.Forum,
                    contentDescription = stringResource(R.string.main_open_dm),
                    tint = Navy,
                )
            }
            // 프로필 동그라미 (사진 기능은 프로필 단계에서)
            IconButton(onClick = { onProfileClick?.invoke() }) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.main_open_profile),
                        tint = Navy,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SkyBlue),
    )
}

/** 메인 화면 제목: 눈송이 + moim 로고 */
@Composable
fun MoimLogoTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.img_nunsongi),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = NavyDeep,
        )
    }
}

/** 다른 화면 제목 (예: Calendar) */
@Composable
fun MoimTopBarTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = NavyDeep,
    )
}

/**
 * 하위 화면 상단바: [←] 제목 ... (추가 아이콘) [프로필 동그라미]
 * 친구 추가(S-12), 프로필 수정(S-11)처럼 메인에서 한 단계 들어간 화면에 쓴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoimBackTopBar(
    title: String,
    onBack: () -> Unit,
    onProfileClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Navy)
            }
        },
        title = { MoimTopBarTitle(title) },
        actions = {
            actions()
            if (onProfileClick != null) {
                IconButton(
                    onClick = onProfileClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Navy, contentColor = White),
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.main_open_profile))
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Surface),
    )
}

@Preview(showBackground = true)
@Composable
private fun MoimBackTopBarPreview() {
    MoimTheme {
        MoimBackTopBar(title = "Add Friend", onBack = {}, onProfileClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MoimAppTopBarPreview() {
    MoimTheme {
        MoimAppTopBar(title = { MoimLogoTitle() }, onCalendarClick = {}, onDmClick = {}, onProfileClick = {})
    }
}
