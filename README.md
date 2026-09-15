# Speech to Text Web Application

## Overview
This project is a Spring Boot Speech to Text (STT) web application. It allows a user to record audio through their browser, send the recording to the Java back-end and receive a text transcription from an external speech-to-text service. The application provides administrative REST end-points for server up-time, global token statistics, and graceful shutdown. 

This project was designed with a focus on separation of concerns, concurrent request handling, thread safety, secure API configuration, error handling and regression testing.


### Features of Application
This web application exhibits a wide variety of features which will be further explained below. To quote a few:
1. Record audio directly from browser after requesting permission.
2. Play back recorded audio.
3. Send recorded audio to back-end using REST.
4. Transcribe audio using external transcription API.
5. Display transcription in browser.
6. Track global input adn output token usage.
7. Report server up-time.
8. Support graceful server shutdown.
9. Handle API and server errors.
10. Support concurrent transcription requests.
11. Store global statistics safely during concurrent requests.
12. Keep API key outside source code.
13. Automated regression tests for main application functionality.

### Technology
This program employs a number of different technologies. AI was significantly helpful in understanding the implementations of these technologies, syntax specifics and purposes.

The project uses:
Java, Spring Boot, Spring Web, Spring WebFlux 'WebClient', Project Reactor, HTML, CSS, JavaScript, Browser MediaRecorder API, Maven, JUnit, Mockito to name a few.

### Project Structure
The back-end is separated into several packages to maintain SOLID principles and appropriate programming practice.

- 'controller': handles incoming REST requests
- 'service': contains application logic
- 'client': communicates with external API
- 'config': contains application configuration such as 'WebClient'
- 'model': contains application data and response objects.
- 'exception': handles application errors.

### Architecture/Transcription Flow
The back-end follows a layered structure:

Browser -> Controller -> Service -> Transcription Client & GlobalStatisticsService -> External STT API.

Controllers will handle HTTP request and response handling. Services contain application logic, which TranscriptionClient will communicate with the external API. This separation keeps external API communication independent from controller logic and makes unit testing possible.

The main transcription flow is:

Browser records audio -> POST /api/transcriptions -> TranscriptionController -> TranscriptionService -> TranscriptionClient -> External STT API -> TranscriptionResponse (Token usage recorded) -> Transcription returned to browser.


## Running the Application
Project requires Java 17 or later and a valid OpenAI API key. The API key is supplied through the environment variable: OPENAI_API_KEY.

Internet access and a modern browser with microphone support is also required.

The application can be started from the project root using:
'./mvnw spring-boot:run'

By default, the application is available at:
'http://localhost:8080'

The API key is never hard-coded into the Java or JavaScript source code and is not exposed to the browser.

## REST API
### Transcription
POST /api/transcription

Accepts an audio recording using 'multipart/form-data'. The audio must be provided using the field 'audio'. The back-end send the recording to the API and returns the transcript.

### Server Up-time
GET /api/v1/admin/uptime

Returns information about how long the server has been running. Response includes server start time, current time and up-time in seconds.

### Global Statistics
GET /api/v1/global/stats

Returns the total input and output tokens used by successful transcription requests. 

### Graceful Shutdown
POST /api/v1/admin/shutdown

Requests graceful shutdown of the application. A successful request returns '202 Accepted'.

If shutdown is in progress, another shutdown request returns '409 Conflict'.

## Speech-to-Text (STT) Integration
'TranscriptionClient' is responsible for communication with the external STT API. Audio is sent as 'multipart/form-data'. This request contains the recorded audio file and the 'gpt-4o-mini-transcribe' model. Authentication is sent using a Bearer token containing the API key. The application uses Spring 'WebClient' for external request. The transcription code returns Reactor 'Mono' objects so the application does not need to synchronously wait for the external network request before allowing other work to continue. 

The transcription URL can also be configured using: 'OPENAI_TRANSCRIPTION_URL'. This allows automated tests to replace the real API with a local test server.

## Concurrency
Transcription requests can take time because they depend on an external service. Using Reactor 'Mono' and 'WebClient' supports multiple requests being in progress at the same time without unnecessarily blocking request-processing threads while waiting for network responses. 

A concurrency regression test sends 250 concurrent HTTP transcription requests to a running instance of the application. The simulated transcription operation performs blocking work using 'Thread.sleep'. This blocking work is scheduled using Reactor's bounded elastic scheduler so it does not block the main reactive processing threads. The test checks that all 250 requests complete successfully within a defined time limit.

This test helps detect problems such as thread starvation, connection exhaustion, large performance regressions and failures under concurrent load.

## Thread-Safe Statistics
The application keeps global input and output token counts. Multiple transcription requests may finish at the same time, so these values are shared between concurrent requests. 'GlobalStatisticsService' uses 'AtomicLong' for the counters as this allows token values to be updated atomically and prevents updates from being lost when multiple threads update the statistics at the same time. 

A race-condition regression test performs 500 concurrent updates and checks that the final token counts are exactly correct.

## Graceful Shutdown
The application supports graceful shutdown through the administration REST API. When shutdown is requested, the application first returns the HTTP response and then begins shutting down. An `AtomicBoolean' is used to make sure that only one shutdown request can begin the shutdown process.

If another shutdown request is received while shutdown is already in progress, the application returns `409 Conflict'. Spring Boot graceful shutdown is enabled so active work has an opportunity to finish before the application exits.

