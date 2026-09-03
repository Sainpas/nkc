package com.sainpas.nkc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun NkcApp(viewModel: NkcViewModel) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("NKC") }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("APK version: ${state.appVersion}", style = MaterialTheme.typography.titleMedium)
            Text("Content version: ${state.content.version ?: "none"}")
            Text("Content source: ${state.content.source}")
            Text("Update status: ${state.status}")
            Button(onClick = viewModel::checkForUpdates) { Text("Check for updates") }
            HorizontalDivider()
            Text("Remote content test", style = MaterialTheme.typography.titleMedium)
            Text(state.content.config?.get("welcomeMessage")?.toString()?.trim('"')
                ?: "No downloaded content yet. Built-in fallback remains available offline.")
        }
    }
}
