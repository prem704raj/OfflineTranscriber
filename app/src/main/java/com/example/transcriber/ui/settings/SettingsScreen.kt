package com.example.transcriber.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcriber.R
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.billing.BillingUiState
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.settings.AppSettings
import com.example.transcriber.settings.LanguageCatalog
import com.example.transcriber.ui.accessibility.minimumTouchTarget
import com.example.transcriber.ui.layout.ReadingWidthContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val app = application as TranscriberApplication

    val settings: StateFlow<AppSettings> =
        app.settingsRepository
            .settings
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AppSettings()
            )

    val billingState: StateFlow<BillingUiState> =
        app.billingRepository
            .state
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BillingUiState()
            )

    fun setLanguage(code: String) {
        viewModelScope.launch {
            app.settingsRepository.setLanguage(code)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            app.billingRepository.restorePurchase()
        }
    }

    fun clearMessage() {
        app.billingRepository.clearMessage()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onModels: () -> Unit,
    onQueue: () -> Unit,
    onPaywall: () -> Unit,
    onPrivacyData: () -> Unit,
    onDiagnostics: () -> Unit,
    onAbout: () -> Unit,
    onBackupRestore: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs by viewModel.settings.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var languageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(billingState.message) {
        billingState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumTouchTarget()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pro Lifetime Entitlement Card
                if (billingState.entitlement == Entitlement.PRO) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Offline Transcriber Pro",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Lifetime unlocked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    // Free User - Pro Promo Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Upgrade to Pro",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "Unlock Accurate model, Subtitle studio & more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onPaywall,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.minimumTouchTarget()
                            ) {
                                Text("See Pro")
                            }
                        }
                    }
                }

                // Section: Transcription
                Text(
                    text = "Transcription",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                SettingRow(
                    icon = Icons.Default.Memory,
                    title = "Whisper models",
                    subtitle = "Fast, Balanced, and Accurate models",
                    onClick = onModels
                )

                SettingRow(
                    icon = Icons.Default.Language,
                    title = "Spoken language",
                    subtitle = LanguageCatalog.byCode(prefs.languageCode).label,
                    onClick = { languageDialog = true }
                )

                SettingRow(
                    icon = Icons.Default.Queue,
                    title = "Transcription queue",
                    subtitle = "Manage queued and completed background jobs",
                    onClick = onQueue
                )

                // Section: Data & Privacy
                Text(
                    text = "Data & Privacy",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                SettingRow(
                    icon = Icons.Default.Restore,
                    title = "Backup & restore library",
                    subtitle = "Create portable encrypted backups or restore your transcripts",
                    onClick = onBackupRestore
                )

                SettingRow(
                    icon = Icons.Default.Security,
                    title = "Privacy & data controls",
                    subtitle = "Clean temporary files, delete data, or reset preferences",
                    onClick = onPrivacyData
                )

                SettingRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy policy",
                    subtitle = "Read our full offline privacy commitment",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.privacy_policy_url))
                        )
                        context.startActivity(intent)
                    }
                )

                SettingRow(
                    icon = Icons.Default.QueryStats,
                    title = "Local diagnostics",
                    subtitle = "Review system specs, model metrics, and share debug info",
                    onClick = onDiagnostics
                )

                SettingRow(
                    icon = Icons.Default.Restore,
                    title = "Restore purchase",
                    subtitle = "Restore Pro entitlement from Google Play",
                    onClick = { viewModel.restorePurchases() }
                )

                // Section: About
                Text(
                    text = "Application",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                SettingRow(
                    icon = Icons.Default.Info,
                    title = "About Offline Transcriber",
                    subtitle = "Version, architecture, and open-source licenses",
                    onClick = onAbout
                )
            }
        }
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text("Transcription language") },
            text = {
                Column {
                    LanguageCatalog.all.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(language.code)
                                    languageDialog = false
                                }
                                .minimumTouchTarget(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.languageCode == language.code,
                                onClick = {
                                    viewModel.setLanguage(language.code)
                                    languageDialog = false
                                }
                            )
                            Text(
                                language.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { languageDialog = false },
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .minimumTouchTarget(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
