package com.moim.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.White

/** 남색으로 꽉 찬 기본 버튼 (예: 로그인, 등록하기) */
@Composable
fun MoimPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(MoimDimens.ButtonHeight),
        shape = RoundedCornerShape(MoimDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 흰 배경 + 남색 테두리 버튼 (예: 회원가입) */
@Composable
fun MoimOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(MoimDimens.ButtonHeight),
        shape = RoundedCornerShape(MoimDimens.ButtonRadius),
        border = BorderStroke(2.dp, Navy),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = Navy),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun MoimButtonsPreview() {
    MoimTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MoimPrimaryButton(text = "로그인", onClick = {})
            MoimOutlinedButton(text = "회원가입", onClick = {}, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
