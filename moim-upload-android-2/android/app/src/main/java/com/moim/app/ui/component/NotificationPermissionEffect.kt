package com.moim.app.ui.component

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.moim.app.R
import com.moim.app.core.reminder.ReminderNotifications

/**
 * Android 13+에서 화면이 처음 열릴 때 알림 권한을 한 번 물어본다.
 * 거절하면 안내 문구만 띄우고 다시 조르지 않는다 (같은 화면이 다시 열려도 rememberSaveable로 한 번만).
 */
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(context, R.string.reminder_permission_denied, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        if (!asked && !ReminderNotifications.canPost(context)) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
