let mediaRecorder;
let microphoneStream;
let recordedChunks = [];
let recordingUrl;
let recordingTimeout;

// Maximum recording length in milliseconds.
const MAX_RECORDING_DURATION = 60_000;

// Get the main page elements.
const recordButton = document.getElementById('record-button');
const stopButton = document.getElementById('stop-button');
const audioPlayer = document.getElementById('recorded-audio');
const transcriptionResult = document.getElementById('transcription-result');
const recordingStatus = document.getElementById('recording-status');


// clicking record button
recordButton.addEventListener('click', async () => {

    // Remove any previous transcription.
    transcriptionResult.textContent = '';

    // Disable buttons while microphone permission is being requested.
    recordButton.disabled = true;
    stopButton.disabled = true;

    recordingStatus.textContent = 'Requesting microphone access...';

    try {

        // Check that the browser supports microphone recording.
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            throw new Error('Microphone recording is not supported by this browser.');
        }

        // Request microphone permission.
        microphoneStream = await navigator.mediaDevices.getUserMedia({
            audio: true
        });

        console.log('Microphone access granted');

        // Use WebM with Opus compression when supported.
        const mimeType = getSupportedMimeType();

        if (mimeType) {
            mediaRecorder = new MediaRecorder(
                microphoneStream,
                { mimeType: mimeType }
            );
        } else {
            mediaRecorder = new MediaRecorder(microphoneStream);
        }

        // Clear chunks from any previous recording.
        recordedChunks = [];

        // Collect audio data while recording.
        mediaRecorder.ondataavailable = (event) => {

            if (event.data.size > 0) {
                recordedChunks.push(event.data);
            }
        };


        // Runs after recording has stopped.
        mediaRecorder.onstop = async () => {

            // Stop using the physical microphone.
            stopMicrophone();

            // Clear the automatic recording timer.
            clearTimeout(recordingTimeout);

            recordingStatus.textContent = 'Transcribing...';

            // Create the recorded audio file.
            const blob = new Blob(
                recordedChunks,
                { type: mediaRecorder.mimeType }
            );

            // Remove the old audio URL before creating a new one.
            if (recordingUrl) {
                URL.revokeObjectURL(recordingUrl);
            }

            recordingUrl = URL.createObjectURL(blob);
            audioPlayer.src = recordingUrl;

            try {

                // Send the audio file to the Java backend.
                const formData = new FormData();

                formData.append(
                    'audio',
                    blob,
                    'recording.webm'
                );

                const response = await fetch('/api/transcriptions', {
                    method: 'POST',
                    body: formData
                });

                // Read the server response once.
                const result = await response.text();

                // Handle HTTP errors instead of displaying raw JSON.
                if (!response.ok) {

                    console.error(
                        'Transcription request failed:',
                        response.status,
                        result
                    );

                    throw new Error(
                        `Server returned HTTP ${response.status}`
                    );
                }

                // Display the completed transcription.
                transcriptionResult.textContent = result;

                recordingStatus.textContent =
                    'Transcription complete';

            } catch (error) {

                console.error(
                    'Transcription error:',
                    error
                );

                transcriptionResult.textContent =
                    'Unable to transcribe the recording. Please try again.';

                recordingStatus.textContent =
                    'Transcription failed';

            } finally {

                // Allow another recording once processing is finished.
                recordButton.disabled = false;
                stopButton.disabled = true;

                recordButton.textContent = 'Record';
            }
        };


        // Start recording.
        mediaRecorder.start();

        recordingStatus.textContent =
            'Recording in progress...';
        console.log('recordingStatus element:', recordingStatus);

        recordButton.disabled = true;
        stopButton.disabled = false;

        recordButton.textContent = 'Recording';


        // Automatically stop very long recordings.
        recordingTimeout = setTimeout(() => {

            if (
                mediaRecorder &&
                mediaRecorder.state === 'recording'
            ) {
                stopRecording();

                recordingStatus.textContent =
                    'Maximum recording time reached. Transcribing...';
            }

        }, MAX_RECORDING_DURATION);


    } catch (error) {

        // Handle microphone permission and device errors.
        handleMicrophoneError(error);

        stopMicrophone();

        recordButton.disabled = false;
        stopButton.disabled = true;

        recordButton.textContent = 'Record';
    }
});


// stop button
stopButton.addEventListener('click', () => {

    stopRecording();

});


// Stops the current recording safely.
function stopRecording() {

    if (
        mediaRecorder &&
        mediaRecorder.state === 'recording'
    ) {

        mediaRecorder.stop();

        recordButton.disabled = true;
        stopButton.disabled = true;

        recordButton.textContent = 'Record';

        recordingStatus.textContent = 'Transcribing...';
    }
}


// Stop all microphone tracks so the browser releases the microphone.
function stopMicrophone() {

    if (microphoneStream) {

        microphoneStream.getTracks().forEach(track => {
            track.stop();
        });

        microphoneStream = null;
    }
}


// Choose an efficient audio format supported by the browser.
function getSupportedMimeType() {

    const supportedTypes = [
        'audio/webm;codecs=opus',
        'audio/webm'
    ];

    for (const type of supportedTypes) {

        if (MediaRecorder.isTypeSupported(type)) {
            return type;
        }
    }

    // Let the browser choose its default if WebM is unavailable.
    return '';
}


// Show useful microphone errors to the user.
function handleMicrophoneError(error) {

    if (
        error.name === 'NotAllowedError' ||
        error.name === 'PermissionDeniedError'
    ) {

        recordingStatus.textContent =
            'Microphone access was denied. Please allow microphone access and try again.';

    } else if (
        error.name === 'NotFoundError' ||
        error.name === 'DeviceNotFoundError'
    ) {

        recordingStatus.textContent =
            'No microphone was found on this device.';

    } else {

        recordingStatus.textContent =
            'Unable to access the microphone. Please try again.';

        console.error('Microphone error:', error);
    }
}