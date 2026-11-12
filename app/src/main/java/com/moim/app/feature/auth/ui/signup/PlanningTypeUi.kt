package com.moim.app.feature.auth.ui.signup

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.moim.app.R
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility

/** 성향 카드에 보여줄 이모지/문구 (UI 전용 정보라 domain이 아닌 ui 계층에 둔다) */
data class PlanningTypeUi(
    val emoji: String,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:StringRes val selectedDescription: Int,
)

fun PlanningType.toUi(): PlanningTypeUi = when (this) {
    PlanningType.EARLY -> PlanningTypeUi(
        "🐢", R.string.planning_early, R.string.planning_early_desc, R.string.planning_early_desc_selected,
    )
    PlanningType.THREE_DAY -> PlanningTypeUi(
        "📚", R.string.planning_three_day, R.string.planning_three_day_desc, R.string.planning_three_day_desc_selected,
    )
    PlanningType.CRAMMING -> PlanningTypeUi(
        "⚡", R.string.planning_cramming, R.string.planning_cramming_desc, R.string.planning_cramming_desc_selected,
    )
    PlanningType.PLANNER -> PlanningTypeUi(
        "🗓️", R.string.planning_planner, R.string.planning_planner_desc, R.string.planning_planner_desc_selected,
    )
}

@StringRes
fun Visibility.labelRes(): Int = when (this) {
    Visibility.PUBLIC -> R.string.visibility_public
    Visibility.FRIENDS -> R.string.visibility_friends
    Visibility.GROUP -> R.string.visibility_group
    Visibility.PRIVATE -> R.string.visibility_private
}

/** 0 → "당일", 3 → "3일 전" */
@Composable
fun reminderDaysLabel(days: Int): String =
    if (days == 0) stringResource(R.string.reminder_same_day) else stringResource(R.string.signup2_days_before, days)
