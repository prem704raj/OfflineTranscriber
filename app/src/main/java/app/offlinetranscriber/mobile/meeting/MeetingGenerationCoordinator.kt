package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.classic.ClassicMeetingEngine
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingPack
import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment
import app.offlinetranscriber.mobile.meeting.nano.MeetingProtocolNanoEngine
import com.google.mlkit.genai.prompt.GenerativeModel

class MeetingGenerationCoordinator(
    private val model: GenerativeModel? = null,
    private val classicEngine: ClassicMeetingEngine = ClassicMeetingEngine(),
    private val chunker: MeetingChunker = MeetingChunker()
) {

    suspend fun generate(
        segments: List<MeetingSourceSegment>,
        allowNano: Boolean,
        onProgress: (Int) -> Unit = {}
    ): GeneratedMeetingPack {
        val classicPack = classicEngine.generate(segments)

        if (!allowNano || model == null || segments.isEmpty()) {
            onProgress(100)
            return classicPack
        }

        val chunks = chunker.chunk(segments)
        if (chunks.isEmpty()) {
            onProgress(100)
            return classicPack
        }

        val enhancedPacks = mutableListOf<GeneratedMeetingPack>()
        val nanoEngine = MeetingProtocolNanoEngine(model)

        chunks.forEachIndexed { index, chunk ->
            val chunkPack = runCatching {
                nanoEngine.generate(chunk)
            }.getOrElse {
                val split = TokenSafeChunkSplitter.split(chunk)
                if (split != null) {
                    val first = runCatching { nanoEngine.generate(split.first) }.getOrElse {
                        classicEngine.generate(split.first.segments)
                    }
                    val second = runCatching { nanoEngine.generate(split.second) }.getOrElse {
                        classicEngine.generate(split.second.segments)
                    }
                    MeetingPackMerger.merge(listOf(first, second), classicEngine.generate(chunk.segments))
                } else {
                    classicEngine.generate(chunk.segments)
                }
            }

            enhancedPacks += chunkPack
            val progressPercent = ((index + 1) * 90) / chunks.size
            onProgress(progressPercent.coerceIn(1, 95))
        }

        val finalPack = MeetingPackMerger.merge(enhancedPacks, classicPack)
        onProgress(100)
        return finalPack
    }
}
