package comp3011.assignment1.model;

// stores the audio bytes and file information passed through the transcription layers.

// A record is suitable because this object only carries response data.

public record AudioData(byte[] data, String filename, String contentType) {

}
