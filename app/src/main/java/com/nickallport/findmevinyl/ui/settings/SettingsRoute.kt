package com.nickallport.findmevinyl.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.settings.Settings
import kotlinx.coroutines.launch

/**
 * The settings page with the Your data section working. [backUp] and [restore] do the whole job,
 * including asking the user for a file, and return what to tell the user, or null when the user backed
 * out of the file picker. While one runs both buttons wait. The last message is kept over a rotation.
 */
@Composable
fun SettingsRoute(
    settings: Settings,
    onChange: (Settings) -> Unit,
    backUp: (suspend () -> String?)?,
    restore: (suspend () -> String?)?,
    modifier: Modifier = Modifier
) {
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run(action: suspend () -> String?): () -> Unit = {
        scope.launch {
            busy = true
            try {
                action()?.let { message = it }
            } finally {
                busy = false
            }
        }
    }

    SettingsScreen(
        settings = settings,
        onChange = onChange,
        modifier = modifier,
        onBackUp = backUp?.let { run(it) },
        onRestore = restore?.let { run(it) },
        dataMessage = message,
        busy = busy
    )
}
