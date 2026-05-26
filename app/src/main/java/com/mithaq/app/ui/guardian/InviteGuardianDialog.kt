package com.mithaq.app.ui.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * An elegant, localized-ready Material Design 3 Composable Dialog for inviting a Guardian (Wali).
 * Includes fields for Guardian Name and Email, handles form validation, loading indicator, and
 * visual success/error states according to the current [GuardianUiState].
 */
@Composable
fun InviteGuardianDialog(
    onDismissRequest: () -> Unit,
    viewModel: GuardianViewModel,
    modifier: Modifier = Modifier,
    // Custom labels for easy localization/translation injection
    titleText: String = "Invite Guardian (Wali)",
    subtitleText: String = "In Islamic matchmaking, involving a guardian ensures a blessed, transparent, and safe journey.",
    nameLabel: String = "Guardian's Full Name",
    emailLabel: String = "Guardian's Email Address",
    submitButtonText: String = "Send Invitation",
    cancelButtonText: String = "Cancel",
    successTitle: String = "Invitation Sent",
    successSubtitle: String = "An invitation has been sent to your Guardian. We will notify you once they accept.",
    closeButtonText: String = "Close",
    fieldRequiredError: String = "Both fields are required",
    invalidEmailError: String = "Please enter a valid email address",
    isArabic: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Client-side quick validation states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Helper to validate and submit
    val performSubmit = {
        nameError = name.trim().isEmpty()
        emailError = email.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

        when {
            nameError -> errorMessage = fieldRequiredError
            emailError -> errorMessage = if (email.trim().isEmpty()) fieldRequiredError else invalidEmailError
            else -> {
                errorMessage = null
                viewModel.inviteGuardian(name, email)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (uiState !is GuardianUiState.Loading) {
                viewModel.resetState()
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = uiState !is GuardianUiState.Loading,
            dismissOnClickOutside = uiState !is GuardianUiState.Loading
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize()
            ) {
                when (uiState) {
                    is GuardianUiState.Success -> {
                        // Success State View with premium verification checkmark animation and text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = successTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = successSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    sendWaliEmailInvitation(context, name, email, isArabic)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(text = if (isArabic) "أرسل بريد الدعوة الآن" else "Send Email Invite Now")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.resetState()
                                    onDismissRequest()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = closeButtonText)
                            }
                        }
                    }

                    else -> {
                        // Normal input states (Idle, Loading, or Error)
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Guardian Name Field
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = false
                                    errorMessage = null
                                },
                                label = { Text(nameLabel) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                isError = nameError,
                                singleLine = true,
                                enabled = uiState !is GuardianUiState.Loading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Guardian Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = false
                                    errorMessage = null
                                },
                                label = { Text(emailLabel) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                isError = emailError,
                                singleLine = true,
                                enabled = uiState !is GuardianUiState.Loading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        performSubmit()
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Error Messaging Layout
                            val activeError = (uiState as? GuardianUiState.Error)?.errorMessage ?: errorMessage
                            if (activeError != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Dialog Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.resetState()
                                        onDismissRequest()
                                    },
                                    enabled = uiState !is GuardianUiState.Loading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(text = cancelButtonText)
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        performSubmit()
                                    },
                                    enabled = uiState !is GuardianUiState.Loading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.minWidth(120.dp)
                                ) {
                                    if (uiState is GuardianUiState.Loading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(text = submitButtonText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inline helper extension for setting button minWidth
private fun Modifier.minWidth(width: androidx.compose.ui.unit.Dp): Modifier = this.defaultMinSize(minWidth = width)

private fun sendWaliEmailInvitation(
    context: android.content.Context,
    guardianName: String,
    guardianEmail: String,
    isArabic: Boolean
) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(guardianEmail.trim().lowercase()))
            putExtra(android.content.Intent.EXTRA_SUBJECT, if (isArabic) "دعوة إشراك ولي أمر - تطبيق ميثاق" else "Guardian Invitation - Mithaq App")
            putExtra(android.content.Intent.EXTRA_TEXT, if (isArabic) {
                "السلام عليكم يا $guardianName،\n\nلقد قمت بإضافتك كولي أمر لي في تطبيق ميثاق للزواج الإسلامي المحافظ.\n\nيرجى تحميل التطبيق والتسجيل فيه بحساب ولي أمر (Guardian) باستخدام بريدك الإلكتروني هذا ($guardianEmail) لمتابعة المحادثات والطلبات بشكل مباشر وآمن لضمان بيئة شرعية ومحافظة.\n\nرابط تحميل التطبيق: https://github.com/ahmedbebars/test5/raw/main/Mithaq-v2.0-Release.apk\n\nشكراً لك."
            } else {
                "Assalamu Alaikum $guardianName,\n\nI have invited you to be my Guardian (Wali) on the Mithaq matchmaking application.\n\nPlease download the app and register a Guardian account using this email address ($guardianEmail) to oversee my chats and requests securely.\n\nApp Download Link: https://github.com/ahmedbebars/test5/raw/main/Mithaq-v2.0-Release.apk\n\nThank you."
            })
        }
        val chooser = android.content.Intent.createChooser(intent, if (isArabic) "اختر تطبيق البريد الإلكتروني" else "Choose Email Client")
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            if (isArabic) "تعذر فتح تطبيق البريد" else "Could not open email application",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
