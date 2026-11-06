package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.data.ai.configureAppCheck
import com.example.ui.chat.ChatScreen
import com.example.ui.drawer.AppDrawerContent
import com.example.ui.pdf.PdfAssistantScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.study.StudyAssistantScreen
import com.example.ui.theme.AnonbotTheme
import com.example.ui.voice.VoiceAssistantScreen
import com.example.viewmodel.AnonbotViewModel
import com.example.viewmodel.AppDestination
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AnonbotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            AnonbotTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AnonbotViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentScreen by viewModel.currentScreen.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawerContent(
                    viewModel = viewModel,
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        val openDrawerAction: () -> Unit = {
            scope.launch { drawerState.open() }
        }

        when (currentScreen) {
            AppDestination.CHAT -> {
                ChatScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
            AppDestination.STUDY -> {
                StudyAssistantScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
            AppDestination.PDF -> {
                PdfAssistantScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
            AppDestination.VOICE -> {
                VoiceAssistantScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
            AppDestination.SETTINGS -> {
                SettingsScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
            AppDestination.PROFILE -> {
                ProfileScreen(viewModel = viewModel, onOpenDrawer = openDrawerAction)
            }
        }
    }
}
