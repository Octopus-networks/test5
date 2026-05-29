package com.mithaq.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.domain.model.PublicProfile
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.requests.InterestRequestUiState
import com.mithaq.app.ui.requests.InterestRequestViewModel
import com.mithaq.app.ui.requests.PhotoRequestUiState
import com.mithaq.app.ui.requests.PhotoRequestViewModel

@Composable
fun MithaqDiscoverScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(key = "mithaq_discover_home"),
    interestRequestViewModel: InterestRequestViewModel,
    photoRequestViewModel: PhotoRequestViewModel
) {
    val state by viewModel.state.collectAsState()
    val interestState by interestRequestViewModel.state.collectAsState()
    val photoState by photoRequestViewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = if (isArabic) "اكتشف توافقًا جادًا" else "Discover serious matches",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isArabic) "بطاقات منظمة من البيانات العامة الآمنة فقط."
            else "Organized cards built only from safe public profile data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        PublicProfileFilterRow(
            isArabic = isArabic,
            selectedFilter = state.selectedFilter,
            onFilterSelected = viewModel::selectFilter
        )
        if (state.isNearMePlaceholder) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isArabic) "فلتر القرب سيستخدم المدينة أو GPS لاحقًا بدون عرض مسافة دقيقة الآن."
                else "Near me will use city or GPS later; no exact distance is shown yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        InterestRequestMessage(state = interestState)
        PhotoRequestMessage(state = photoState)
        if (interestState.message != null ||
            interestState.errorMessage != null ||
            photoState.message != null ||
            photoState.errorMessage != null
        ) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        DiscoverProfileContent(
            state = state,
            isArabic = isArabic,
            currentUserId = currentUserId,
            interestState = interestState,
            photoState = photoState,
            onSendInterest = { toUserId ->
                interestRequestViewModel.sendInterest(currentUserId, toUserId)
            },
            onRequestPhoto = { toUserId ->
                photoRequestViewModel.requestPhoto(currentUserId, toUserId)
            },
            onRetry = viewModel::loadProfiles
        )
    }
}
@Composable
fun PublicProfileFilterRow(
    isArabic: Boolean,
    selectedFilter: PublicProfileFilter,
    onFilterSelected: (PublicProfileFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PublicProfileFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label(isArabic)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun InterestRequestMessage(state: InterestRequestUiState) {
    val message = state.errorMessage ?: state.message ?: return
    val isError = state.errorMessage != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

@Composable
private fun PhotoRequestMessage(state: PhotoRequestUiState) {
    val message = state.errorMessage ?: state.message ?: return
    val isError = state.errorMessage != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

@Composable
private fun DiscoverProfileContent(
    state: DiscoverUiState,
    isArabic: Boolean,
    currentUserId: String,
    interestState: InterestRequestUiState,
    photoState: PhotoRequestUiState,
    onSendInterest: (String) -> Unit,
    onRequestPhoto: (String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        state.errorMessage != null -> {
            MithaqEmptyState(
                title = if (isArabic) "تعذر تحميل الملفات العامة" else "Could not load public profiles",
                message = if (isArabic) "تحقق من الاتصال وحاول مرة أخرى."
                else "Please check your connection and try again.",
                icon = Icons.Filled.Refresh,
                actionLabel = if (isArabic) "إعادة المحاولة" else "Retry",
                onAction = onRetry
            )
        }
        state.isEmpty -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد ملفات عامة بعد" else "No public profiles yet",
                message = if (isArabic) "ستظهر هنا الملفات المكتملة التي اختارت الظهور في البحث."
                else "Completed public discovery profiles will appear here.",
                icon = Icons.Filled.Search
            )
        }
        state.hasNoFilterResults -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد نتائج لهذا الفلتر" else "No results for this filter",
                message = if (isArabic) "جرب فلترًا آخر أو عد إلى المقترحات."
                else "Try another filter or return to recommended profiles.",
                icon = Icons.Filled.Search
            )
        }
        else -> {
            state.visibleProfiles.forEach { profile ->
                MithaqPublicProfileCard(
                    profile = profile,
                    isArabic = isArabic,
                    currentUserId = currentUserId,
                    interestState = interestState,
                    photoState = photoState,
                    onSendInterest = onSendInterest,
                    onRequestPhoto = onRequestPhoto,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun MithaqPublicProfileCard(
    profile: PublicProfile,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    interestState: InterestRequestUiState = InterestRequestUiState(),
    photoState: PhotoRequestUiState = PhotoRequestUiState(),
    onSendInterest: (String) -> Unit = {},
    onRequestPhoto: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PhotoPrivacyPlaceholder(
                mode = profile.photoPrivacyMode,
                isArabic = isArabic
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayTitle(isArabic),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = profile.locationLabel(isArabic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${profile.profileCompletionPercent}%") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profile.isIdentityVerified) {
                    ProfileSignalChip(if (isArabic) "هوية موثقة" else "Identity verified", Icons.Filled.CheckCircle)
                } else if (profile.isEmailVerified) {
                    ProfileSignalChip(if (isArabic) "بريد موثق" else "Email verified", Icons.Filled.CheckCircle)
                }
                if (profile.hasGuardian) {
                    ProfileSignalChip(if (isArabic) "ولي مضاف" else "Guardian added", Icons.Filled.Person)
                }
                if (profile.prayerRoutineShared) {
                    ProfileSignalChip(if (isArabic) "نمط الصلاة ظاهر" else "Prayer shared", Icons.Filled.Lock)
                }
                if (profile.localTimeEnabled) {
                    ProfileSignalChip(if (isArabic) "التوقيت المحلي ظاهر" else "Local time shared", Icons.Filled.Search)
                }
                if (profile.accountType.isNotBlank()) {
                    ProfileSignalChip(profile.accountType, Icons.Filled.Person)
                }
                if (profile.maritalStatus.isNotBlank()) {
                    ProfileSignalChip(profile.maritalStatus, Icons.Filled.Favorite)
                }
                if (profile.marriageTimeline.isNotBlank()) {
                    ProfileSignalChip(profile.marriageTimeline, Icons.Filled.Chat)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val requestStatus = interestState.sentStatusByUserId[profile.userId]
                val canSendInterest = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in interestState.sendingToUserIds &&
                        (requestStatus == null || requestStatus == "cancelled")
                Button(
                    onClick = { onSendInterest(profile.userId) },
                    enabled = canSendInterest,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val buttonText = when {
                        profile.userId in interestState.sendingToUserIds -> if (isArabic) "\u062c\u0627\u0631\u064a \u0627\u0644\u0625\u0631\u0633\u0627\u0644" else "Sending..."
                        requestStatus == "pending" -> if (isArabic) "\u062a\u0645 \u0627\u0644\u0625\u0631\u0633\u0627\u0644" else "Sent"
                        requestStatus == "accepted" -> if (isArabic) "\u062a\u0645 \u0627\u0644\u0642\u0628\u0648\u0644" else "Accepted"
                        requestStatus == "declined" -> if (isArabic) "\u062a\u0645 \u0631\u0641\u0636 \u0627\u0644\u0627\u0647\u062a\u0645\u0627\u0645" else "Interest declined"
                        else -> if (isArabic) "\u0625\u0631\u0633\u0627\u0644 \u0627\u0647\u062a\u0645\u0627\u0645" else "Send interest"
                    }
                    Text(buttonText)
                }
                val photoStatus = photoState.sentStatusByUserId[profile.userId]
                val hasAcceptedInterest = profile.userId in interestState.acceptedWithUserIds
                val normalizedPhotoMode = profile.photoPrivacyMode.ifBlank { "blurred_by_default" }
                val photoModeAllowsRequest = normalizedPhotoMode == "blurred_by_default" ||
                        normalizedPhotoMode == "approved_users_only"
                val canRequestPhoto = currentUserId.isNotBlank() &&
                        profile.userId != currentUserId &&
                        profile.userId !in photoState.requestingToUserIds &&
                        hasAcceptedInterest &&
                        photoModeAllowsRequest &&
                        (photoStatus == null || photoStatus == "cancelled")
                OutlinedButton(
                    onClick = { onRequestPhoto(profile.userId) },
                    enabled = canRequestPhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val photoButtonText = when {
                        profile.userId in photoState.requestingToUserIds -> if (isArabic) "جاري الطلب" else "Requesting..."
                        photoStatus == "pending" -> if (isArabic) "تم طلب الصورة" else "Photo requested"
                        photoStatus == "approved" -> if (isArabic) "تمت الموافقة" else "Photo access approved"
                        photoStatus == "declined" -> if (isArabic) "رفض طلب الصورة" else "Photo request declined"
                        !photoModeAllowsRequest && normalizedPhotoMode == "matched_users_only" -> if (isArabic) "للمتوافقين فقط" else "Matched only"
                        !photoModeAllowsRequest -> if (isArabic) "الصور خاصة" else "Photos private"
                        !hasAcceptedInterest -> if (isArabic) "الاهتمام أولا" else "Interest first"
                        else -> if (isArabic) "طلب عرض الصورة" else "Request photo access"
                    }
                    Text(photoButtonText)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "طلب محادثة محترمة" else "Request chat")
            }
        }
    }
}

@Composable
private fun PhotoPrivacyPlaceholder(
    mode: String,
    isArabic: Boolean
) {
    val normalizedMode = mode.ifBlank { "blurred_by_default" }
    val label = when (normalizedMode) {
        "hidden" -> if (isArabic) "الصورة مخفية" else "Photo hidden"
        "approved_users_only" -> if (isArabic) "الصورة بالموافقة فقط" else "Photo by approval only"
        "matched_users_only" -> if (isArabic) "الصورة للتوافقات فقط" else "Photo for matches only"
        else -> if (isArabic) "الصورة مموهة افتراضيًا" else "Photo blurred by default"
    }
    val icon = when (normalizedMode) {
        "approved_users_only" -> Icons.Filled.Visibility
        "matched_users_only" -> Icons.Filled.Favorite
        else -> Icons.Filled.Lock
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.22f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileSignalChip(
    label: String,
    icon: ImageVector
) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.secondary
        )
    )
}

fun PublicProfileFilter.label(isArabic: Boolean): String {
    return when (this) {
        PublicProfileFilter.Recommended -> if (isArabic) "مقترح" else "Recommended"
        PublicProfileFilter.NearMe -> if (isArabic) "بالقرب مني" else "Near me"
        PublicProfileFilter.Verified -> if (isArabic) "موثق" else "Verified"
        PublicProfileFilter.WithGuardian -> if (isArabic) "مع ولي" else "With guardian"
        PublicProfileFilter.RecentlyActive -> if (isArabic) "نشط حديثًا" else "Recently active"
        PublicProfileFilter.PrayerRoutineShared -> if (isArabic) "يشارك نمط الصلاة" else "Prayer routine shared"
        PublicProfileFilter.NewMembers -> if (isArabic) "أعضاء جدد" else "New members"
    }
}

private fun PublicProfile.displayTitle(isArabic: Boolean): String {
    val name = displayName.ifBlank { if (isArabic) "عضو ميثاق" else "Mithaq member" }
    val ageLabel = age?.toString()
    return if (ageLabel == null) name else "$name, $ageLabel"
}

private fun PublicProfile.locationLabel(isArabic: Boolean): String {
    val parts = listOf(city, country).map { it.trim() }.filter { it.isNotBlank() }
    return parts.joinToString(", ").ifBlank {
        if (isArabic) "الموقع غير ظاهر" else "Location not shared"
    }
}
