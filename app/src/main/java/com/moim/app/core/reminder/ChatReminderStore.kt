package com.moim.app.core.reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moim.app.core.model.ChatReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

/**
 * 눈송이 대화로 받아들인 준비 알림을 기기 안(DataStore)에 저장한다.
 * 일정 자체의 알림은 일정 데이터에서 다시 계산하므로 여기 저장하지 않는다.
 */
@Singleton
class ChatReminderStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observe(): Flow<List<ChatReminder>> = context.reminderDataStore.data.map { prefs ->
        decode(prefs[KEY])
    }

    /** 같은 제목·날짜 알림이 있으면 새 것으로 바꾸고, 이미 지난 알림은 정리한다 */
    suspend fun add(reminder: ChatReminder) {
        val today = LocalDate.now(clock)
        context.reminderDataStore.edit { prefs ->
            val kept = decode(prefs[KEY]).filter {
                it.eventDate.isAfter(today) && !(it.title == reminder.title && it.eventDate == reminder.eventDate)
            }
            prefs[KEY] = json.encodeToString((kept + reminder).map { it.toRow() })
        }
    }

    /** 로그아웃할 때 비운다 */
    suspend fun clear() {
        context.reminderDataStore.edit { it.remove(KEY) }
    }

    private fun decode(raw: String?): List<ChatReminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Row>>(raw).map { it.toModel() } }.getOrDefault(emptyList())
    }

    @Serializable
    private data class Row(val title: String, val eventDate: String, val from: String) {
        fun toModel() = ChatReminder(title, LocalDate.parse(eventDate), LocalDate.parse(from))
    }

    private fun ChatReminder.toRow() = Row(title, eventDate.toString(), from.toString())

    private companion object {
        val KEY = stringPreferencesKey("chat_reminders")
    }
}
