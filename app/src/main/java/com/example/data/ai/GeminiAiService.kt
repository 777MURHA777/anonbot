package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiAiService"

data class StudyQuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class GeminiAiService {

    private val systemPrompt = """
        You are ANONBOT, a professional personal AI assistant. 
        Your tagline is: "Your Intelligence. Your Assistant."
        Always maintain this identity. Never refer to yourself as NOVA AI or any other name.
        You are knowledgeable, articulate, concise when appropriate, and deeply analytical.
        You support multiple languages seamlessly including English, Urdu, Roman Urdu, and Hindi.
        Provide answers formatted in rich Markdown with headers, bold text, lists, and code blocks with syntax tags.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) }
        )
    }

    /**
     * Stream conversation response chunk by chunk for rapid feedback.
     */
    fun streamChat(
        history: List<Pair<String, String>>, // role to content
        newMessage: String,
        bitmapAttachment: Bitmap? = null
    ): Flow<String> = kotlinx.coroutines.flow.channelFlow {
        withTimeout(60_000L) {
            val contentList = mutableListOf<com.google.firebase.ai.type.Content>()
            
            // Build conversation history
            for ((role, text) in history) {
                contentList.add(
                    content(if (role == "user") "user" else "model") {
                        text(text)
                    }
                )
            }

            // Current prompt
            val currentContent = content("user") {
                if (bitmapAttachment != null) {
                    image(bitmapAttachment)
                }
                text(newMessage)
            }

            contentList.add(currentContent)

            model.generateContentStream(contentList).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    send(chunkText)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generate study concept explanation in selected subject and language.
     */
    suspend fun explainConcept(
        subject: String,
        concept: String,
        language: String
    ): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000L) {
                val prompt = """
                    Explain the following concept in $subject in detail:
                    Concept: "$concept"
                    Language: $language (If Urdu or Roman Urdu or Hindi, provide clear accurate phrasing with key scientific terms).
                    Format with:
                    1. Core Definition
                    2. Real-World Analogy
                    3. Key Principles & Formulas (if any)
                    4. Practical Applications & Summary
                """.trimIndent()
                val response = model.generateContent(prompt)
                response.text ?: "No explanation returned by ANONBOT."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Concept explanation failed", e)
            throw e
        }
    }

    /**
     * Generate interactive MCQs for study testing.
     */
    suspend fun generateStudyQuiz(
        subject: String,
        topic: String,
        language: String,
        questionCount: Int = 4
    ): List<StudyQuizQuestion> = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000L) {
                val prompt = """
                    Generate $questionCount multiple-choice questions (MCQs) for the subject '$subject' on the topic '$topic'.
                    Language: $language.
                    Output MUST be a valid JSON array of objects with the exact keys:
                    [
                      {
                        "question": "Question text",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctIndex": 0,
                        "explanation": "Why this is correct"
                      }
                    ]
                    Do not add introductory conversational text. Output ONLY the raw JSON array.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val rawText = response.text?.trim() ?: "[]"
                
                // Extract json array if wrapped in code fence
                val jsonString = if (rawText.startsWith("```")) {
                    rawText.substringAfter("\n").substringBeforeLast("```").trim()
                } else {
                    rawText
                }

                val questions = mutableListOf<StudyQuizQuestion>()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val qText = obj.getString("question")
                    val optsArray = obj.getJSONArray("options")
                    val opts = mutableListOf<String>()
                    for (j in 0 until optsArray.length()) {
                        opts.add(optsArray.getString(j))
                    }
                    val correct = obj.getInt("correctIndex")
                    val expl = obj.optString("explanation", "")
                    questions.add(
                        StudyQuizQuestion(
                            id = "q_${System.currentTimeMillis()}_$i",
                            question = qText,
                            options = opts,
                            correctIndex = correct,
                            explanation = expl
                        )
                    )
                }
                questions
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quiz generation failed", e)
            // Fallback default sample questions if network/parsing error
            listOf(
                StudyQuizQuestion(
                    id = "fallback_1",
                    question = "What is the primary unit of force in Physics?",
                    options = listOf("Newton (N)", "Joule (J)", "Watt (W)", "Pascal (Pa)"),
                    correctIndex = 0,
                    explanation = "Newton is the SI unit of force, named after Isaac Newton (1 N = 1 kg·m/s²)."
                ),
                StudyQuizQuestion(
                    id = "fallback_2",
                    question = "Which cell organelle is known as the powerhouse of the cell?",
                    options = listOf("Nucleus", "Ribosome", "Mitochondria", "Golgi apparatus"),
                    correctIndex = 2,
                    explanation = "Mitochondria generate most of the cell's ATP chemical energy."
                )
            )
        }
    }

    /**
     * Analyze document/PDF text for summaries or answering questions.
     */
    suspend fun analyzeDocument(
        documentText: String,
        action: String, // "summarize", "qa", "study_questions"
        question: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(60_000L) {
                val prompt = when (action) {
                    "summarize" -> """
                        You are ANONBOT PDF & Document Assistant.
                        Please provide an executive summary of this document:
                        - Key Takeaways & Main Objectives
                        - Bullet point highlights
                        - Important conclusions
                        Document:
                        ${documentText.take(12000)}
                    """.trimIndent()

                    "study_questions" -> """
                        You are ANONBOT PDF & Document Assistant.
                        Based on the following document content, create 5 comprehensive study questions (with brief model answers):
                        Document:
                        ${documentText.take(12000)}
                    """.trimIndent()

                    else -> """
                        You are ANONBOT PDF & Document Assistant.
                        Answer the user's question accurately based on the document text provided:
                        User Question: "$question"
                        
                        Document Content:
                        ${documentText.take(12000)}
                    """.trimIndent()
                }

                val response = model.generateContent(prompt)
                response.text ?: "ANONBOT could not analyze the document."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Document analysis failed", e)
            throw e
        }
    }
}
