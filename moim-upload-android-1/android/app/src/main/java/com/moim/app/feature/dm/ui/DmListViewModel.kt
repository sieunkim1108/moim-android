package com.moim.app.feature.dm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomType
import com.moim.app.feature.dm.domain.usecase.ObserveRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class RoomFilter { ALL, DIRECT, GROUP }

data class DmListUiState(
    val today: LocalDate,
    val allRooms: List<ChatRoom> = emptyList(),
    val query: String = "",
    val filter: RoomFilter = RoomFilter.ALL,
    val unreadOnly: Boolean = false,
) {
    /** 1:1 탭에는 눈송이(AI) 방도 포함 */
    private fun ChatRoom.matches(f: RoomFilter) = when (f) {
        RoomFilter.ALL -> true
        RoomFilter.DIRECT -> type != RoomType.GROUP
        RoomFilter.GROUP -> type == RoomType.GROUP
    }

    fun count(f: RoomFilter): Int = allRooms.count { it.matches(f) }

    val activeVotes: Int get() = allRooms.count { it.hasActiveVote }

    val rooms: List<ChatRoom>
        get() = allRooms.filter { room ->
            room.matches(filter) &&
                (!unreadOnly || room.unreadCount > 0) &&
                (query.isBlank() || room.title.contains(query.trim()) || room.lastMessage.contains(query.trim()))
        }
}

private data class Filters(val query: String = "", val filter: RoomFilter = RoomFilter.ALL, val unreadOnly: Boolean = false)

@HiltViewModel
class DmListViewModel @Inject constructor(
    observeRooms: ObserveRoomsUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<DmListUiState> = combine(observeRooms(), filters) { rooms, f ->
        DmListUiState(today = today, allRooms = rooms, query = f.query, filter = f.filter, unreadOnly = f.unreadOnly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DmListUiState(today = today))

    fun onQueryChange(text: String) = filters.update { it.copy(query = text) }

    fun onFilterSelect(filter: RoomFilter) = filters.update { it.copy(filter = filter) }

    fun onToggleUnread() = filters.update { it.copy(unreadOnly = !it.unreadOnly) }
}
