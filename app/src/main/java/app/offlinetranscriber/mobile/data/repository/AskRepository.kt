package app.offlinetranscriber.mobile.data.repository

import androidx.room.withTransaction
import app.offlinetranscriber.mobile.ask.AskEvidenceRetriever
import app.offlinetranscriber.mobile.ask.model.AskRole
import app.offlinetranscriber.mobile.ask.model.AskScope
import app.offlinetranscriber.mobile.ask.model.GroundedAskAnswer
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.model.AskCitationEntity
import app.offlinetranscriber.mobile.data.model.AskConversationEntity
import app.offlinetranscriber.mobile.data.model.AskMessageEntity

class AskRepository(
    private val database: AppDatabase
) {

    private val dao = database.askDao()

    fun retriever() = AskEvidenceRetriever(dao)

    suspend fun getOrCreateConversation(
        scope: AskScope,
        transcriptId: Long?,
        title: String
    ): Long {
        val existing = dao.latestConversation(
            scope.name,
            transcriptId
        )

        if (existing != null) {
            return existing.id
        }

        return dao.insertConversation(
            AskConversationEntity(
                scope = scope.name,
                transcriptId = transcriptId,
                title = title
            )
        )
    }

    fun observeMessages(
        conversationId: Long
    ) = dao.observeMessages(conversationId)

    suspend fun resolvedCitations(
        messageId: Long
    ) = dao.resolvedCitations(messageId)

    suspend fun saveQuestion(
        conversationId: Long,
        text: String
    ) {
        dao.insertMessage(
            AskMessageEntity(
                conversationId = conversationId,
                role = AskRole.USER.name,
                text = text,
                engine = null
            )
        )

        dao.touchConversation(conversationId)
    }

    suspend fun saveAnswer(
        conversationId: Long,
        answer: GroundedAskAnswer
    ) {
        database.withTransaction {
            val messageId = dao.insertMessage(
                AskMessageEntity(
                    conversationId = conversationId,
                    role = AskRole.ASSISTANT.name,
                    text = answer.answer,
                    engine = answer.engine.name,
                    insufficientEvidence = answer.insufficientEvidence
                )
            )

            dao.insertCitations(
                answer.citations.mapIndexed { index, citation ->
                    AskCitationEntity(
                        messageId = messageId,
                        segmentId = citation.segmentId,
                        position = index
                    )
                }
            )

            dao.touchConversation(conversationId)
        }
    }

    suspend fun clearConversation(
        id: Long
    ) {
        dao.deleteConversation(id)
    }
}
