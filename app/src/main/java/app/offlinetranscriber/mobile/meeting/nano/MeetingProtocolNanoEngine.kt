package app.offlinetranscriber.mobile.meeting.nano

import app.offlinetranscriber.mobile.meeting.MeetingChunk
import app.offlinetranscriber.mobile.meeting.MeetingSourceFormatter
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingPack
import app.offlinetranscriber.mobile.meeting.model.MeetingEngine
import com.google.mlkit.genai.prompt.GenerativeModel

class MeetingProtocolNanoEngine(
    private val model: GenerativeModel
) {

    suspend fun generate(
        chunk: MeetingChunk
    ): GeneratedMeetingPack {
        val labeled = MeetingSourceFormatter.format(chunk)
        val prompt = MeetingNanoPrompt.protocolPrompt(labeled.text)

        runCatching { model.warmup() }

        val response = model.generateContent(prompt)
        val raw = response.candidates.joinToString("\n") { it.text.orEmpty() }

        val parsed = MeetingProtocolParser.parse(raw)
            ?: error("Invalid meeting protocol response.")

        return MeetingNanoValidator.validateProtocol(
            parsed = parsed,
            chunk = chunk,
            labeledChunk = labeled,
            engine = MeetingEngine.GEMINI_NANO_PROTOCOL
        )
    }
}
