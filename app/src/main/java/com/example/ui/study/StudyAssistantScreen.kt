package com.example.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.markdown.MarkdownView
import com.example.ui.theme.AnonCyan
import com.example.ui.theme.AnonDarkBorder
import com.example.ui.theme.AnonDarkCard
import com.example.ui.theme.AnonError
import com.example.ui.theme.AnonSuccess
import com.example.ui.theme.AnonViolet
import com.example.viewmodel.AnonbotViewModel
import com.example.viewmodel.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAssistantScreen(
    viewModel: AnonbotViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppDestination.CHAT)
    }

    val selectedSubject by viewModel.studySubject.collectAsState()
    val selectedLanguage by viewModel.studyLanguage.collectAsState()
    val selectedMode by viewModel.studyMode.collectAsState()
    val conceptInput by viewModel.studyConceptInput.collectAsState()
    val explanationResult by viewModel.studyExplanationResult.collectAsState()
    val isLoading by viewModel.isStudyLoading.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val userAnswers by viewModel.userQuizAnswers.collectAsState()
    val isQuizSubmitted by viewModel.isQuizSubmitted.collectAsState()

    val subjects = listOf("Physics", "Chemistry", "Biology", "Mathematics")
    val languages = listOf("English", "Urdu", "Roman Urdu", "Hindi")
    val modes = listOf("Concept", "MCQ Quiz", "Questions")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Study Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ANONBOT Scientific Mastery",
                            fontSize = 11.sp,
                            color = AnonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("study_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.CHAT) },
                        modifier = Modifier.testTag("study_back_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Chat",
                            tint = AnonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode Tabs (Concept Explainer / MCQ Quiz / Exam Questions)
            TabRow(
                selectedTabIndex = modes.indexOf(selectedMode).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AnonCyan
            ) {
                modes.forEach { mode ->
                    Tab(
                        selected = selectedMode == mode,
                        onClick = { viewModel.studyMode.value = mode },
                        text = {
                            Text(
                                text = mode,
                                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Subject & Language Selectors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Subjects row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { viewModel.studySubject.value = subj },
                            label = { Text(text = subj, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AnonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AnonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedSubject == subj,
                                selectedBorderColor = AnonCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Languages row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { viewModel.studyLanguage.value = lang },
                            label = { Text(text = lang, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AnonViolet.copy(alpha = 0.2f),
                                selectedLabelColor = AnonViolet
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedLanguage == lang,
                                selectedBorderColor = AnonViolet
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Body Content based on Mode
            when (selectedMode) {
                "Concept", "Questions" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = conceptInput,
                            onValueChange = { viewModel.studyConceptInput.value = it },
                            label = {
                                Text(if (selectedMode == "Concept") "Enter Concept or Topic" else "Topic for Exam Questions")
                            },
                            placeholder = {
                                Text("e.g. Newton's 3rd Law, Photosynthesis, Thermodynamics...")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study_topic_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AnonCyan,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.requestConceptExplanation() },
                            enabled = conceptInput.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AnonCyan,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_study_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ANONBOT is synthesizing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Explanation in $selectedLanguage", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        explanationResult?.let { text ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(AnonCyan.copy(alpha = 0.4f), AnonViolet.copy(alpha = 0.4f)))
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = AnonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$selectedSubject • $selectedLanguage",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AnonCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    MarkdownView(
                                        text = text,
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                "MCQ Quiz" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = conceptInput,
                                onValueChange = { viewModel.studyConceptInput.value = it },
                                label = { Text("Quiz Topic") },
                                placeholder = { Text("e.g. Electromagnetism, Cell Division") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_topic_input")
                            )

                            Button(
                                onClick = { viewModel.requestStudyQuiz() },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AnonViolet),
                                modifier = Modifier.testTag("start_quiz_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Start Quiz", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (quizQuestions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Quiz,
                                        contentDescription = null,
                                        tint = AnonViolet,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Generate Interactive MCQs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap 'Start Quiz' above to generate questions for $selectedSubject in $selectedLanguage.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Quiz Questions List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(quizQuestions) { qIndex, question ->
                                    val selectedOption = userAnswers[qIndex]

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Question ${qIndex + 1}: ${question.question}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            question.options.forEachIndexed { optIndex, optionText ->
                                                val isSelected = selectedOption == optIndex
                                                val isCorrect = optIndex == question.correctIndex

                                                val backgroundColor = when {
                                                    isQuizSubmitted && isCorrect -> AnonSuccess.copy(alpha = 0.2f)
                                                    isQuizSubmitted && isSelected && !isCorrect -> AnonError.copy(alpha = 0.2f)
                                                    isSelected -> AnonCyan.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                }

                                                val borderColor = when {
                                                    isQuizSubmitted && isCorrect -> AnonSuccess
                                                    isQuizSubmitted && isSelected && !isCorrect -> AnonError
                                                    isSelected -> AnonCyan
                                                    else -> Color.Transparent
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(backgroundColor)
                                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                                        .clickable(enabled = !isQuizSubmitted) {
                                                            viewModel.selectQuizAnswer(qIndex, optIndex)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) AnonCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = ('A'.code + optIndex).toChar().toString(),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = optionText,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // Explanation after submission
                                            if (isQuizSubmitted && question.explanation.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Explanation: ${question.explanation}",
                                                    fontSize = 12.sp,
                                                    color = AnonCyan,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    if (!isQuizSubmitted) {
                                        Button(
                                            onClick = { viewModel.submitQuiz() },
                                            enabled = userAnswers.size == quizQuestions.size,
                                            colors = ButtonDefaults.buttonColors(containerColor = AnonSuccess),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("submit_quiz_button")
                                        ) {
                                            Text("Submit Answers & Check Score", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    } else {
                                        val correctCount = quizQuestions.indices.count { idx ->
                                            userAnswers[idx] == quizQuestions[idx].correctIndex
                                        }
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = AnonSuccess.copy(alpha = 0.15f)
                                            ),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.horizontalGradient(listOf(AnonSuccess, AnonSuccess))
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Quiz Completed!",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = AnonSuccess
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Score: $correctCount / ${quizQuestions.size}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
