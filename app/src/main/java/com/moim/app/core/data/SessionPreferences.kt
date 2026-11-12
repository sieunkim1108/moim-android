package com.moim.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/*
 * DataStore: 앱 안에 작은 설정값(키-값)을 저장하는 Jetpack 라이브러리.
 * SharedPreferences와 비슷하지만 코루틴/Flow로 읽고 써서 화면이 멈추지 않는다.
 * 여기에는 "자동 로그인 여부"와 "마지막 학번"만 저장한다. 비밀번호는 절대 저장하지 않는다.
 */
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** 저장된 로그인 기록 */
data class SavedSession(
    val studentId: String,
    val autoLogin: Boolean,
)

@Singleton
class SessionPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun read(): SavedSession? {
        val prefs = context.sessionDataStore.data.first()
        val studentId = prefs[KEY_STUDENT_ID] ?: return null
        return SavedSession(studentId = studentId, autoLogin = prefs[KEY_AUTO_LOGIN] ?: false)
    }

    suspend fun save(studentId: String, autoLogin: Boolean) {
        context.sessionDataStore.edit {
            it[KEY_STUDENT_ID] = studentId
            it[KEY_AUTO_LOGIN] = autoLogin
        }
    }

    /** 로그아웃: 자동 로그인 정보 삭제 */
    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_STUDENT_ID = stringPreferencesKey("student_id")
        val KEY_AUTO_LOGIN = booleanPreferencesKey("auto_login")
    }
}
