package com.mithaq.app.ui.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mithaq.app.ui.auth.AuthState
import com.mithaq.app.ui.auth.AuthViewModel

@Composable
fun VerifyEmailScreen(
    email: String?,
    viewModel: AuthViewModel,
    isArabic: Boolean,
    onVerified: (String) -> Unit,
    onChangeEmail: () -> Unit,
    onSignOut: () -> Unit
) {
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val title = if (isArabic) "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0628\u0631\u064a\u062f\u0643 \u0627\u0644\u0625\u0644\u0643\u062a\u0631\u0648\u0646\u064a" else "Check your email"
    val body = if (isArabic) {
        "\u0623\u0631\u0633\u0644\u0646\u0627 \u0631\u0627\u0628\u0637 \u062a\u0641\u0639\u064a\u0644 \u0625\u0644\u0649 \u0628\u0631\u064a\u062f\u0643 \u0627\u0644\u0625\u0644\u0643\u062a\u0631\u0648\u0646\u064a. \u0627\u0641\u062a\u062d \u0627\u0644\u0628\u0631\u064a\u062f \u0648\u0627\u0636\u063a\u0637 \u0639\u0644\u0649 \u062a\u0641\u0639\u064a\u0644 \u0627\u0644\u062d\u0633\u0627\u0628 \u0644\u0644\u0645\u062a\u0627\u0628\u0639\u0629."
    } else {
        "We sent an activation link to your email. Please open your inbox and tap Activate Account to continue."
    }

    fun arabicResendError(resultMessage: String): String {
        return when {
            resultMessage.contains("wait", ignoreCase = true) ->
                "\u0627\u0646\u062a\u0638\u0631 \u0642\u0644\u064a\u0644\u0627\u064b \u0642\u0628\u0644 \u0625\u0639\u0627\u062f\u0629 \u0625\u0631\u0633\u0627\u0644 \u0631\u0627\u0628\u0637 \u0627\u0644\u062a\u0641\u0639\u064a\u0644."
            resultMessage.contains("connection", ignoreCase = true) || resultMessage.contains("network", ignoreCase = true) ->
                "\u062d\u062f\u062b\u062a \u0645\u0634\u0643\u0644\u0629 \u0641\u064a \u0627\u0644\u0627\u062a\u0635\u0627\u0644. \u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a \u0648\u062d\u0627\u0648\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649."
            else ->
                "\u0644\u0645 \u0646\u062a\u0645\u0643\u0646 \u0645\u0646 \u0625\u0631\u0633\u0627\u0644 \u0631\u0633\u0627\u0644\u0629 \u0627\u0644\u062a\u0641\u0639\u064a\u0644. \u062d\u0627\u0648\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = if (isArabic) "\u0627\u0644\u0628\u0631\u064a\u062f \u0627\u0644\u0625\u0644\u0643\u062a\u0631\u0648\u0646\u064a" else "Email",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = email ?: (if (isArabic) "\u0627\u0644\u0628\u0631\u064a\u062f \u063a\u064a\u0631 \u0645\u062a\u0627\u062d" else "Email unavailable"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (message != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message.orEmpty(),
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isChecking = true
                        message = null
                        viewModel.reloadAndCheckEmailVerification { verified, resultMessage ->
                            isChecking = false
                            isError = !verified
                            message = if (verified) {
                                if (isArabic) "\u062a\u0645 \u062a\u0641\u0639\u064a\u0644 \u0627\u0644\u0628\u0631\u064a\u062f \u0628\u0646\u062c\u0627\u062d." else "Email verified successfully."
                            } else if (isArabic) {
                                if (resultMessage.contains("connection", ignoreCase = true) || resultMessage.contains("network", ignoreCase = true)) {
                                    "\u062d\u062f\u062b\u062a \u0645\u0634\u0643\u0644\u0629 \u0641\u064a \u0627\u0644\u0627\u062a\u0635\u0627\u0644. \u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a \u0648\u062d\u0627\u0648\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649."
                                } else {
                                    "\u0644\u0645 \u064a\u062a\u0645 \u062a\u0641\u0639\u064a\u0644 \u0627\u0644\u0628\u0631\u064a\u062f \u0628\u0639\u062f. \u0627\u0641\u062a\u062d \u0631\u0627\u0628\u0637 \u0627\u0644\u062a\u0641\u0639\u064a\u0644 \u0623\u0648\u0644\u0627\u064b."
                                }
                            } else {
                                resultMessage
                            }
                            if (verified) {
                                val uid = (viewModel.authState.value as? AuthState.Authenticated)?.userId
                                if (uid != null) onVerified(uid)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking && !isResending,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (isArabic) "\u062a\u0645 \u062a\u0641\u0639\u064a\u0644 \u0627\u0644\u0628\u0631\u064a\u062f" else "I verified my email")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isResending = true
                        message = null
                        viewModel.resendVerificationEmail { success, resultMessage ->
                            isResending = false
                            isError = !success
                            message = if (success) {
                                if (isArabic) "\u062a\u0645 \u0625\u0631\u0633\u0627\u0644 \u0631\u0633\u0627\u0644\u0629 \u0627\u0644\u062a\u0641\u0639\u064a\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649." else "Verification email sent again."
                            } else if (isArabic) {
                                arabicResendError(resultMessage)
                            } else {
                                resultMessage
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking && !isResending,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isResending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (isArabic) "\u0625\u0639\u0627\u062f\u0629 \u0625\u0631\u0633\u0627\u0644 \u0627\u0644\u0631\u0633\u0627\u0644\u0629" else "Resend email")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            onChangeEmail()
                        },
                        enabled = !isChecking && !isResending
                    ) {
                        Text(if (isArabic) "\u062a\u063a\u064a\u064a\u0631 \u0627\u0644\u0628\u0631\u064a\u062f" else "Change email")
                    }

                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        enabled = !isChecking && !isResending
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (isArabic) "\u062a\u0633\u062c\u064a\u0644 \u0627\u0644\u062e\u0631\u0648\u062c" else "Sign out")
                    }
                }
            }
        }
    }
}
