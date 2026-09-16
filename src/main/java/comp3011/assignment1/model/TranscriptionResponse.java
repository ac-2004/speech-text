package comp3011.assignment1.model;

//maps the transcription text and token usage returned by the speech-to-text api.

public record TranscriptionResponse(String text, TranscriptionUsage usage) {

}
