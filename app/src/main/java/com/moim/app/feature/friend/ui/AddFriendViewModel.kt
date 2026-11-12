package com.moim.app.feature.friend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.FriendSearchResult
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.usecase.ObserveCurrentUserUseCase
import com.moim.app.feature.friend.domain.usecase.ObserveFriendRequestsUseCase
import com.moim.app.feature.friend.domain.usecase.RespondFriendRequestUseCase
import com.moim.app.feature.friend.domain.usecase.SearchFriendUseCase
import com.moim.app.feature.friend.domain.usecase.SendFriendRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** 화면 아래 잠깐 뜨는 안내 */
enum class AddFriendMessage { INVALID_ID, REQUEST_SENT, ACCEPTED, DECLINED, FAILED }

/** 친구 추가 화면 상태 */
data class AddFriendUiState(
    val now: LocalDateTime,
    val me: UserProfile? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false, // 한 번이라도 [검색]을 눌렀는지 (결과 없음 문구용)
    val result: FriendSearchResult? = null,
    val isSending: Boolean = false,
    val requests: List<FriendRequest> = emptyList(),
    val showAllRequests: Boolean = false,
    val message: AddFriendMessage? = null,
) {
    /** 접혀 있을 때는 2개까지만 보인다 */
    val visibleRequests: List<FriendRequest>
        get() = if (showAllRequests) requests else requests.take(COLLAPSED_REQUEST_COUNT)

    companion object {
        const val COLLAPSED_REQUEST_COUNT = 2
    }
}

/** 검색 관련 화면 상태 (요청 목록·내 정보 제외) */
private data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val result: FriendSearchResult? = null,
    val isSending: Boolean = false,
    val showAllRequests: Boolean = false,
    val message: AddFriendMessage? = null,
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeRequests: ObserveFriendRequestsUseCase,
    private val searchFriend: SearchFriendUseCase,
    private val sendRequest: SendFriendRequestUseCase,
    private val respond: RespondFriendRequestUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val search = MutableStateFlow(SearchState())

    val uiState: StateFlow<AddFriendUiState> = combine(
        search,
        observeCurrentUser(),
        observeRequests(),
    ) { s, me, requests ->
        AddFriendUiState(
            now = LocalDateTime.now(clock),
            me = me,
            query = s.query,
            isSearching = s.isSearching,
            hasSearched = s.hasSearched,
            result = s.result,
            isSending = s.isSending,
            requests = requests.sortedByDescending { it.requestedAt },
            showAllRequests = s.showAllRequests,
            message = s.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddFriendUiState(now = LocalDateTime.now(clock)),
    )

    fun onQueryChange(value: String) = search.update {
        // 학번은 숫자 7자리. 검색어가 바뀌면 이전 결과는 지운다
        it.copy(query = value.filter(Char::isDigit).take(7), result = null, hasSearched = false)
    }

    fun onClearQuery() = search.update { it.copy(query = "", result = null, hasSearched = false) }

    fun onSearch() {
        val query = search.value.query
        if (search.value.isSearching) return
        viewModelScope.launch {
            search.update { it.copy(isSearching = true) }
            searchFriend(query)
                .onSuccess { found -> search.update { it.copy(isSearching = false, hasSearched = true, result = found) } }
                .onFailure {
                    val msg = if (it is IllegalArgumentException) AddFriendMessage.INVALID_ID else AddFriendMessage.FAILED
                    search.update { s -> s.copy(isSearching = false, message = msg) }
                }
        }
    }

    /** [친구 요청] → 버튼이 "요청됨"으로 바뀐다 */
    fun onSendRequest() {
        val target = search.value.result ?: return
        if (target.relation != FriendRelation.NONE || search.value.isSending) return
        viewModelScope.launch {
            search.update { it.copy(isSending = true) }
            sendRequest(target.profile.id)
                .onSuccess {
                    search.update {
                        it.copy(
                            isSending = false,
                            result = it.result?.copy(relation = FriendRelation.REQUESTED),
                            message = AddFriendMessage.REQUEST_SENT,
                        )
                    }
                }
                .onFailure { search.update { it.copy(isSending = false, message = AddFriendMessage.FAILED) } }
        }
    }

    fun onAccept(requestId: String) = respondTo(requestId, accept = true)

    fun onDecline(requestId: String) = respondTo(requestId, accept = false)

    private fun respondTo(requestId: String, accept: Boolean) {
        // 수락/거절하면 목록에서 빠지므로 누가 보낸 요청인지 먼저 기억해 둔다
        val fromId = uiState.value.requests.firstOrNull { it.id == requestId }?.from?.id
        viewModelScope.launch {
            val result = if (accept) respond.accept(requestId) else respond.decline(requestId)
            val msg = when {
                result.isFailure -> AddFriendMessage.FAILED
                accept -> AddFriendMessage.ACCEPTED
                else -> AddFriendMessage.DECLINED
            }
            search.update { s ->
                // 검색 결과에 같은 사람이 떠 있으면 관계도 바로 갱신
                val updated = s.result?.takeIf { it.profile.id == fromId && result.isSuccess }
                    ?.copy(relation = if (accept) FriendRelation.FRIEND else FriendRelation.NONE)
                s.copy(result = updated ?: s.result, message = msg)
            }
        }
    }

    fun onToggleShowAll() = search.update { it.copy(showAllRequests = !it.showAllRequests) }

    fun onMessageShown() = search.update { it.copy(message = null) }
}
