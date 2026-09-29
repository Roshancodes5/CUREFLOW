import { Router, Request, Response } from 'express';
import prisma from '../config/database';
import { io } from '../index';

const router = Router();

// Get Live Queue Status for Doctor
router.get('/doctor/:doctorId', async (req: Request, res: Response): Promise<void> => {
  try {
    const { doctorId } = req.params;

    let queue = await prisma.queue.findUnique({
      where: { doctorId },
      include: {
        doctor: {
          include: {
            user: { select: { name: true } },
            department: { select: { name: true } }
          }
        }
      }
    });

    if (!queue) {
      queue = await prisma.queue.create({
        data: {
          doctorId,
          currentServingToken: 'A-104',
          patientsAheadCount: 3,
          estimatedWaitRange: '10-15 minutes',
          delayReason: 'Routine consultation in progress'
        },
        include: {
          doctor: {
            include: {
              user: { select: { name: true } },
              department: { select: { name: true } }
            }
          }
        }
      });
    }

    res.json(queue);
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Update Live Queue (Staff/Doctor action) - emits WebSocket event
router.post('/doctor/:doctorId/advance', async (req: Request, res: Response): Promise<void> => {
  try {
    const { doctorId } = req.params;
    const { currentServingToken, patientsAheadCount, estimatedWaitRange, delayReason } = req.body;

    const updatedQueue = await prisma.queue.upsert({
      where: { doctorId },
      update: {
        currentServingToken: currentServingToken || undefined,
        patientsAheadCount: patientsAheadCount !== undefined ? patientsAheadCount : undefined,
        estimatedWaitRange: estimatedWaitRange || undefined,
        delayReason: delayReason !== undefined ? delayReason : undefined
      },
      create: {
        doctorId,
        currentServingToken: currentServingToken || 'A-105',
        patientsAheadCount: patientsAheadCount !== undefined ? patientsAheadCount : 2,
        estimatedWaitRange: estimatedWaitRange || '10-15 minutes',
        delayReason: delayReason || 'Normal flow'
      }
    });

    io.to(`doctor_${doctorId}`).emit('queue_update', updatedQueue);

    res.json({
      message: 'Queue updated successfully and broadcasted',
      queue: updatedQueue
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

export default router;
