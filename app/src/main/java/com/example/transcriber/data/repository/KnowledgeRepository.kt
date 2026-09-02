package com.example.transcriber.data.repository

import com.example.transcriber.data.database.BookmarkDao
import com.example.transcriber.data.database.CollectionDao
import com.example.transcriber.data.database.SearchDao
import com.example.transcriber.data.model.CollectionEntity
import com.example.transcriber.data.model.TranscriptCollectionCrossRef

class KnowledgeRepository(
    private val searchDao: SearchDao,
    private val bookmarkDao: BookmarkDao,
    private val collectionDao: CollectionDao
) {

    suspend fun search(
        ftsQuery: String,
        mediaType: String?,
        bookmarksOnly: Boolean,
        limit: Int = 150
    ) = searchDao.search(
        ftsQuery = ftsQuery,
        mediaType = mediaType,
        bookmarksOnly = bookmarksOnly,
        limit = limit
    )

    fun observeBookmarkedSegmentIds(
        transcriptId: Long
    ) = bookmarkDao.observeBookmarkedSegmentIds(
        transcriptId
    )

    fun observeRecentBookmarks(
        limit: Int = 12
    ) = bookmarkDao.observeRecentBookmarks(limit)

    suspend fun toggleBookmark(
        transcriptId: Long,
        segmentId: Long
    ): Boolean = bookmarkDao.toggle(
        transcriptId = transcriptId,
        segmentId = segmentId
    )

    fun observeCollectionSummaries() =
        collectionDao.observeCollectionSummaries()

    fun observeLibraryTranscripts() =
        collectionDao.observeLibraryTranscripts()

    fun observeCollection(collectionId: Long) =
        collectionDao.observeCollection(collectionId)

    fun observeTranscriptsInCollection(
        collectionId: Long
    ) = collectionDao.observeTranscriptsInCollection(
        collectionId
    )

    suspend fun createCollection(
        name: String
    ): Long {
        val normalized = name.trim()
        require(normalized.isNotBlank()) {
            "Collection name can't be empty."
        }

        return collectionDao.insertCollection(
            CollectionEntity(name = normalized)
        )
    }

    suspend fun deleteCollection(
        collectionId: Long
    ) {
        collectionDao.deleteCollection(collectionId)
    }

    suspend fun collectionIdsForTranscript(
        transcriptId: Long
    ): Set<Long> =
        collectionDao.getCollectionIdsForTranscript(
            transcriptId
        ).toSet()

    suspend fun setTranscriptInCollection(
        transcriptId: Long,
        collectionId: Long,
        selected: Boolean
    ) {
        if (selected) {
            collectionDao.addMembership(
                TranscriptCollectionCrossRef(
                    transcriptId = transcriptId,
                    collectionId = collectionId
                )
            )
        } else {
            collectionDao.removeMembership(
                transcriptId = transcriptId,
                collectionId = collectionId
            )
        }
    }
}
