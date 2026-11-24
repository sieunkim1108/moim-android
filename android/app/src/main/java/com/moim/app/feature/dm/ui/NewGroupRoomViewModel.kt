package com.moim.app.feature.dm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.core.model.Friend
import com.moim.app.feature.dm.domain.usecase.CreateGroupRoomUseCase
import com.moim.app.feature.dm.domain.usecase.ObserveInvitableFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 새 단체방 만들기 화면 상태 */
data class NewGroupRoomUiState(
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = true,
    val name: String = "",
    val selectedIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val createdRoomId: String? = null, // 만들어지면 그 방으로 이동 (한 번만)
    val showError: Boolean = false,
) {
    val selectedFriends: List<Friend> get() = friends.filter { it.id in selectedIds }
    val canCreate: Boolean get() = selectedIds.isNotEmpty() && !isCreating

    /** 이름을 비워 두면 이렇게 지어진다는 안내 (placeholder) */
    val defaultName: String get() = CreateGroupRoomUseCase.defaultName(selectedFriends)
}

/** 화면 쪽에서만 바뀌는 상태 (친구 목록 제외) */
private data class Local(
    val name: String = "",
    val selectedIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val createdRoomId: String? = null,
    val showError: Boolean = false,
)

@HiltViewModel
class NewGroupRoomViewModel @Inject constructor(
    observeFriends: ObserveInvitableFriendsUseCase,
    private val createGroupRoom: CreateGroupRoomUseCase,
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<NewGroupRoomUiState> = combine(observeFriends(), local) { friends, l ->
        NewGroupRoomUiState(
            friends = friends.sortedBy { it.name },
            isLoading = false,
            name = l.name,
            // 친구 목록이 바뀌어 사라진 사람은 선택에서도 뺀다
            selectedIds = l.selectedIds.filterTo(mutableSetOf()) { id -> friends.any { it.id == id } },
            isCreating = l.isCreating,
            createdRoomId = l.createdRoomId,
            showError = l.showError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewGroupRoomUiState())

    fun onNameChange(text: String) = local.update {
        it.copy(name = text.take(CreateGroupRoomUseCase.MAX_NAME_LENGTH))
    }

    /** 친구 줄을 누를 때마다 선택 ↔ 해제 */
    fun onFriendToggle(friendId: String) = local.update {
        val ids = if (friendId in it.selectedIds) it.selectedIds - friendId else it.selectedIds + friendId
        it.copy(selectedIds = ids)
    }

    fun onCreate() {
        val state = uiState.value
        if (!state.canCreate) return
        local.update { it.copy(isCreating = true, showError = false) }
        viewModelScope.launch {
            createGroupRoom(state.name, state.selectedFriends)
                .onSuccess { roomId -> local.update { it.copy(isCreating = false, createdRoomId = roomId) } }
                .onFailure { local.update { it.copy(isCreating = false, showError = true) } }
        }
    }

    /** 화면이 방으로 이동했으면 다시 이동하지 않게 지운다 */
    fun onNavigated() = local.update { it.copy(createdRoomId = null) }

    fun onErrorShown() = local.update { it.copy(showError = false) }
}
