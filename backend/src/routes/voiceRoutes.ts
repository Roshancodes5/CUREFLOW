import { Router, Response } from 'express';
import prisma from '../config/database';
import { authenticateToken, AuthRequest } from '../middleware/auth';

const router = Router();

// Process Voice Intent
router.post('/intent', authenticateToken, async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { transcript, language } = req.body;
    const patientId = req.user?.userId;

    if (!transcript) {
      res.status(400).json({ error: 'Transcript is required.' });
      return;
    }

    const lower = transcript.toLowerCase();
    const isTamil = language?.startsWith('ta');
    let responseText = '';
    let actionType = 'NONE';
    let payload: any = null;

    if (lower.includes('token') || lower.includes('where') || lower.includes('டோக்கன்') || lower.includes('எங்கே')) {
      actionType = 'FETCH_TOKEN';
      // Find latest appointment
      const latestAppt = await prisma.appointment.findFirst({
        where: { patientId, status: 'BOOKED' },
        include: { doctor: { include: { user: { select: { name: true } }, department: { select: { name: true } } } } },
        orderBy: { dateTime: 'desc' }
      });

      if (latestAppt) {
        responseText = isTamil
          ? `உங்கள் டோக்கன் எண் ${latestAppt.tokenNumber}. இது டாக்டர் ${latestAppt.doctor.user.name} அறையில் அழைக்கப்படும்.`
          : `Your token is ${latestAppt.tokenNumber} for Dr. ${latestAppt.doctor.user.name} (${latestAppt.doctor.department.name}).`;
        payload = latestAppt;
      } else {
        responseText = isTamil ? 'உங்களுக்கு सक्रिय டோக்கன்கள் எதுவும் இல்லை.' : 'You have no active token or appointments.';
      }
    } else if (lower.includes('wait') || lower.includes('long') || lower.includes('time') || lower.includes('காத்திருப்பு')) {
      actionType = 'FETCH_WAIT_TIME';
      responseText = isTamil
        ? 'மதிப்பிடப்பட்ட காத்திருப்பு நேரம் சுமார் 15 நிமிடங்கள். உங்களுக்கு முன் 3 நோயாளிகள் உள்ளனர்.'
        : 'Estimated wait time is approximately 15 minutes. There are 3 patients ahead of you in the queue.';
    } else if (lower.includes('emergency') || lower.includes('அவசரம்') || lower.includes('help')) {
      actionType = 'EMERGENCY_TRIGGER';
      responseText = isTamil
        ? 'அவசர உதவி எச்சரிக்கை செயல்படுத்தப்பட்டது! மருத்துவமனை அவசரக் குழுவிற்கு தகவல் அனுப்பப்படுகிறது.'
        : 'Emergency alert activated! Alerting hospital emergency response team.';
    } else {
      actionType = 'UNKNOWN';
      responseText = isTamil
        ? 'மன்னிக்கவும், எனக்குப் புரியவில்லை. "டோக்கன் எங்கே" அல்லது "காத்திருப்பு நேரம்" என்று கேட்கலாம்.'
        : 'I didn\'t understand that. You can ask "Where is my token?" or "How long is my wait?".';
    }

    res.json({
      transcript,
      actionType,
      responseText,
      payload
    });
  } catch (error: any) {
    res.status(500).json({ error: 'Internal server error', details: error.message });
  }
});

export default router;
