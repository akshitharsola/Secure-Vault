// app/src/main/java/com/securevault/ui/screens/settings/SettingsScreen.kt
package com.securevault.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.securevault.di.AppModule
import com.securevault.ui.components.BackupDialog
import com.securevault.ui.components.ColorPicker
import com.securevault.ui.components.PinSetupDialog
import com.securevault.ui.components.RestoreDialog
import com.securevault.utils.ThemeManager
import com.securevault.utils.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current
    val biometricHelper = AppModule.provideBiometricHelper(context)
    val updateManager = AppModule.provideUpdateManager(context)
    val scope = rememberCoroutineScope()

    // State variables
    val themeConfig by viewModel.themeConfig.collectAsState()
    val tempThemeConfig by viewModel.tempThemeConfig.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isBackupLoading by viewModel.isBackupLoading.collectAsState()
    val isRestoreLoading by viewModel.isRestoreLoading.collectAsState()
    val selectedBackupUri by viewModel.selectedBackupUri.collectAsState()
    val selectedBackupFileName by viewModel.selectedBackupFileName.collectAsState()

    // Update states
    val updateInfo by updateManager.updateInfo
    val isCheckingForUpdates by updateManager.isCheckingForUpdates

    // Expandable sections state
    var isBasicThemeExpanded by remember { mutableStateOf(false) }
    var isAdvancedThemeExpanded by remember { mutableStateOf(false) }
    var isSecurityExpanded by remember { mutableStateOf(false) }
    var isBackupExpanded by remember { mutableStateOf(false) }
    var isAppInfoExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBiometricDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // File picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Extract filename from URI (simplified)
            val fileName = it.path?.substringAfterLast("/") ?: "backup.backup"
            viewModel.selectBackupFile(it, fileName)
        }
    }

    // Check if biometric is available
    val canUseBiometric = remember {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        result == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Helper function to open URLs
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle messages
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccessMessage()
        }
    }

    // Handle back button when there are unsaved changes
    val onBackPressed: () -> Unit = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settings")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Show unsaved changes indicator
            if (hasUnsavedChanges) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "You have unsaved theme changes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Basic Theme Section
            SettingsCard(
                title = "Basic Theme",
                icon = Icons.Default.Palette,
                isExpanded = isBasicThemeExpanded,
                onToggleExpand = { isBasicThemeExpanded = !isBasicThemeExpanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (tempThemeConfig.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Theme")
                    }
                    Switch(
                        checked = tempThemeConfig.isDark,
                        onCheckedChange = {
                            viewModel.toggleDarkMode()
                            // For basic theme, apply immediately
                            viewModel.applyThemeChanges(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Theme Section
            SettingsCard(
                title = "Advanced Theme",
                icon = Icons.Default.ColorLens,
                isExpanded = isAdvancedThemeExpanded,
                onToggleExpand = { isAdvancedThemeExpanded = !isAdvancedThemeExpanded }
            ) {
                // Color pickers now use tempThemeConfig and don't apply immediately
                ColorPicker(
                    selectedColor = tempThemeConfig.backgroundColor,
                    onColorSelected = { color ->
                        viewModel.updateBackgroundColor(color)
                    },
                    label = "Background Color"
                )

                ColorPicker(
                    selectedColor = tempThemeConfig.surfaceColor,
                    onColorSelected = { color ->
                        viewModel.updateSurfaceColor(color)
                    },
                    label = "Box/Card Color"
                )

                ColorPicker(
                    selectedColor = tempThemeConfig.primaryColor,
                    onColorSelected = { color ->
                        viewModel.updatePrimaryColor(color)
                    },
                    label = "Accent Color"
                )

                ColorPicker(
                    selectedColor = tempThemeConfig.textColor,
                    onColorSelected = { color ->
                        viewModel.updateTextColor(color)
                    },
                    label = "Text Color"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = { viewModel.resetTheme() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }

                    // Discard Changes Button
                    if (hasUnsavedChanges) {
                        OutlinedButton(
                            onClick = { viewModel.discardChanges() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discard")
                        }
                    }
                }

                // Save & Apply Button
                if (hasUnsavedChanges) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveAndApplyThemeChanges(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Save & Apply Changes")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Section
            SettingsCard(
                title = "Security",
                icon = Icons.Default.Security,
                isExpanded = isSecurityExpanded,
                onToggleExpand = { isSecurityExpanded = !isSecurityExpanded }
            ) {
                // Biometric toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Biometric Authentication")
                        }
                        if (!canUseBiometric) {
                            Text(
                                "Not available on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enabled ->
                            if (canUseBiometric) {
                                viewModel.setBiometricEnabled(enabled)
                                if (enabled) {
                                    showBiometricDialog = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Biometric authentication not available",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = canUseBiometric
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // PIN setup
                ListItem(
                    headlineContent = { Text("Set Fallback PIN") },
                    supportingContent = {
                        Text(if (biometricHelper.isFallbackPinSet()) "PIN is set" else "No PIN set")
                    },
                    leadingContent = {
                        Icon(Icons.Default.Pin, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPinSetupDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup & Restore Section
            SettingsCard(
                title = "Backup & Restore",
                icon = Icons.Default.CloudSync,
                isExpanded = isBackupExpanded,
                onToggleExpand = { isBackupExpanded = !isBackupExpanded }
            ) {
                // Create Backup
                ListItem(
                    headlineContent = { Text("Create Backup") },
                    supportingContent = { Text("Create encrypted backup of all passwords") },
                    leadingContent = {
                        if (isBackupLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(enabled = !isBackupLoading && !isRestoreLoading) {
                        showBackupDialog = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Restore from Backup
                ListItem(
                    headlineContent = { Text("Restore from Backup") },
                    supportingContent = {
                        Text(
                            selectedBackupFileName?.let { "Selected: $it" }
                                ?: "Select backup file to restore passwords"
                        )
                    },
                    leadingContent = {
                        if (isRestoreLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(enabled = !isBackupLoading && !isRestoreLoading) {
                        showRestoreDialog = true
                    }
                )

                // File selection info
                if (selectedBackupFileName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: $selectedBackupFileName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearSelectedBackupFile() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Info & Updates Section
            SettingsCard(
                title = "App Info & Updates",
                icon = Icons.Default.Info,
                isExpanded = isAppInfoExpanded,
                onToggleExpand = { isAppInfoExpanded = !isAppInfoExpanded }
            ) {
                // App Description
                ListItem(
                    headlineContent = { Text("About SecureVault") },
                    supportingContent = { 
                        Text("A secure, offline-first password manager with biometric authentication and encrypted backup functionality") 
                    },
                    leadingContent = {
                        Icon(Icons.Default.Security, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open README")
                    },
                    modifier = Modifier.clickable {
                        openUrl("https://github.com/akshitharsola/SecureVault-Android/blob/master/README.md")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Key Features
                ListItem(
                    headlineContent = { Text("Key Features") },
                    supportingContent = { 
                        Text("• AES encryption for all data\n• Biometric authentication\n• Encrypted backups\n• Material 3 design\n• No internet permissions") 
                    },
                    leadingContent = {
                        Icon(Icons.Default.Star, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Features")
                    },
                    modifier = Modifier.clickable {
                        openUrl("https://github.com/akshitharsola/SecureVault-Android#features")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Current Version
                ListItem(
                    headlineContent = { Text("Current Version") },
                    supportingContent = { Text("v${updateInfo.currentVersion}") },
                    leadingContent = {
                        Icon(Icons.Default.AppSettingsAlt, contentDescription = null)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Developer Info
                ListItem(
                    headlineContent = { Text("Developer") },
                    supportingContent = { Text("Akshit Harsola (@akshitharsola)") },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open GitHub Profile")
                    },
                    modifier = Modifier.clickable {
                        openUrl("https://github.com/akshitharsola")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Project Info
                ListItem(
                    headlineContent = { Text("Project License") },
                    supportingContent = { Text("MIT License - Open Source Project") },
                    leadingContent = {
                        Icon(Icons.Default.Code, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open License")
                    },
                    modifier = Modifier.clickable {
                        openUrl("https://github.com/akshitharsola/SecureVault-Android/blob/master/LICENSE")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Built With
                ListItem(
                    headlineContent = { Text("Built With") },
                    supportingContent = { 
                        Text("Kotlin • Jetpack Compose • Room Database • Clean Architecture") 
                    },
                    leadingContent = {
                        Icon(Icons.Default.Build, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Project")
                    },
                    modifier = Modifier.clickable {
                        openUrl("https://github.com/akshitharsola/SecureVault-Android#technologies-used")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Check for Updates
                ListItem(
                    headlineContent = { Text("Check for Updates") },
                    supportingContent = {
                        when {
                            isCheckingForUpdates -> Text("Checking for updates...")
                            updateInfo.isUpdateAvailable -> Text("Update available: v${updateInfo.latestVersion}")
                            else -> Text("You're using the latest version")
                        }
                    },
                    leadingContent = {
                        if (isCheckingForUpdates) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                if (updateInfo.isUpdateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (updateInfo.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingContent = {
                        if (updateInfo.isUpdateAvailable) {
                            Button(
                                onClick = { showUpdateDialog = true }
                            ) {
                                Text("Update")
                            }
                        }
                    },
                    modifier = Modifier.clickable(enabled = !isCheckingForUpdates) {
                        scope.launch {
                            updateManager.checkForUpdates()
                        }
                    }
                )

                // Update notification if available
                if (updateInfo.isUpdateAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "New version available!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Version ${updateInfo.latestVersion} is now available for download.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved theme changes. Do you want to discard them?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.discardChanges()
                        showDiscardDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            icon = {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Biometric Authentication Enabled") },
            text = { Text("You will now be required to authenticate using your biometric data to access the app and view passwords.") },
            confirmButton = {
                TextButton(onClick = { showBiometricDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinSet = { pin ->
                biometricHelper.setFallbackPin(pin)
                Toast.makeText(context, "Fallback PIN set successfully", Toast.LENGTH_SHORT).show()
                showPinSetupDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onBackup = { password, deleteAfterBackup ->
                viewModel.createBackup(password, deleteAfterBackup)
                showBackupDialog = false
            },
            isLoading = isBackupLoading,
            backupLocationInfo = viewModel.getBackupLocationDescription()
        )
    }

    if (showRestoreDialog) {
        RestoreDialog(
            onDismiss = { showRestoreDialog = false },
            onSelectFile = {
                // Launch document picker for backup files
                documentPickerLauncher.launch(arrayOf("*/*"))
            },
            onRestore = { password, replaceAll ->
                viewModel.restoreBackup(password, replaceAll)
                showRestoreDialog = false
            },
            selectedFileName = selectedBackupFileName,
            isLoading = isRestoreLoading
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("A new version (v${updateInfo.latestVersion}) is available for download.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Text(
                            text = "What's new:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateManager.downloadUpdate(updateInfo.downloadUrl)
                        showUpdateDialog = false
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}