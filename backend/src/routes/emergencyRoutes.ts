import { Router, Response } from 'express';
import prisma from '../config/database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = Router();

// Dispatch Emergency
router.post('/dispatch', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const patientId = req.user?.userId;
    const { latitude, longitude, emergencyType } = req.body;

    if (latitude === undefined || longitude === undefined || !emergencyType) {
      res.status(400).json({ error: 'Latitude, longitude, and emergencyType are required.' });
      return;
    }

    const emergency = await prisma.emergencyLog.create({
      data: {
        patientId: patientId!,
        latitude,
        longitude,
        emergencyType,
        status: 'DISPATCHED',
        receivingHospital: 'Careflow General Hospital - Emergency Trauma Unit'
      }
    });

    res.status(201).json({
      message: 'Emergency dispatched successfully. Medical personnel are en route.',
      emergency
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

// Get Emergency Status
router.get('/:id/status', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    const emergency = await prisma.emergencyLog.findUnique({ where: { id } });

    if (!emergency) {
      res.status(404).json({ error: 'Emergency log not found.' });
      return;
    }

    res.json(emergency);
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

export default router;
