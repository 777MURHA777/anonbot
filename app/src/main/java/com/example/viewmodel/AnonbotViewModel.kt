package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.ai.StudyQuizQuestion
import com.example.data.db.AnonbotDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.voice.VoiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "AnonbotViewModel"

enum class AppDestination {
    CHAT,
    STUDY,
    PDF,
    VOICE,
    SETTINGS,
    PROFILE
}

class AnonbotViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnonbotDatabase.getDatabase(application)
    private val dao = db.dao()
    val aiService = GeminiAiService()
    val voiceManager = VoiceManager(application)

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppDestination.CHAT)
    val currentScreen: StateFlow<AppDestination> = _currentScreen.asStateFlow()

    // Sessions & Active Session
    val allSessions: StateFlow<List<ChatSessionEntity>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    // Chat Input State
    val chatInputText = MutableStateFlow("")
    val attachedImage = MutableStateFlow<Bitmap?>(null)
    val isMarkdownPreviewEnabled = MutableStateFlow(false)

    // AI Generation & Streaming
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Study Assistant State
    val studySubject = MutableStateFlow("Physics")
    val studyLanguage = MutableStateFlow("English")
    val studyMode = MutableStateFlow("Concept") // "Concept", "MCQ Quiz", "Questions"
    val studyConceptInput = MutableStateFlow("")
    val studyExplanationResult = MutableStateFlow<String?>(null)
    val isStudyLoading = MutableStateFlow(false)
    val quizQuestions = MutableStateFlow<List<StudyQuizQuestion>>(emptyList())
    val userQuizAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap()) // question index to selected option
    val isQuizSubmitted = MutableStateFlow(false)

    // PDF / Document Assistant State
    val docTitle = MutableStateFlow("Physics_Fundamentals_Chapter1.pdf")
    val docContent = MutableStateFlow(
        """
        Chapter 1: Mechanics and Laws of Motion
        Physics is the foundational science studying matter, motion, energy, and force.
        Newton's First Law states that an object will remain at rest or in uniform motion in a straight line unless acted upon by an external net force (Law of Inertia).
        Newton's Second Law defines force as the rate of change of momentum: F = m * a (Force equals mass times acceleration).
        Newton's Third Law states that for every action, there is an equal and opposite reaction.
        Work is done when a force causes displacement: W = F * d * cos(theta). The SI unit of work is Joules (J).
        Kinetic energy is given by KE = 0.5 * m * v^2, and Potential energy in a uniform gravitational field is PE = m * g * h.
        Conservation of Energy dictates that the total energy in an isolated system remains constant over time.
        """.trimIndent()
    )
    val docUserQuestion = MutableStateFlow("")
    val docAnalysisResult = MutableStateFlow<String?>(null)
    val isDocLoading = MutableStateFlow(false)

    // App Preferences
    val isDarkTheme = MutableStateFlow(true)
    val userName = MutableStateFlow("Explorer")
    val defaultLanguage = MutableStateFlow("English")

    init {
        // Observe sessions and initialize first session if none exists
        viewModelScope.launch {
            allSessions.collectLatest { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentScreen.value = destination
    }

    fun startNewChat(title: String = "New Chat") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val session = ChatSessionEntity(
                id = newId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertSession(session)
            selectSession(newId)
            chatInputText.value = ""
            attachedImage.value = null
            _streamingText.value = null
            _errorMessage.value = null
            _currentScreen.value = AppDestination.CHAT
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            dao.getMessagesForSession(sessionId).collectLatest { list ->
                _messages.value = list
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = dao.getSessionById(sessionId)
            if (session != null) {
                dao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            dao.deleteSessionById(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAllMessages()
            dao.clearAllSessions()
            _currentSessionId.value = null
            _messages.value = emptyList()
        }
    }

    fun sendMessage(textOverride: String? = null) {
        val messageText = (textOverride ?: chatInputText.value).trim()
        if (messageText.isBlank()) return

        val img = attachedImage.value
        chatInputText.value = ""
        attachedImage.value = null
        isMarkdownPreviewEnabled.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            // Ensure session exists
            var sId = _currentSessionId.value
            if (sId == null) {
                sId = UUID.randomUUID().toString()
                val sessionTitle = if (messageText.length > 28) messageText.take(28) + "..." else messageText
                val newSession = ChatSessionEntity(id = sId, title = sessionTitle)
                dao.insertSession(newSession)
                _currentSessionId.value = sId
            }

            // Insert User Message into Room DB
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sId,
                role = "user",
                content = messageText,
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(userMsg)

            // Update session timestamp & title if default
            val currentSession = dao.getSessionById(sId)
            if (currentSession != null) {
                val updatedTitle = if (currentSession.title == "New Chat") {
                    if (messageText.length > 28) messageText.take(28) + "..." else messageText
                } else currentSession.title
                dao.updateSession(currentSession.copy(title = updatedTitle, updatedAt = System.currentTimeMillis()))
            }

            // Prepare history for Gemini
            val currentHistory = _messages.value.map { it.role to it.content }

            _isGenerating.value = true
            _streamingText.value = ""

            val accumulatedResponse = StringBuilder()

            try {
                aiService.streamChat(
                    history = currentHistory,
                    newMessage = messageText,
                    bitmapAttachment = img
                ).collect { chunk ->
                    accumulatedResponse.append(chunk)
                    _streamingText.value = accumulatedResponse.toString()
                }

                val finalAiResponse = accumulatedResponse.toString()
                if (finalAiResponse.isNotBlank()) {
                    val aiMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sId,
                        role = "model",
                        content = finalAiResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    dao.insertMessage(aiMsg)
                } else {
                    _errorMessage.value = "ANONBOT returned an empty response. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                val friendlyError = "ANONBOT generation error: ${e.localizedMessage ?: "Unable to complete request."}"
                _errorMessage.value = friendlyError
                val aiMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sId,
                    role = "model",
                    content = "⚠️ $friendlyError",
                    timestamp = System.currentTimeMillis()
                )
                dao.insertMessage(aiMsg)
            } finally {
                _isGenerating.value = false
                _streamingText.value = null
            }
        }
    }

    // Study Assistant Actions
    fun requestConceptExplanation() {
        val concept = studyConceptInput.value.trim()
        if (concept.isBlank()) return
        isStudyLoading.value = true
        studyExplanationResult.value = null

        viewModelScope.launch {
            try {
                val explanation = aiService.explainConcept(
                    subject = studySubject.value,
                    concept = concept,
                    language = studyLanguage.value
                )
                studyExplanationResult.value = explanation
            } catch (e: Exception) {
                studyExplanationResult.value = "Error generating explanation: ${e.localizedMessage}"
            } finally {
                isStudyLoading.value = false
            }
        }
    }

    fun requestStudyQuiz() {
        val topic = studyConceptInput.value.ifBlank { "Core Principles" }
        isStudyLoading.value = true
        userQuizAnswers.value = emptyMap()
        isQuizSubmitted.value = false

        viewModelScope.launch {
            try {
                val questions = aiService.generateStudyQuiz(
                    subject = studySubject.value,
                    topic = topic,
                    language = studyLanguage.value
                )
                quizQuestions.value = questions
            } catch (e: Exception) {
                Log.e(TAG, "Quiz error", e)
            } finally {
                isStudyLoading.value = false
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
        if (isQuizSubmitted.value) return
        val current = userQuizAnswers.value.toMutableMap()
        current[questionIndex] = optionIndex
        userQuizAnswers.value = current
    }

    fun submitQuiz() {
        isQuizSubmitted.value = true
    }

    // PDF / Document Assistant Actions
    fun analyzeDocument(action: String) {
        val content = docContent.value.trim()
        if (content.isBlank()) return
        isDocLoading.value = true
        docAnalysisResult.value = null

        viewModelScope.launch {
            try {
                val result = aiService.analyzeDocument(
                    documentText = content,
                    action = action,
                    question = docUserQuestion.value
                )
                docAnalysisResult.value = result
            } catch (e: Exception) {
                docAnalysisResult.value = "Document analysis failed: ${e.localizedMessage}"
            } finally {
                isDocLoading.value = false
            }
        }
    }

    // Voice Assistant Actions
    fun speakText(text: String) {
        val langCode = when (defaultLanguage.value.lowercase()) {
            "urdu" -> "ur"
            "hindi" -> "hi"
            else -> "en"
        }
        voiceManager.speak(text, langCode)
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening { spoken ->
                chatInputText.value = spoken
                if (_currentScreen.value == AppDestination.VOICE) {
                    sendMessage(spoken)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
