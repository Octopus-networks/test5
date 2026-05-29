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
import androidx.compose.material.icons.filled.Logout
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

    val title = if (isArabic) "تحقق من بريدك الإلكتروني" else "Check your email"
    val body = if (isArabic) {
        "أرسلنا رابط تفعيل إلى بريدك الإلكتروني. افتح البريد واضغط على تفعيل الحساب للمتابعة."
    } else {
        "We sent an activation link to your email. Please open your inbox and tap Activate Account to continue."
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
            shape = RoundedCornerShape(8.dp),
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
                    contentDescription = if (isArabic) "البريد الإلكتروني" else "Email",
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
                    text = email ?: (if (isArabic) "البريد غير متاح" else "Email unavailable"),
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
                                if (isArabic) "تم تفعيل البريد بنجاح." else "Email verified successfully."
                            } else {
                                if (isArabic) "لم يتم تفعيل البريد بعد. افتح رابط التفعيل أولاً." else resultMessage
                            }
                            if (verified) {
                                val uid = (viewModel.authState.value as? com.mithaq.app.ui.auth.AuthState.Authenticated)?.userId
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
                        Text(if (isArabic) "تم تفعيل البريد" else "I verified my email")
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
                                if (isArabic) "تم إرسال رسالة التفعيل مرة أخرى." else "Verification email sent again."
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
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (isArabic) "إعادة إرسال الرسالة" else "Resend email")
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
                        Text(if (isArabic) "تغيير البريد" else "Change email")
                    }

                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        enabled = !isChecking && !isResending
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (isArabic) "تسجيل الخروج" else "Sign out")
                    }
                }
            }
        }
    }
}
