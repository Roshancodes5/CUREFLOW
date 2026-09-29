import express, { Request, Response } from 'express';
import http from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import dotenv from 'dotenv';
import prisma from './config/database';

import authRoutes from './routes/authRoutes';
import hospitalRoutes from './routes/hospitalRoutes';
import appointmentRoutes from './routes/appointmentRoutes';
import queueRoutes from './routes/queueRoutes';
import voiceRoutes from './routes/voiceRoutes';
import emergencyRoutes from './routes/emergencyRoutes';

dotenv.config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE']
  }
});

app.use(cors());
app.use(express.json());

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/hospitals', hospitalRoutes);
app.use('/api/appointments', appointmentRoutes);
app.use('/api/queue', queueRoutes);
app.use('/api/voice', voiceRoutes);
app.use('/api/emergency', emergencyRoutes);

// Health Check Endpoint
app.get('/api/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', message: 'Careflow Backend API is running successfully.' });
});

// Socket.io Connection Handler for Live Queue
io.on('connection', (socket) => {
  console.log(`Client connected: ${socket.id}`);

  socket.on('join_doctor_queue', (doctorId: string) => {
    socket.join(`doctor_${doctorId}`);
    console.log(`Client ${socket.id} joined doctor queue: doctor_${doctorId}`);
  });

  socket.on('disconnect', () => {
    console.log(`Client disconnected: ${socket.id}`);
  });
});

const PORT = process.env.PORT || 5000;

server.listen(PORT, () => {
  console.log(`Careflow backend server running on port ${PORT}`);
});

export { io };
