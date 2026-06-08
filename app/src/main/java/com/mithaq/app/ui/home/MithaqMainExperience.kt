package com.mithaq.app.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mithaq.app.ui.messages.MithaqMessagesScreen
import com.mithaq.app.ui.profile.MithaqProfileHubScreen
import com.mithaq.app.ui.requests.ChatRequestViewModel
import com.mithaq.app.ui.requests.InterestRequestViewModel
import com.mithaq.app.ui.requests.MithaqRequestsScreen
import com.mithaq.app.ui.requests.PhotoRequestViewModel
import com.mithaq.app.ui.search.MithaqSearchScreen
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class MithaqMainTab {
    Home,
    Search,
    Requests,
    Messages,
    Profile
}

private data class MithaqNavItem(
    val tab: MithaqMainTab,
    val englishLabel: String,
    val arabicLabel: String,
    val icon: ImageVector
)

private val mainNavItems = listOf(
    MithaqNavItem(MithaqMainTab.Home, "Home", "الرئيسية", Icons.Filled.Home),
    MithaqNavItem(MithaqMainTab.Search, "Search", "البحث", Icons.Filled.Search),
    MithaqNavItem(MithaqMainTab.Requests, "Requests", "الطلبات", Icons.Filled.Favorite),
    MithaqNavItem(MithaqMainTab.Messages, "Messages", "الرسائل", Icons.Filled.Chat),
    MithaqNavItem(MithaqMainTab.Profile, "Profile", "حسابي", Icons.Filled.Person)
)

@Composable
fun MithaqMainExperience(
    currentUserId: String,
    isArabic: Boolean,
    onSignOut: () -> Unit,
    isAdmin: Boolean = false,
    onOpenAdminModeration: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenProfileSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(currentUserId) { mutableStateOf(MithaqMainTab.Home) }
    val interestRequestViewModel: InterestRequestViewModel = viewModel(key = "mithaq_interest_requests_$currentUserId")
    val photoRequestViewModel: PhotoRequestViewModel = viewModel(key = "mithaq_photo_requests_$currentUserId")
    val chatRequestViewModel: ChatRequestViewModel = viewModel(key = "mithaq_chat_requests_$currentUserId")

    LaunchedEffect(currentUserId) {
        interestRequestViewModel.loadForUser(currentUserId)
        photoRequestViewModel.loadForUser(currentUserId)
        chatRequestViewModel.loadForUser(currentUserId)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                mainNavItems.forEach { item ->
                    val selected = selectedTab == item.tab
                    val label = if (isArabic) item.arabicLabel else item.englishLabel
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = item.tab },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        val screenModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            MithaqMainTab.Home -> MithaqDiscoverScreen(
                currentUserId = currentUserId,
                isArabic = isArabic,
                interestRequestViewModel = interestRequestViewModel,
                photoRequestViewModel = photoRequestViewModel,
                chatRequestViewModel = chatRequestViewModel,
                onOpenMessages = { selectedTab = MithaqMainTab.Messages },
                modifier = screenModifier
            )
            MithaqMainTab.Search -> MithaqSearchScreen(
                currentUserId = currentUserId,
                isArabic = isArabic,
                interestRequestViewModel = interestRequestViewModel,
                photoRequestViewModel = photoRequestViewModel,
                chatRequestViewModel = chatRequestViewModel,
                onOpenMessages = { selectedTab = MithaqMainTab.Messages },
                modifier = screenModifier
            )
            MithaqMainTab.Requests -> MithaqRequestsScreen(
                currentUserId = currentUserId,
                isArabic = isArabic,
                interestRequestViewModel = interestRequestViewModel,
                photoRequestViewModel = photoRequestViewModel,
                chatRequestViewModel = chatRequestViewModel,
                modifier = screenModifier
            )
            MithaqMainTab.Messages -> MithaqMessagesScreen(
                currentUserId = currentUserId,
                isArabic = isArabic,
                modifier = screenModifier
            )
            MithaqMainTab.Profile -> MithaqProfileHubScreen(
                currentUserId = currentUserId,
                isArabic = isArabic,
                isAdmin = isAdmin,
                onOpenAdminModeration = onOpenAdminModeration,
                onOpenAppSettings = onOpenAppSettings,
                onOpenProfileSettings = onOpenProfileSettings,
                onSignOut = onSignOut,
                modifier = screenModifier
            )
        }
    }
}
