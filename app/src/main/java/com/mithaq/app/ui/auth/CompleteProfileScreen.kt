package com.mithaq.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.mithaq.app.model.Gender
import com.mithaq.app.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    currentUserProfile: UserProfile,
    viewModel: AuthViewModel,
    isArabic: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var name by remember { mutableStateOf(currentUserProfile.name) }
    var username by remember { mutableStateOf(currentUserProfile.username) }
    var age by remember { mutableStateOf(if (currentUserProfile.age > 0 && currentUserProfile.age != 18) currentUserProfile.age.toString() else "") }
    var gender by remember { mutableStateOf(currentUserProfile.gender) }
    var country by remember { mutableStateOf(if (currentUserProfile.country.isNotEmpty()) currentUserProfile.country else "Saudi Arabia") }
    var city by remember { mutableStateOf(currentUserProfile.city) }
    var oathChecked by remember { mutableStateOf(currentUserProfile.oathChecked) }

    var localError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "إكمال بيانات الملف الشخصي" else "Complete Your Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isArabic) "🔒 يرجى تعبئة البيانات الأساسية للمتابعة ودخول التطبيق شرعياً" else "🔒 Please complete your basic information to enter the app securely",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Username Input
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        localError = null
                    },
                    label = { Text(if (isArabic) "اسم المستخدم (فريد)" else "Username (Unique)") },
                    placeholder = { Text(if (isArabic) "3-14 أحرف، أرقام فقط" else "3-14 chars, no spaces") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Age Input
                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        age = it
                        localError = null
                    },
                    label = { Text(if (isArabic) "العمر (18 - 77)" else "Age (18 - 77)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Gender Selector
                Text(
                    text = if (isArabic) "الجنس (لا يمكن تعديله لاحقاً):" else "Gender (Cannot be changed later):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable { gender = Gender.MALE },
                        colors = CardDefaults.cardColors(
                            containerColor = if (gender == Gender.MALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (gender == Gender.MALE) 2.dp else 1.dp,
                            color = if (gender == Gender.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "أخ (ذكر)" else "Brother (Male)",
                                fontWeight = FontWeight.Bold,
                                color = if (gender == Gender.MALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable { gender = Gender.FEMALE },
                        colors = CardDefaults.cardColors(
                            containerColor = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (gender == Gender.FEMALE) 2.dp else 1.dp,
                            color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "أخت (أنثى)" else "Sister (Female)",
                                fontWeight = FontWeight.Bold,
                                color = if (gender == Gender.FEMALE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Country Selector Dropdown
                var countryExpanded by remember { mutableStateOf(false) }
                val countryOptions = listOf("Saudi Arabia", "Egypt", "United Arab Emirates", "Jordan", "Syria", "Yemen", "Morocco", "Other")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isArabic) "الدولة" else "Country") },
                        trailingIcon = {
                            IconButton(onClick = { countryExpanded = !countryExpanded }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { countryExpanded = !countryExpanded }
                    )
                    DropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        countryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    country = option
                                    countryExpanded = false
                                    localError = null
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // City Input
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        localError = null
                    },
                    label = { Text(if (isArabic) "المدينة" else "City") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Oath Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = oathChecked,
                        onCheckedChange = {
                            oathChecked = it
                            localError = null
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "أقسم بالله العظيم أن استخدامي لهذا التطبيق هو لأغراض الزواج الشرعي، وأتعهد بصدق بياناتي." else "I swear to Allah Almighty that my use of this app is for marital purposes, and I vouch for my data's accuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (oathChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val parsedAge = age.toIntOrNull()
                        if (name.isBlank() || username.isBlank() || city.isBlank() || country.isBlank() || parsedAge == null) {
                            localError = if (isArabic) "يرجى ملء جميع الحقول بشكل صحيح." else "Please fill in all fields correctly."
                        } else if (!validateUsername(username)) {
                            localError = if (isArabic) "اسم المستخدم غير صالح (3-14 حرف، أرقام فقط بحد أقصى 4)." else "Invalid username (3-14 chars, max 4 digits)."
                        } else if (parsedAge !in 18..77) {
                            localError = if (isArabic) "العمر يجب أن يكون بين 18 و77 سنة." else "Age must be between 18 and 77."
                        } else if (!oathChecked) {
                            localError = if (isArabic) "يجب الموافقة على القسم الشرعي للمتابعة." else "You must take the religious oath to continue."
                        } else {
                            isLoading = true
                            viewModel.completeCoreProfile(
                                name = name,
                                username = username,
                                age = parsedAge,
                                gender = gender,
                                country = country,
                                city = city,
                                oathChecked = true,
                                context = context
                            ) { success, error ->
                                isLoading = false
                                if (success) {
                                    onComplete()
                                } else {
                                    localError = error ?: "Failed to save profile."
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isArabic) "حفظ واستمرار" else "Save & Continue",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
