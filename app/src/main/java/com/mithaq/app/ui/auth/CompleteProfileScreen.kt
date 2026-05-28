package com.mithaq.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.*
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    userId: String,
    onCompleteSuccess: () -> Unit,
    viewModel: AuthViewModel,
    isArabic: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.mithaq.app.ui.theme.LocalMithaqStrings.current
    val scrollState = rememberScrollState()

    var currentStep by remember { mutableStateOf(1) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Screen 1: Basics
    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var oathChecked by remember { mutableStateOf(false) }

    // Screen 2: Location
    var country by remember { mutableStateOf(if (isArabic) "السعودية" else "Saudi Arabia") }
    var city by remember { mutableStateOf("Riyadh") }

    fun validateUsername(u: String): Boolean {
        if (u.length !in 3..14) return false
        val digits = u.filter { it.isDigit() }
        if (digits.length > 4) return false
        return u.all { it.isLetterOrDigit() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Language Switcher
        TextButton(
            onClick = { onLanguageChange(!isArabic) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Text(
                text = if (isArabic) "English" else "العربية",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = if (isArabic) "الخطوة $currentStep من 2" else "Step $currentStep of 2",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = currentStep / 2f,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                AnimatedContent(targetState = currentStep) { step ->
                    when (step) {
                        1 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isArabic) "إكمال الملف الشخصي" else "Complete Profile",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text(if (isArabic) "اسم المستخدم (فريد)" else "Username (Unique)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it },
                                    label = { Text(if (isArabic) "العمر (18 - 77)" else "Age (18 - 77)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isArabic) "الجنس:" else "Gender:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f).height(60.dp).clickable { gender = Gender.MALE },
                                        colors = CardDefaults.cardColors(containerColor = if (gender == Gender.MALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(width = if (gender == Gender.MALE) 2.dp else 1.dp, color = if (gender == Gender.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(text = if (isArabic) "أخ (ذكر)" else "Brother", fontWeight = FontWeight.Bold, color = if (gender == Gender.MALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f).height(60.dp).clickable { gender = Gender.FEMALE },
                                        colors = CardDefaults.cardColors(containerColor = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(width = if (gender == Gender.FEMALE) 2.dp else 1.dp, color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(text = if (isArabic) "أخت (أنثى)" else "Sister", fontWeight = FontWeight.Bold, color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Checkbox(checked = oathChecked, onCheckedChange = { oathChecked = it })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "أوافق على الشروط والأحكام" else "I agree to terms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (oathChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        val parsedAge = age.toIntOrNull()
                                        if (username.isBlank() || parsedAge == null) {
                                            localError = if (isArabic) "الرجاء ملء جميع الحقول" else "Please fill all fields"
                                        } else if (!validateUsername(username)) {
                                            localError = if (isArabic) "اسم المستخدم غير صالح" else "Invalid username"
                                        } else if (parsedAge !in 18..77) {
                                            localError = if (isArabic) "عذرًا، يجب أن يكون عمرك بين 18 و 77 سنة للتسجيل." else "Sorry, you must be between 18 and 77 years to register."
                                        } else if (!oathChecked) {
                                            localError = if (isArabic) "يجب الموافقة" else "You must agree"
                                        } else {
                                            localError = null
                                            currentStep = 2
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.next)
                                }
                            }
                        }
                        2 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isArabic) "الموقع الجغرافي" else "Location",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = country,
                                    onValueChange = { country = it },
                                    label = { Text(if (isArabic) "الدولة" else "Country") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text(if (isArabic) "المدينة" else "City") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 1 },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isLoading
                                    ) {
                                        Text(if (isArabic) "السابق" else "Back")
                                    }
                                    Button(
                                        onClick = {
                                            if (country.isBlank() || city.isBlank()) {
                                                localError = if (isArabic) "الرجاء ملء الموقع" else "Please enter location"
                                            } else {
                                                localError = null
                                                isLoading = true
                                                viewModel.updateGoogleUserProfile(
                                                    userId = userId,
                                                    username = username,
                                                    age = age.toIntOrNull() ?: 25,
                                                    gender = gender,
                                                    country = country,
                                                    city = city,
                                                    onSuccess = onCompleteSuccess,
                                                    onError = { err ->
                                                        isLoading = false
                                                        localError = err
                                                    }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                        } else {
                                            Text(if (isArabic) "إكمال التسجيل" else "Complete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (localError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
