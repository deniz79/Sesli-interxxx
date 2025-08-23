const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');
const http = require('http');

// Express app oluştur
const app = express();
app.use(cors());
app.use(express.json());

// HTTP server oluştur
const server = http.createServer(app);

// WebSocket server oluştur
const wss = new WebSocket.Server({ server });

// Bağlı client'ları sakla
const clients = new Map();
const rooms = new Map();

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        connectedClients: clients.size,
        activeRooms: rooms.size,
        timestamp: new Date().toISOString()
    });
});

// Server durumu endpoint
app.get('/status', (req, res) => {
    const roomInfo = Array.from(rooms.entries()).map(([roomId, members]) => ({
        roomId,
        memberCount: members.size,
        members: Array.from(members)
    }));
    
    res.json({
        connectedClients: clients.size,
        activeRooms: roomInfo,
        uptime: process.uptime()
    });
});

// WebSocket bağlantı yönetimi
wss.on('connection', (ws) => {
    const clientId = generateId();
    clients.set(clientId, ws);
    
    console.log(`🟢 Client connected: ${clientId}`);
    
    // Bağlantı bilgisini gönder
    ws.send(JSON.stringify({
        type: 'connection',
        clientId: clientId,
        timestamp: Date.now()
    }));
    
    // Mesaj dinleme
    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            handleMessage(clientId, data);
        } catch (error) {
            console.error(`❌ Error parsing message from ${clientId}:`, error);
        }
    });
    
    // Bağlantı koptuğunda
    ws.on('close', () => {
        handleClientDisconnect(clientId);
    });
    
    // Hata durumunda
    ws.on('error', (error) => {
        console.error(`❌ WebSocket error for client ${clientId}:`, error);
        handleClientDisconnect(clientId);
    });
});

// Mesaj işleme fonksiyonu
function handleMessage(clientId, data) {
    console.log(`📨 Message from ${clientId}: ${data.type}`);
    
    switch(data.type) {
        case 'join-room':
            handleJoinRoom(clientId, data);
            break;
            
        case 'leave-room':
            handleLeaveRoom(clientId, data);
            break;
            
        case 'offer':
            handleOffer(clientId, data);
            break;
            
        case 'answer':
            handleAnswer(clientId, data);
            break;
            
        case 'ice-candidate':
            handleIceCandidate(clientId, data);
            break;
            
        case 'ping':
            handlePing(clientId);
            break;
            
        default:
            console.log(`⚠️ Unknown message type: ${data.type}`);
    }
}

// Odaya katılma
function handleJoinRoom(clientId, data) {
    const { roomId } = data;
    
    if (!rooms.has(roomId)) {
        rooms.set(roomId, new Set());
    }
    
    rooms.get(roomId).add(clientId);
    
    console.log(`👥 Client ${clientId} joined room: ${roomId}`);
    
    // Odadaki diğer üyelere bildir
    broadcastToRoom(roomId, {
        type: 'user-joined',
        clientId: clientId,
        roomId: roomId,
        timestamp: Date.now()
    }, clientId);
    
    // Yeni üyeye odadaki diğer üyeleri bildir
    const roomMembers = Array.from(rooms.get(roomId)).filter(id => id !== clientId);
    sendToClient(clientId, {
        type: 'room-info',
        roomId: roomId,
        members: roomMembers,
        timestamp: Date.now()
    });
}

// Odadan ayrılma
function handleLeaveRoom(clientId, data) {
    const { roomId } = data;
    
    if (rooms.has(roomId)) {
        rooms.get(roomId).delete(clientId);
        
        if (rooms.get(roomId).size === 0) {
            rooms.delete(roomId);
            console.log(`🗑️ Room ${roomId} deleted (empty)`);
        }
    }
    
    console.log(`👋 Client ${clientId} left room: ${roomId}`);
    
    // Odadaki diğer üyelere bildir
    broadcastToRoom(roomId, {
        type: 'user-left',
        clientId: clientId,
        roomId: roomId,
        timestamp: Date.now()
    }, clientId);
}

// Offer işleme
function handleOffer(clientId, data) {
    const { target, offer, roomId } = data;
    
    console.log(`📤 Offer from ${clientId} to ${target} in room ${roomId}`);
    
    sendToClient(target, {
        type: 'offer',
        from: clientId,
        offer: offer,
        roomId: roomId,
        timestamp: Date.now()
    });
}

// Answer işleme
function handleAnswer(clientId, data) {
    const { target, answer, roomId } = data;
    
    console.log(`📤 Answer from ${clientId} to ${target} in room ${roomId}`);
    
    sendToClient(target, {
        type: 'answer',
        from: clientId,
        answer: answer,
        roomId: roomId,
        timestamp: Date.now()
    });
}

// ICE candidate işleme
function handleIceCandidate(clientId, data) {
    const { target, candidate, roomId } = data;
    
    console.log(`🧊 ICE candidate from ${clientId} to ${target} in room ${roomId}`);
    
    sendToClient(target, {
        type: 'ice-candidate',
        from: clientId,
        candidate: candidate,
        roomId: roomId,
        timestamp: Date.now()
    });
}

// Ping işleme
function handlePing(clientId) {
    sendToClient(clientId, {
        type: 'pong',
        timestamp: Date.now()
    });
}

// Client bağlantısı koptuğunda
function handleClientDisconnect(clientId) {
    console.log(`🔴 Client disconnected: ${clientId}`);
    
    // Tüm odalardan çıkar
    for (const [roomId, members] of rooms.entries()) {
        if (members.has(clientId)) {
            members.delete(clientId);
            
            if (members.size === 0) {
                rooms.delete(roomId);
                console.log(`🗑️ Room ${roomId} deleted (empty)`);
            } else {
                // Odadaki diğer üyelere bildir
                broadcastToRoom(roomId, {
                    type: 'user-disconnected',
                    clientId: clientId,
                    roomId: roomId,
                    timestamp: Date.now()
                }, clientId);
            }
        }
    }
    
    // Client'ı listeden çıkar
    clients.delete(clientId);
}

// Client'a mesaj gönderme
function sendToClient(clientId, message) {
    const client = clients.get(clientId);
    if (client && client.readyState === WebSocket.OPEN) {
        client.send(JSON.stringify(message));
    } else {
        console.log(`⚠️ Client ${clientId} not found or not connected`);
    }
}

// Odaya broadcast
function broadcastToRoom(roomId, message, excludeClientId = null) {
    if (!rooms.has(roomId)) return;
    
    const members = rooms.get(roomId);
    members.forEach(clientId => {
        if (clientId !== excludeClientId) {
            sendToClient(clientId, message);
        }
    });
}

// ID oluşturma
function generateId() {
    return Math.random().toString(36).substr(2, 9);
}

// Server başlatma
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`🚀 Intercom Signaling Server running on port ${PORT}`);
    console.log(`📊 Health check: http://localhost:${PORT}/health`);
    console.log(`📈 Status: http://localhost:${PORT}/status`);
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n🛑 Shutting down server...');
    wss.close(() => {
        server.close(() => {
            console.log('✅ Server stopped');
            process.exit(0);
        });
    });
});

console.log('🎯 Intercom Signaling Server starting...');
