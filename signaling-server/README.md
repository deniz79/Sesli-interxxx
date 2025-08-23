# Intercom Signaling Server

WebRTC signaling server for Intercom Android app.

## Features

- WebSocket-based signaling
- Room management
- Real-time communication
- Health check endpoints
- Automatic cleanup

## Installation

1. Install Node.js (v14 or higher)
2. Install dependencies:
```bash
npm install
```

## Usage

### Development
```bash
npm run dev
```

### Production
```bash
npm start
```

## Endpoints

- **Health Check**: `GET /health`
- **Status**: `GET /status`
- **WebSocket**: `ws://localhost:3000`

## WebSocket Messages

### Client to Server
- `join-room`: Join a room
- `leave-room`: Leave a room
- `offer`: Send WebRTC offer
- `answer`: Send WebRTC answer
- `ice-candidate`: Send ICE candidate
- `ping`: Ping server

### Server to Client
- `connection`: Connection established
- `user-joined`: User joined room
- `user-left`: User left room
- `user-disconnected`: User disconnected
- `room-info`: Room information
- `offer`: WebRTC offer
- `answer`: WebRTC answer
- `ice-candidate`: ICE candidate
- `pong`: Ping response

## Configuration

- **Port**: 3000 (default)
- **Environment Variable**: `PORT`

## Logs

The server provides detailed logging:
- 🟢 Client connected
- 🔴 Client disconnected
- 📨 Message received
- 👥 User joined room
- 👋 User left room
- 🧊 ICE candidate
- �� Offer/Answer sent
