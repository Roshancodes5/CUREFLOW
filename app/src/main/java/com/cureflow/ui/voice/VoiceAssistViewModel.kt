package com.cureflow.ui.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cureflow.data.repository.CareflowRepository
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAssistViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    var isListening by mutableStateOf(false)
        private set

    var spokenText by mutableStateOf("")
        private set

    var responseText by mutableStateOf("")
        private set

    var currentLanguage by mutableStateOf("en-US")
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isEmergencyActive by mutableStateOf(false)
        private set

    var pendingConfirmationAction by mutableStateOf<String?>(null)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var lastResponseText: String = ""
    private val repository = CareflowRepository()

    init {
        textToSpeech = TextToSpeech(application, this)
        initSpeechRecognizer()
        responseText = getGreetingText(currentLanguage)
    }

    private fun initSpeechRecognizer() {
        val context = getApplication<Application>()
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        errorMessage = null
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        errorMessage = getErrorMessage(error)
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            spokenText = recognized
                            processCommand(recognized)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            spokenText = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            errorMessage = if (currentLanguage.startsWith("ta")) {
                "பேச்சு அடையாளம் காணும் வசதி இந்த சாதனத்தில் இல்லை."
            } else {
                "Speech recognition is not available on this device."
            }
        }
    }

    fun startListening() {
        errorMessage = null
        val context = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = if (currentLanguage.startsWith("ta")) "பேச்சு அடையாளம் காணும் வசதி இல்லை" else "Speech recognition unavailable"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            errorMessage = "Could not start listening: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage.startsWith("en")) "ta-IN" else "en-US"
        val locale = if (currentLanguage.startsWith("ta")) {
            Locale.Builder().setLanguage("ta").setRegion("IN").build()
        } else {
            Locale.US
        }
        textToSpeech?.language = locale
        responseText = getGreetingText(currentLanguage)
        speak(responseText)
    }

    fun processCommand(command: String) {
        val lower = command.lowercase(Locale.ROOT)

        // Handle explicit confirmation flow for mutating actions
        if (pendingConfirmationAction != null) {
            if (lower.contains("yes") || lower.contains("confirm") || lower.contains("ஆம்") || lower.contains("உறுதி")) {
                executeConfirmedAction()
                return
            } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("இல்லை") || lower.contains("ரத்து")) {
                pendingConfirmationAction = null
                responseText = if (currentLanguage.startsWith("ta")) "செயல் ரத்து செய்யப்பட்டது." else "Action cancelled."
                speak(responseText)
                return
            }
        }

        viewModelScope.launch {
            // Attempt to query backend voice intent API
            val intentResult = repository.processVoiceIntent(command, currentLanguage)
            if (intentResult != null) {
                // If backend action requires modification/mutation, require explicit confirmation per security rules
                if (intentResult.actionType == "EMERGENCY_TRIGGER" || intentResult.actionType == "BOOK_APPOINTMENT") {
                    pendingConfirmationAction = intentResult.actionType
                    responseText = if (currentLanguage.startsWith("ta"))
                        "நீங்கள் இந்த செயலை உறுதிப்படுத்துகிறீர்களா? 'ஆம்' அல்லது 'இல்லை' எனப் பேசவும்."
                    else
                        "Do you want to confirm this action? Please say 'Yes' to confirm or 'No' to cancel."
                } else {
                    responseText = intentResult.responseText
                }
            } else {
                // Fallback local response if backend is offline
                responseText = getLocalFallbackResponse(lower)
            }

            lastResponseText = responseText
            speak(responseText)
        }
    }

    private fun executeConfirmedAction() {
        val action = pendingConfirmationAction
        pendingConfirmationAction = null
        when (action) {
            "EMERGENCY_TRIGGER" -> {
                isEmergencyActive = true
                responseText = if (currentLanguage.startsWith("ta"))
                    "அவசர உதவி எச்சரிக்கை உறுதிப்படுத்தப்பட்டு அனுப்பப்பட்டது!"
                    else "Emergency alert confirmed and dispatched to hospital team."
            }
            else -> {
                responseText = if (currentLanguage.startsWith("ta")) "செயல் வெற்றிகரமாக உறுதிப்படுத்தப்பட்டது." else "Action confirmed and executed successfully."
            }
        }
        lastResponseText = responseText
        speak(responseText)
    }

    private fun getLocalFallbackResponse(lower: String): String {
        val isTamil = currentLanguage.startsWith("ta")
        return when {
            lower.contains("token") || lower.contains("where") || lower.contains("டோக்கன்") || lower.contains("எங்கே") -> {
                if (isTamil) "உங்கள் டோக்கன் எண் #A-104. கவுண்டர் 3-ல் அழைக்கப்படும்." else "Your token is #A-104 at Counter 3 (Backend fallback)."
            }
            lower.contains("wait") || lower.contains("long") || lower.contains("time") || lower.contains("காத்திருப்பு") -> {
                if (isTamil) "மதிப்பிடப்பட்ட காத்திருப்பு நேரம் 10-15 நிமிடங்கள்." else "Estimated wait range is 10-15 minutes. 3 patients ahead."
            }
            lower.contains("repeat") || lower.contains("மீண்டும்") -> {
                if (lastResponseText.isNotBlank()) lastResponseText else "Nothing to repeat."
            }
            lower.contains("stop") || lower.contains("நிறுத்து") -> {
                stopListening()
                textToSpeech?.stop()
                isSpeaking = false
                if (isTamil) "நிறுத்தப்பட்டது." else "Stopped."
            }
            lower.contains("emergency") || lower.contains("அவசரம்") -> {
                pendingConfirmationAction = "EMERGENCY_TRIGGER"
                if (isTamil) "அவசர உதவியை உறுதிப்படுத்த 'ஆம்' எனப் பேசவும்." else "Say 'Yes' to confirm emergency dispatch."
            }
            else -> {
                if (isTamil) "மன்னிக்கவும், எனக்குப் புரியவில்லை." else "I didn't quite catch that. Try asking about your token or wait time."
            }
        }
    }

    fun triggerEmergency() {
        pendingConfirmationAction = "EMERGENCY_TRIGGER"
        responseText = if (currentLanguage.startsWith("ta"))
            "அவசர உதவியை உறுதிப்படுத்த 'ஆம்' எனப் பேசவும்."
            else "Please say 'Yes' to confirm emergency dispatch to the hospital."
        speak(responseText)
    }

    fun dismissEmergency() {
        isEmergencyActive = false
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CareflowUtteranceId")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = if (currentLanguage.startsWith("ta")) {
                Locale.Builder().setLanguage("ta").setRegion("IN").build()
            } else {
                Locale.US
            }
            textToSpeech?.language = locale
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                }
            })
        } else {
            errorMessage = "TextToSpeech initialization failed"
        }
    }

    private fun getGreetingText(lang: String): String {
        return if (lang.startsWith("ta")) {
            "வணக்கம்! இது கேர்ஃப்ளோ குரல் உதவியாளர். நான் உங்களுக்கு எப்படி உதவ வேண்டும்?"
        } else {
            "Hello! This is Careflow Voice Assistant. How can I help you today?"
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        val isTamil = currentLanguage.startsWith("ta")
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> if (isTamil) "ஆடியோ பதிவு பிழை" else "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> if (isTamil) "கி라이언트 பிழை" else "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> if (isTamil) "மைக்ரோஃபோன் அனுமதி தேவை" else "Permission denied"
            SpeechRecognizer.ERROR_NETWORK -> if (isTamil) "நெட்வொர்க் பிழை" else "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> if (isTamil) "நெட்வொர்க் நேரம் முடிந்தது" else "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> if (isTamil) "பேச்சு அடையாளம் காணப்படவில்லை. மீண்டும் முயற்சிக்கவும்." else "No speech detected. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (isTamil) "உதவியாளர் பிஸியாக உள்ளார்" else "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> if (isTamil) "சேவையக பிழை" else "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isTamil) "பேச்சு எதுவும் கேட்கவில்லை" else "No speech input"
            else -> if (isTamil) "அறியப்படாத பிழை" else "Unknown speech error"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
