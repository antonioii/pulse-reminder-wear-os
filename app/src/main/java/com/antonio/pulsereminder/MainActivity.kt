package com.antonio.pulsereminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.antonio.pulsereminder.data.SettingsRepository
import com.antonio.pulsereminder.ui.ReminderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(this)
        setContent {
            ReminderApp(settingsRepository = settingsRepository)
        }
    }
}

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun PulseReminderPreview() {
    ReminderApp(settingsRepository = null)
}
