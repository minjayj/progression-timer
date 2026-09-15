package com.example.progressiontimer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progressiontimer.ui.MainAppScreen
import com.example.progressiontimer.ui.TimerViewModel
import com.example.progressiontimer.ui.theme.ProgressionTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ProgressionTimerTheme {
                val viewModel = viewModel<TimerViewModel>()
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
