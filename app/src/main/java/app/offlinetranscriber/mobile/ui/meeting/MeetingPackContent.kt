package app.offlinetranscriber.mobile.ui.meeting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.repository.MeetingPackState
import app.offlinetranscriber.mobile.meeting.MeetingInsightsTab
import app.offlinetranscriber.mobile.meeting.model.MeetingActionStatus

@Composable
fun MeetingPackContent(
    packState: MeetingPackState,
    selectedTab: MeetingInsightsTab,
    onSelectTab: (MeetingInsightsTab) -> Unit,
    onToggleAction: (actionId: Long, done: Boolean) -> Unit,
    onEditAction: (actionId: Long, text: String, assignee: String, dueText: String) -> Unit,
    onOpenSource: (startMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = MeetingInsightsTab.entries

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { tab ->
                val count = when (tab) {
                    MeetingInsightsTab.OVERVIEW -> null
                    MeetingInsightsTab.ACTIONS -> packState.actions.count { it.status == MeetingActionStatus.OPEN.name }
                    MeetingInsightsTab.DECISIONS -> packState.decisions.size
                    MeetingInsightsTab.QUESTIONS -> packState.questions.size
                    MeetingInsightsTab.TIMELINE -> packState.topics.size
                }

                val title = when (tab) {
                    MeetingInsightsTab.OVERVIEW -> "Overview"
                    MeetingInsightsTab.ACTIONS -> if (count != null && count > 0) "Actions ($count)" else "Actions"
                    MeetingInsightsTab.DECISIONS -> if (count != null && count > 0) "Decisions ($count)" else "Decisions"
                    MeetingInsightsTab.QUESTIONS -> if (count != null && count > 0) "Questions ($count)" else "Questions"
                    MeetingInsightsTab.TIMELINE -> "Timeline"
                }

                Tab(
                    selected = selectedTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                MeetingInsightsTab.OVERVIEW -> {
                    MeetingOverviewTab(
                        packState = packState,
                        onOpenSource = onOpenSource
                    )
                }
                MeetingInsightsTab.ACTIONS -> {
                    MeetingActionsTab(
                        actions = packState.actions,
                        onToggleAction = onToggleAction,
                        onEditAction = onEditAction,
                        onOpenSource = onOpenSource
                    )
                }
                MeetingInsightsTab.DECISIONS -> {
                    MeetingDecisionsTab(
                        decisions = packState.decisions,
                        onOpenSource = onOpenSource
                    )
                }
                MeetingInsightsTab.QUESTIONS -> {
                    MeetingQuestionsTab(
                        questions = packState.questions,
                        onOpenSource = onOpenSource
                    )
                }
                MeetingInsightsTab.TIMELINE -> {
                    MeetingTimelineTab(
                        topics = packState.topics,
                        onOpenSource = onOpenSource
                    )
                }
            }
        }
    }
}
