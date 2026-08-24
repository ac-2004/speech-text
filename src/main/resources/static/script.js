let mediaRecorder;

let recordButton = document.getElementById('record-button');

let stopButton = document.getElementById('stop-button');

let recordedChunks = []

// for audio player
const audioPlayer = document.getElementById('recorded-audio');

// clicking record button
recordButton.addEventListener('click', () => {
		// test: console.log("Hello")
		
		async function requestMicAccess() {
			try {
				// audio permission request
				const stream = await navigator.mediaDevices.getUserMedia({ audio:true });
				console.log("access for mic granted")
				mediaRecorder = new MediaRecorder(stream);
				
				// collecting data chunks
				mediaRecorder.ondataavailable = (event) => {
					if (event.data.size > 0) {
						recordedChunks.push(event.data);
					} 
				};
				
				// use async here because fetch() is asynchronous and we want to use await
				mediaRecorder.onstop = async () => {
					console.log('Recording Finished')
					const blob = new Blob(recordedChunks, { type: mediaRecorder.mimeType });
					console.log(mediaRecorder.mimeType);
					const recordingURL = URL.createObjectURL(blob);
					audioPlayer.src = recordingURL;
					
					// connecting to java endpoint
					const formData = new FormData();
					formData.append("audio", blob, "recording.webm");
					// http request
					const response = await fetch("/api/transcriptions", {
						method: "POST",
						body: formData
					});
					
					const result = await response.text();
					console.log('connection established? ', result); // test connection establishment
					
					
					// debugging
					console.log('blob size:', blob.size);
					console.log('blob type:', blob.type);
					console.log('player src:', audioPlayer.src);
				}
				
				// clear chunks for next session
				recordedChunks = [];
				mediaRecorder.start();
				
				recordButton.disabled = true
				stopButton.disabled = false
				
				recordButton.innerHTML = 'Recording';
				// testing if button disables (debugging)
					console.log('record disabled?', recordButton.disabled);
					console.log('stop disabled? ', stopButton.disabled);
					
			} catch (error) {
				// if user denies or no mic found
				if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
					console.error("user denied access")
				} else if (error.name === 'NotFoundError' || error.name === 'DeviceNotFoundError') {
					console.error('no mic device found on system');
				} else {
					console.error('mic error', error);
				}
			}
		}
		requestMicAccess();
	})
	
// stop button
stopButton.addEventListener('click', () => {
	mediaRecorder.stop() // stop recording
	
	// restore function abilities
	recordButton.disabled = false
	stopButton.disabled = true
	
	// testing if button disables (debugging)
	console.log('record disabled?', recordButton.disabled);
	console.log('stop disabled? ', stopButton.disabled);
	
	recordButton.innerHTML = 'Record';
	
	
	
})