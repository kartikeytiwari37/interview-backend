# Interview App Backend

A Spring Boot backend service that manages AI-powered interviews using Google's Gemini Live API.

## Features

- WebSocket-based real-time communication
- Integration with Gemini Live API for AI interviewing
- Support for audio, video, and screen sharing
- Extensible architecture for multiple use cases
- CORS enabled for frontend integration

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Gemini API Key

## Configuration

Set your Gemini API key as an environment variable:

```bash
export GEMINI_API_KEY=your-api-key-here
```

Or update it in `application.properties`.

## Running the Application

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## API Endpoints

### REST Endpoints

- `GET /api/interview/health` - Health check
- `GET /api/interview/config` - Get configuration

### WebSocket Endpoints

- `/api/ws` - WebSocket connection endpoint
- `/app/interview/start` - Start an interview session
- `/app/interview/message` - Send messages during interview
- `/app/interview/end` - End an interview session

## Architecture

- **WebSocket Layer**: Handles real-time communication with frontend
- **Gemini Integration**: Manages connection to Gemini Live API
- **Service Layer**: Business logic for interview management
- **DTO Layer**: Data transfer objects for type safety

## Message Format

```json
{
  "type": "AUDIO|VIDEO|TEXT|SCREEN_SHARE|MIXED|CONTROL",
  "sessionId": "uuid",
  "content": "text content",
  "mediaChunks": [
    {
      "mimeType": "audio/pcm;rate=16000",
      "data": "base64-encoded-data"
    }
  ],
  "timestamp": 1234567890
}
```

## Extending the Application

The backend is designed to be extensible. You can:

1. Add new message types in `InterviewMessage.MessageType`
2. Implement additional handlers in `InterviewService`
3. Add new tools/functions for Gemini in `GeminiWebSocketClient`
4. Create new endpoints for specific use cases

## Security Considerations

- Add authentication/authorization as needed
- Implement rate limiting for production use
- Secure WebSocket connections with proper token validation
- Validate and sanitize all input data