## Front-end
The front-end of this project is written using HTML, CSS and JavaScript. The browser MediaRecorder API is used to access the microphone and record audio. The interface provides clear states including 'Ready to Record', 'Requesting Microphone Access', 'Recording', 'Transcribing', 'Transcription Complete' and 'Transcription Failed'. The Record and Stop buttons are enabled or disabled depending on the current state, strengthening the UX. Recorded audio can be played back before or after transcription. 

The front-end also handles microphone and REST request failures and displays user-friendly error messages. If the browser allows, WebM audio using Opus codec is used to reduce the amount of audio data uploaded to the server. 

## Error Handling
Backend errors are handled centrally using 'GlobalExceptionHandler'. Errors use a consistent response structure containing timestamp, HTTP status, error, message and request path. Unexpected internal errors return a general message rather than exposing internal application details.

A shutdown request received while shutdown is already in progress returns HTTP `409 Conflict'. The front-end checks whether transcription HTTP requests are successful and displays a clear error message when transcription fails.

## Security
The main exhibition of security is regarding the API key not being hard-coded in the source code. Instead, it is read from 'OPENAI_API_KEY'. This keeps the API key on the back-end and prevents it from being exposed to the browser. 

The application logging also avoids logging API keys, authorisation headers, raw audio data and transcription text.

Tests use fake API keys rather than real credentials.

## Logging
The application uses SLF4J logging for important application events. The transcription service logs when a transcription request starts, completes and/or fails. The server lifecycle logs important shutdown events. Logging is kept simple and avoids recording sensitive request information. 

Regression tests capture application logging and verify that expected operational messages are produced. The failure logging test also checks that simulated sensitive error details are not written to the normal application log.

## Regression Testing

Regression tests are used to ensure that previously working behaviour continues to operate as the application changes. The tests cover both functional behaviour and non-functional requirements such as concurrency, race conditions, logging, and error handling.

### Controller Regression Tests

'AdminControllerTest', 'StatisticsControllerTest', and 'TranscriptionControllerTest' test the REST-facing behaviour of the application.

The admin tests cover successful up-time responses, graceful shutdown requests, repeated shutdown requests, and unexpected service failures. The expected HTTP statuses and response bodies are checked against the API behaviour.

The statistics tests verify successful token statistics responses and internal error handling.

The transcription controller tests verify multi-part audio requests, asynchronous successful responses, and transcription failures.

The expected result is that each controller returns the correct HTTP status and response structure for both normal and error paths.

These tests provide assurance that changes to controller or service code do not unintentionally break the public REST API contract.

### STT Client Regression Test

'TranscriptionClientTest' tests the real 'TranscriptionClient' against a local stub HTTP server.

The test sends fake audio through the real client and inspects the HTTP request received by the stub. It verifies that Bearer authentication is present, the request uses `multipart/form-data', the audio filename and transcription model are included, and an STT-style JSON response is correctly deserialised.

The expected result is a correctly populated `TranscriptionResponse' without contacting the real external API.

This provides assurance that the outbound STT request format, authentication handling, configurable end-point, and response mapping continue to work correctly.

This test does not require internet access, a real API key, or paid API usage.

### Transcription Service Regression Test

'TranscriptionServiceTest' tests the application logic between the STT client and global statistics.

The transcription client is mocked to return a known transcription and known token usage. The real 'TranscriptionService' and 'GlobalStatisticsService' are then used.

The expected result is that the transcription text is returned unchanged and the exact input and output token values are added to the global statistics.

This provides assurance that successful STT responses are correctly processed and that token usage is not lost between the external client and statistics service.

### Concurrency Regression Test

'TranscriptionConcurrencyTest' starts the application on a real HTTP port and generates 250 concurrent transcription requests.

The transcription dependency simulates a blocking operation. The expected result is that every HTTP request completes successfully within the defined performance threshold.

This test provides assurance that the application remains stable under high concurrent request load and helps detect thread starvation, connection exhaustion, crashes, or significant performance regressions.

### Race-Condition Regression Test

'GlobalStatisticsServiceTest' specifically tests shared state under concurrent access.

The test performs 500 concurrent updates and compares the final statistics against the exact mathematically expected totals.

The expected result is that no updates are lost.

This provides assurance that 'GlobalStatisticsService' remains thread-safe and is specifically intended to surface race conditions if unsafe shared-state handling is introduced in the future.

### Logging Regression Tests

'TranscriptionServiceLoggingTest' verifies operational logging for both successful and failed transcription operations.

The expected result is that the appropriate start, completion, or failure messages are produced. The failure test also introduces simulated sensitive exception details and verifies that those details are not written to the captured application output.

This provides assurance that operational logging remains available for diagnosis without exposing sensitive information.

### Application Context Test

`Assignment1ApplicationTests' verifies that the Spring application context loads successfully.

The expected result is successful application startup during testing.

This provides a basic regression check for invalid Spring configuration, missing dependencies, or dependency injection problems.

## Running Tests
Run all tests with './mvnw clean test'

Build the complete application and run the tests with './mvnw clean package'.

Individual tests can be run using Maven by doing 
'./mvnw -Dtest=TranscriptionConcurrencyTest test'.

## Deployment
This application is packaged as a Spring Boot JAR. Build it using './mvnw clean package'. the generated JAR is placed in 'target/' and the application can then be started using Java with the required environment variables configured.]

The 'WebClient' configuration supports system proxy properties so out-bound HTTP requests can operate in environments where HTTP or HTTPS proxy settings are supplied by the deployment platform.