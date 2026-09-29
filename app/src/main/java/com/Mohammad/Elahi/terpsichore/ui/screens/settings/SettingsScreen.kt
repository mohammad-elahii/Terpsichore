package com.Mohammad.Elahi.terpsichore.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Mohammad.Elahi.terpsichore.R

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsRow(
            title = stringResource(R.string.settings_output_folder),
            value = stringResource(R.string.settings_not_set),
        )
        HorizontalDivider()
        SettingsRow(
            title = stringResource(R.string.settings_folder_template),
            value = stringResource(R.string.settings_default_template),
        )
        HorizontalDivider()
        SettingsRow(
            title = stringResource(R.string.settings_quality_floor),
            value = stringResource(R.string.settings_default_quality),
        )
    }
}

@Composable
private fun SettingsRow(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
