import { Router, Request, Response } from 'express';
import prisma from '../config/database';

const router = Router();

// Get All Departments
router.get('/departments', async (req: Request, res: Response): Promise<void> => {
  try {
    const departments = await prisma.department.findMany({
      include: {
        doctors: {
          include: {
            user: {
              select: { name: true, email: true, phone: true }
            }
          }
        }
      }
    });
    res.json(departments);
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Get All Doctors
router.get('/doctors', async (req: Request, res: Response): Promise<void> => {
  try {
    const departmentId = req.query.departmentId as string;
    const whereClause = departmentId ? { departmentId } : {};

    const doctors = await prisma.doctor.findMany({
      where: whereClause,
      include: {
        department: true,
        user: {
          select: { name: true, email: true, phone: true }
        }
      }
    });
    res.json(doctors);
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Get Doctor Availability & Slots
router.get('/doctors/:id/slots', async (req: Request, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    const doctor = await prisma.doctor.findUnique({
      where: { id },
      include: { user: { select: { name: true } }, department: true }
    });

    if (!doctor) {
      res.status(404).json({ error: 'Doctor not found.' });
      return;
    }

    // Mock generated available appointment slots for the next 3 days
    const slots = [
      { slotId: 'slot_1', time: '09:00 AM', date: new Date(Date.now() + 86400000).toISOString().split('T')[0], available: true },
      { slotId: 'slot_2', time: '10:30 AM', date: new Date(Date.now() + 86400000).toISOString().split('T')[0], available: true },
      { slotId: 'slot_3', time: '02:00 PM', date: new Date(Date.now() + 86400000).toISOString().split('T')[0], available: false },
      { slotId: 'slot_4', time: '04:15 PM', date: new Date(Date.now() + 86400000).toISOString().split('T')[0], available: true }
    ];

    res.json({
      doctor: {
        id: doctor.id,
        name: doctor.user.name,
        specialty: doctor.specialty,
        roomNumber: doctor.roomNumber,
        department: doctor.department.name
      },
      availableSlots: slots
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

export default router;
