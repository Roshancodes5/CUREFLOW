import { Router, Response } from 'express';
import prisma from '../config/database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = Router();

// Get Patient Appointments
router.get('/', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const patientId = req.user?.userId;
    const appointments = await prisma.appointment.findMany({
      where: { patientId },
      include: {
        doctor: {
          include: {
            user: { select: { name: true } },
            department: { select: { name: true } }
          }
        }
      },
      orderBy: { dateTime: 'desc' }
    });
    res.json(appointments);
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Book Appointment
router.post('/', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const patientId = req.user?.userId;
    const { doctorId, dateTime } = req.body;

    if (!doctorId || !dateTime) {
      res.status(400).json({ error: 'Doctor ID and appointment dateTime are required.' });
      return;
    }

    const doctor = await prisma.doctor.findUnique({ where: { id: doctorId } });
    if (!doctor) {
      res.status(404).json({ error: 'Doctor not found.' });
      return;
    }

    // Generate token number (e.g. A-105)
    const count = await prisma.appointment.count({ where: { doctorId } });
    const tokenNumber = `A-${101 + count}`;

    const appointment = await prisma.appointment.create({
      data: {
        patientId: patientId!,
        doctorId,
        dateTime: new Date(dateTime),
        tokenNumber,
        status: 'BOOKED'
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

    // Ensure queue entry exists for doctor
    await prisma.queue.upsert({
      where: { doctorId },
      update: { patientsAheadCount: { increment: 1 } },
      create: { doctorId, currentServingToken: 'A-100', patientsAheadCount: 1, estimatedWaitMinutes: 15 }
    });

    res.status(201).json({
      message: 'Appointment booked successfully',
      appointment
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Reschedule Appointment
router.put('/:id/reschedule', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    const { newDateTime } = req.body;
    const patientId = req.user?.userId;

    if (!newDateTime) {
      res.status(400).json({ error: 'New dateTime is required.' });
      return;
    }

    const appointment = await prisma.appointment.findUnique({ where: { id } });
    if (!appointment || appointment.patientId !== patientId) {
      res.status(404).json({ error: 'Appointment not found or unauthorized.' });
      return;
    }

    const updated = await prisma.appointment.update({
      where: { id },
      data: {
        dateTime: new Date(newDateTime),
        status: 'RESCHEDULED'
      }
    });

    res.json({
      message: 'Appointment rescheduled successfully',
      appointment: updated
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Cancel Appointment
router.delete('/:id/cancel', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    const patientId = req.user?.userId;

    const appointment = await prisma.appointment.findUnique({ where: { id } });
    if (!appointment || appointment.patientId !== patientId) {
      res.status(404).json({ error: 'Appointment not found or unauthorized.' });
      return;
    }

    const updated = await prisma.appointment.update({
      where: { id },
      data: { status: 'CANCELLED' }
    });

    res.json({
      message: 'Appointment cancelled successfully',
      appointment: updated
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

export default router;
