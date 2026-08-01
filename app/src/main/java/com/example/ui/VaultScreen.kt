package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: FileManagerViewModel, onExit: () -> Unit) {
    val vaultState by viewModel.vaultState.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()
    val sandboxRoot = viewModel.repository.sandboxRoot

    var isConfigured by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(vaultState) {
        viewModel.checkVaultStatus { hasConfig ->
            isConfigured = hasConfig
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Folder Vault") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, "Exit")
                    }
                },
                actions = {
                    if (vaultState == VaultState.UNLOCKED) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(Icons.Default.Lock, "Lock Vault")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isConfigured == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isConfigured == false) {
                VaultSetupView(onCreatePIN = { pin, q, a ->
                    viewModel.createVaultPIN(pin, q, a)
                    isConfigured = true
                })
            } else if (vaultState == VaultState.LOCKED) {
                VaultUnlockView(
                    viewModel = viewModel,
                    onUnlockSuccess = { isConfigured = true }
                )
            } else {
                // UNLOCKED BROWSER VIEW
                VaultBrowserView(
                    vaultFiles = vaultFiles,
                    sandboxRoot = sandboxRoot,
                    onImportFile = { path ->
                        viewModel.importFileToVault(path)
                    },
                    onRestoreFile = { file ->
                        viewModel.restoreFileFromVault(file)
                    },
                    onDeleteFile = { file ->
                        viewModel.deleteFileFromVault(file)
                    }
                )
            }
        }
    }
}

@Composable
fun VaultSetupView(onCreatePIN: (String, String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("What was the name of your first childhood pet?") }
    var answer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = "Lock",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            "Configure Private Vault PIN",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            "Lock your sensitive data inside a local, obfuscated app folder. Set a secure 4-digit PIN to secure your files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            label = { Text("4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("vault_setup_pin"),
            singleLine = true
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 4) confirmPin = it },
            label = { Text("Confirm PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("vault_setup_confirm"),
            singleLine = true
        )

        Divider()

        Text(
            "Account Recovery Question",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Recovery Security Question") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Your Answer") },
            modifier = Modifier.fillMaxWidth().testTag("vault_setup_answer"),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                if (pin.length < 4) {
                    errorMessage = "PIN must be exactly 4 digits."
                } else if (pin != confirmPin) {
                    errorMessage = "PINs do not match."
                } else if (answer.isBlank()) {
                    errorMessage = "Please specify a recovery answer."
                } else {
                    onCreatePIN(pin, question, answer)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("vault_setup_submit")
        ) {
            Text("Initialize Secure Folder")
        }
    }
}

@Composable
fun VaultUnlockView(viewModel: FileManagerViewModel, onUnlockSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var isResetting by remember { mutableStateOf(false) }
    var securityQuestion by remember { mutableStateOf("") }
    var backupAnswer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Load recovery question
    LaunchedEffect(isResetting) {
        if (isResetting) {
            viewModel.repository.sandboxRoot // trigger touch
            val db = AppDatabase.getDatabase(viewModel.getApplication())
            val config = db.vaultDao().getConfig()
            if (config != null) {
                securityQuestion = config.backupQuestion
            }
        }
    }

    if (isResetting) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reset Vault PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("Security Question: $securityQuestion", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = backupAnswer,
                onValueChange = { backupAnswer = it },
                label = { Text("Recovery Answer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4) newPin = it },
                label = { Text("New 4-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (backupAnswer.isBlank() || newPin.length < 4) {
                        errorMessage = "Complete all fields correctly."
                    } else {
                        val resetDone = viewModel.resetVaultWithBackup(backupAnswer, newPin)
                        if (resetDone) {
                            onUnlockSuccess()
                        } else {
                            errorMessage = "Incorrect answer, reset failed."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Confirm Reset")
            }

            TextButton(onClick = { isResetting = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to Unlock")
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EnhancedEncryption,
                contentDescription = "Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Enter Vault PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4) {
                        pin = it
                        if (it.length == 4) {
                            val success = viewModel.unlockVault(it)
                            if (success) {
                                onUnlockSuccess()
                            } else {
                                pin = ""
                                errorMessage = "Incorrect PIN."
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.width(180.dp).testTag("vault_unlock_pin_input"),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { isResetting = true }, modifier = Modifier.testTag("vault_forgot_pin")) {
                Text("Forgot PIN?")
            }
        }
    }
}

@Composable
fun VaultBrowserView(
    vaultFiles: List<VaultFile>,
    sandboxRoot: File,
    onImportFile: (String) -> Unit,
    onRestoreFile: (VaultFile) -> Unit,
    onDeleteFile: (VaultFile) -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var importableFiles by remember { mutableStateOf<List<File>> (emptyList()) }

    // List standard files in sandbox that can be locked
    LaunchedEffect(showImportDialog) {
        if (showImportDialog) {
            val list = mutableListOf<File>()
            fun collect(dir: File) {
                dir.listFiles()?.forEach {
                    if (it.isDirectory) collect(it) else list.add(it)
                }
            }
            collect(sandboxRoot)
            importableFiles = list.filter { !it.name.startsWith(".") && it.extension != "zip" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Secure Files (${vaultFiles.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.testTag("vault_import_button")
            ) {
                Icon(Icons.Default.Add, "Import")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import File")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vaultFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = "Empty",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Vault is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.originalName,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    formatSize(file.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            // Restore back button
                            IconButton(onClick = { onRestoreFile(file) }) {
                                Icon(Icons.Default.RestorePage, "Restore file", tint = MaterialTheme.colorScheme.primary)
                            }
                            // Delete from vault button
                            IconButton(onClick = { onDeleteFile(file) }) {
                                Icon(Icons.Default.DeleteForever, "Delete from vault", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Select File to Lock") },
            text = {
                if (importableFiles.isEmpty()) {
                    Text("No files in workspace sandbox to lock.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(250.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(importableFiles) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onImportFile(file.absolutePath)
                                        showImportDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, "File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Size: ${formatSize(file.length())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
