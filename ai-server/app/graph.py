"""
LangGraph로 만든 눈송이 대화 흐름 (보고서 4.3).

  classify(Haiku) ─┬─ register ─→ extract(Sonnet) ─→ validate(코드) ─→ 끝
                   └─ query/update/delete/chat ─→ 끝 (대화는 API에서 스트리밍)

결과(state)는 API가 앱으로 보낼 이벤트로 바꾼다:
- 확인 카드(candidates) 또는 되묻기(clarify)
- 일정이 아니면 intent만 정하고, 답장은 스트리밍으로 따로 보낸다
"""
from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from .llm import LLM
from .schemas import ChatTurn, ExtractedSchedule, Intent, ScheduleCandidate, UserContext
from .validate import validate


class ChatState(TypedDict, total=False):
    message: str
    user: UserContext
    history: list[ChatTurn]
    intent: Intent
    empathy: str
    extracted: list[ExtractedSchedule]  # extract → check 사이 중간값
    candidates: list[ScheduleCandidate]
    clarify: str | None


def build_graph(llm: LLM):
    async def classify(state: ChatState) -> ChatState:
        return {"intent": await llm.classify_intent(state["message"], state.get("history", []))}

    async def extract(state: ChatState) -> ChatState:
        result = await llm.extract(state["message"], state["user"], state.get("history", []))
        return {"empathy": result.empathy, "extracted": result.schedules}

    async def check(state: ChatState) -> ChatState:
        extracted = state.get("extracted", [])
        if not extracted:
            # 일정 등록인 줄 알았는데 일정이 안 나오면 일반 대화로 돌린다
            return {"intent": Intent.CHAT}
        user = state["user"]
        result = validate(extracted, user.today, user.notification_prefs)
        return {"candidates": result.candidates, "clarify": result.clarify_question}

    def route(state: ChatState) -> str:
        return "extract" if state["intent"] == Intent.REGISTER else END

    graph = StateGraph(ChatState)
    graph.add_node("classify", classify)
    graph.add_node("extract", extract)
    graph.add_node("check", check)
    graph.add_edge(START, "classify")
    graph.add_conditional_edges("classify", route, {"extract": "extract", END: END})
    graph.add_edge("extract", "check")
    graph.add_edge("check", END)
    return graph.compile()
