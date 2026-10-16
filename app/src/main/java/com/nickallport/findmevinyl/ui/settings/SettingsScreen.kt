package com.nickallport.findmevinyl.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.settings.Settings

/**
 * The settings page: yes-or-no switches in sections. Each change passes on all the settings with that
 * one changed. A whole row is the switch, so the words can be tapped too.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    /** Shows the Your data section, with a Back up button, when given. */
    onBackUp: (() -> Unit)? = null,
    /** Shows a Restore button in the Your data section, when given. */
    onRestore: (() -> Unit)? = null,
    /** What the last back up or restore said. */
    dataMessage: String? = null,
    /** A back up or restore is running, so both buttons wait. */
    busy: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Section("Discography Settings") {
            Text(
                text = "Albums you own or want are always shown. An album is hidden by a format only when " +
                    "every format it came out on is hidden. Bootlegs, promos and unknown status hide an album " +
                    "only when it has no official release.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp).testTag("settings_discography_hint")
            )
            SettingRow("Hide Live Albums", "hideLiveAlbums", settings.hideLiveAlbums) {
                onChange(settings.copy(hideLiveAlbums = it))
            }
            SettingRow("Hide Compilation Albums", "hideCompilations", settings.hideCompilations) {
                onChange(settings.copy(hideCompilations = it))
            }
            SettingRow("Hide Singles", "hideSingles", settings.hideSingles) {
                onChange(settings.copy(hideSingles = it))
            }
            SettingRow("Hide Other Releases", "hideOtherReleases", settings.hideOtherReleases) {
                onChange(settings.copy(hideOtherReleases = it))
            }
            SettingRow("Hide LPs", "hideLps", settings.hideLps) { onChange(settings.copy(hideLps = it)) }
            SettingRow("Hide CDs", "hideCds", settings.hideCds) { onChange(settings.copy(hideCds = it)) }
            SettingRow("Hide 12\"s", "hideTwelveInches", settings.hideTwelveInches) {
                onChange(settings.copy(hideTwelveInches = it))
            }
            SettingRow("Hide 7\"s", "hideSevenInches", settings.hideSevenInches) {
                onChange(settings.copy(hideSevenInches = it))
            }
            SettingRow("Hide MCs", "hideMcs", settings.hideMcs) { onChange(settings.copy(hideMcs = it)) }
            SettingRow("Hide Digital", "hideDigital", settings.hideDigital) {
                onChange(settings.copy(hideDigital = it))
            }
            SettingRow("Hide Bootlegs", "hideBootlegs", settings.hideBootlegs) {
                onChange(settings.copy(hideBootlegs = it))
            }
            SettingRow("Hide Promos", "hidePromos", settings.hidePromos) {
                onChange(settings.copy(hidePromos = it))
            }
            SettingRow("Hide Unknown Status releases", "hideUnknownStatus", settings.hideUnknownStatus) {
                onChange(settings.copy(hideUnknownStatus = it))
            }
        }

        Section("Location Settings") {
            SettingRow("Hide Charity shops", "hideCharityShops", settings.hideCharityShops) {
                onChange(settings.copy(hideCharityShops = it))
            }
            SettingRow("Hide Second-Hand shops", "hideSecondHandShops", settings.hideSecondHandShops) {
                onChange(settings.copy(hideSecondHandShops = it))
            }
        }

        if (onBackUp != null || onRestore != null) {
            Section("Your data") {
                Text(
                    text = "Back up saves your favourite artists and their order, the albums you own or want, " +
                        "your own artist photos and your settings, as one file you can keep anywhere. Restore " +
                        "only adds what is in the file: it never removes anything already on this phone, " +
                        "except that the settings in the file replace the current ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp).testTag("data_hint")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onBackUp != null) {
                        Button(onClick = onBackUp, enabled = !busy, modifier = Modifier.testTag("backup_button")) {
                            Text("Back up")
                        }
                    }
                    if (onRestore != null) {
                        FilledTonalButton(onClick = onRestore, enabled = !busy, modifier = Modifier.testTag("restore_button")) {
                            Text("Restore")
                        }
                    }
                }
                if (dataMessage != null) {
                    Text(
                        text = dataMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp).testTag("data_message")
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    content()
}

@Composable
private fun SettingRow(label: String, key: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setting_$key")
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        // The row is the switch, so this one only shows the state.
        Switch(checked = checked, onCheckedChange = null)
    }
}
