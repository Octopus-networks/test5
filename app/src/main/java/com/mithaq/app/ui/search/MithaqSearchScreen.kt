package com.mithaq.app.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.ui.home.DiscoverUiState
import com.mithaq.app.ui.home.DiscoverViewModel
import com.mithaq.app.ui.home.MithaqPublicProfileCard
import com.mithaq.app.ui.home.PublicProfileFilterRow
import com.mithaq.app.ui.requests.InterestRequestUiState
import com.mithaq.app.ui.requests.InterestRequestViewModel

@Composable
fun MithaqSearchScreen(
    currentUserId: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(key = "mithaq_public_search"),
    interestRequestViewModel: InterestRequestViewModel
) {
    val state by viewModel.state.collectAsState()
    val interestState by interestRequestViewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = if (isArabic) "البحث" else "Search",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isArabic) "فلترة آمنة على البيانات العامة فقط، بدون كشف بيانات خاصة."
            else "Safe filtering over public profile data only, without exposing private fields.",
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
                text = if (isArabic) "فلتر القرب مؤقتًا يعتمد على نفس النتائج لحين تجهيز المدينة/GPS."
                else "Near me currently keeps the same results until city/GPS matching is wired.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SearchResultsContent(
            state = state,
            isArabic = isArabic,
            currentUserId = currentUserId,
            interestState = interestState,
            onSendInterest = { toUserId ->
                interestRequestViewModel.sendInterest(currentUserId, toUserId)
            },
            onRetry = viewModel::loadProfiles
        )
    }
}

@Composable
private fun SearchResultsContent(
    state: DiscoverUiState,
    isArabic: Boolean,
    currentUserId: String,
    interestState: InterestRequestUiState,
    onSendInterest: (String) -> Unit,
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
                title = if (isArabic) "تعذر تحميل نتائج البحث" else "Could not load search results",
                message = if (isArabic) "البيانات العامة غير متاحة الآن. حاول مرة أخرى."
                else "Public profile data is not available right now. Please try again.",
                icon = Icons.Filled.Refresh,
                actionLabel = if (isArabic) "إعادة المحاولة" else "Retry",
                onAction = onRetry
            )
        }
        state.isEmpty -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد ملفات عامة للبحث" else "No public profiles to search",
                message = if (isArabic) "عند اكتمال ملفات أعضاء آخرين ستظهر النتائج هنا."
                else "When other completed public profiles are available, results will appear here.",
                icon = Icons.Filled.Search
            )
        }
        state.hasNoFilterResults -> {
            MithaqEmptyState(
                title = if (isArabic) "لا توجد نتائج مطابقة" else "No matching results",
                message = if (isArabic) "غيّر الفلتر أو ارجع إلى المقترحات."
                else "Change the filter or return to recommended profiles.",
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
                    onSendInterest = onSendInterest,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
