package com.example.transcriber.data.repository

import androidx.room.withTransaction
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.data.model.MeetingActionEntity
import com.example.transcriber.data.model.MeetingCitationRow
import com.example.transcriber.data.model.MeetingDecisionEntity
import com.example.transcriber.data.model.MeetingPackEntity
import com.example.transcriber.data.model.MeetingQuestionEntity
import com.example.transcriber.data.model.MeetingSummaryCitationEntity
import com.example.transcriber.data.model.MeetingTopicEntity
import com.example.transcriber.meeting.MeetingActionPreserver
import com.example.transcriber.meeting.model.GeneratedMeetingPack
import com.example.transcriber.meeting.model.MeetingActionStatus
import com.example.transcriber.meeting.model.MeetingSourceSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class MeetingPackState(
    val pack: MeetingPackEntity,
    val summaryCitations: List<MeetingCitationRow>,
    val actions: List<MeetingActionEntity>,
    val decisions: List<MeetingDecisionEntity>,
    val questions: List<MeetingQuestionEntity>,
    val topics: List<MeetingTopicEntity>
)

class MeetingRepository(
    private val database: AppDatabase
) {

    private val dao = database.meetingDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observe(
        transcriptId: Long
    ): Flow<MeetingPackState?> =
        dao.observePack(transcriptId)
            .flatMapLatest { pack ->
                if (pack == null) {
                    flowOf(null)
                } else {
                    combine(
                        dao.observeResolvedSummaryCitations(pack.id),
                        dao.observeActions(pack.id),
                        dao.observeDecisions(pack.id),
                        dao.observeQuestions(pack.id),
                        dao.observeTopics(pack.id)
                    ) { citations, actions, decisions, questions, topics ->
                        MeetingPackState(
                            pack = pack,
                            summaryCitations = citations,
                            actions = actions,
                            decisions = decisions,
                            questions = questions,
                            topics = topics
                        )
                    }
                }
            }

    suspend fun sourceSegments(
        transcriptId: Long
    ): List<MeetingSourceSegment> {
        val segments = dao.transcriptSegments(transcriptId)
        val assignments = database.speakerDiarizationDao().getAssignmentsOnce(transcriptId).associateBy { it.segmentId }

        return segments.map { row ->
            val speakerInfo = assignments[row.id]
            MeetingSourceSegment(
                segmentId = row.id,
                transcriptId = row.transcriptId,
                startMs = row.startMs,
                endMs = row.endMs,
                text = row.text,
                speakerName = speakerInfo?.customName,
                speakerUserNamed = speakerInfo?.userNamed ?: false
            )
        }
    }

    suspend fun replaceGeneratedPack(
        transcriptId: Long,
        generated: GeneratedMeetingPack
    ) {
        database.withTransaction {
            val oldPack = dao.getPack(transcriptId)
            val oldActions = oldPack?.let { dao.getActionsOnce(it.id) }.orEmpty()

            val actionsToPersist = MeetingActionPreserver.merge(
                old = oldActions,
                generated = generated.actions
            )

            dao.deletePackForTranscript(transcriptId)

            val packId = dao.insertPack(
                MeetingPackEntity(
                    transcriptId = transcriptId,
                    summary = generated.summary.text,
                    engine = generated.engine.name
                )
            )

            if (generated.summary.sourceSegmentIds.isNotEmpty()) {
                dao.insertSummaryCitations(
                    generated.summary.sourceSegmentIds
                        .distinct()
                        .mapIndexed { index, segmentId ->
                            MeetingSummaryCitationEntity(
                                meetingPackId = packId,
                                segmentId = segmentId,
                                position = index
                            )
                        }
                )
            }

            if (actionsToPersist.isNotEmpty()) {
                dao.insertActions(
                    actionsToPersist.map { item ->
                        MeetingActionEntity(
                            meetingPackId = packId,
                            text = item.generated.text,
                            assignee = item.generated.assignee,
                            dueText = item.generated.dueText,
                            sourceSegmentId = item.generated.sourceSegmentId,
                            startMs = item.generated.startMs,
                            status = item.status.name,
                            manuallyEdited = item.manuallyEdited
                        )
                    }
                )
            }

            if (generated.decisions.isNotEmpty()) {
                dao.insertDecisions(
                    generated.decisions.map { item ->
                        MeetingDecisionEntity(
                            meetingPackId = packId,
                            text = item.text,
                            sourceSegmentId = item.sourceSegmentId,
                            startMs = item.startMs
                        )
                    }
                )
            }

            if (generated.questions.isNotEmpty()) {
                dao.insertQuestions(
                    generated.questions.map { item ->
                        MeetingQuestionEntity(
                            meetingPackId = packId,
                            text = item.text,
                            sourceSegmentId = item.sourceSegmentId,
                            startMs = item.startMs
                        )
                    }
                )
            }

            if (generated.topics.isNotEmpty()) {
                dao.insertTopics(
                    generated.topics.map { item ->
                        MeetingTopicEntity(
                            meetingPackId = packId,
                            title = item.title,
                            sourceSegmentId = item.sourceSegmentId,
                            startMs = item.startMs
                        )
                    }
                )
            }
        }
    }

    suspend fun setActionDone(
        id: Long,
        done: Boolean
    ) {
        dao.updateActionStatus(
            actionId = id,
            status = if (done) MeetingActionStatus.DONE.name else MeetingActionStatus.OPEN.name
        )
    }

    suspend fun editAction(
        id: Long,
        text: String,
        assignee: String,
        dueText: String
    ) {
        val safeText = text
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(300)

        require(safeText.length >= 2) { "Action text must have at least 2 characters." }

        dao.editAction(
            actionId = id,
            text = safeText,
            assignee = assignee.trim().take(80),
            dueText = dueText.trim().take(80)
        )
    }
}
