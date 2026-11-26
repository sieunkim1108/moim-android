package com.moim.app.feature.dm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.White

/** 자주 쓰는 이모지 (대학생 단톡방에서 많이 쓰는 것 위주) */
val QuickEmojis = listOf(
    "😀", "😂", "🥹", "😊", "😍", "🥰", "😎", "🤔",
    "😅", "😭", "😡", "😴", "🤯", "🥳", "😇", "🙃",
    "👍", "👏", "🙏", "💪", "👌", "✌️", "🤝", "🙌",
    "❤️", "💙", "🩵", "💜", "✨", "🔥", "🎉", "❄️",
    "📚", "✏️", "💻", "📅", "⏰", "☕", "🍕", "🍻",
)

/**
 * 채팅 입력창 아래에 열리는 이모지 판.
 * 누르면 [onPick]으로 그 이모지를 알려 준다 (입력창에 넣는 건 화면이 한다).
 */
@Composable
fun EmojiPanel(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier
            .fillMaxWidth()
            .height(232.dp)
            .background(White),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(QuickEmojis) { emoji ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 26.sp)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun EmojiPanelPreview() {
    MoimTheme { EmojiPanel(onPick = {}) }
}
