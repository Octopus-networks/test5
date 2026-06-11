package com.mithaq.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mithaq.app.R
import com.mithaq.app.data.repository.OnboardingRepository
import com.mithaq.app.domain.model.OnboardingAnswer
import com.mithaq.app.domain.model.OnboardingSection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAnswersEditorScreen(
    currentUserId: String,
    isArabic: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { OnboardingRepository(context = context) }
    val viewModel = remember { OnboardingViewModel(repository) }
    
    // Default-arg steps() resolves a context via FirebaseApp so the editor shows the
    // same JSON-loaded step list as the live onboarding flow.
    val allSteps = remember { MithaqOnboardingFlow.steps() }
    
    val sectionsWithSteps = remember(allSteps) {
        allSteps.filter { it.isPersisted }
            .groupBy { it.section }
            .filter { it.value.isNotEmpty() }
    }
    
    var existingAnswers by remember { mutableStateOf<Map<String, OnboardingAnswer>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var activeSection by remember { mutableStateOf<OnboardingSection?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val loadAnswers = {
        isLoading = true
        coroutineScope.launch {
            existingAnswers = repository.loadExistingAnswers(currentUserId, allSteps)
            isLoading = false
        }
    }
    
    LaunchedEffect(currentUserId) {
        loadAnswers()
    }
    
    BackHandler {
        if (activeSection != null) {
            activeSection = null
        } else {
            onClose()
        }
    }
    
    if (activeSection != null) {
        QuestionScreen(
            viewModel = viewModel,
            userId = currentUserId,
            isArabic = isArabic,
            onExitRequested = {
                activeSection = null
            },
            onComplete = { _, _ ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (isArabic) "تم حفظ التعديلات بنجاح" else "Changes saved successfully"
                    )
                }
                activeSection = null
                loadAnswers()
            }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(id = if (isArabic) R.string.answers_editor_title_ar else R.string.answers_editor_title))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sectionsWithSteps.entries.toList()) { (section, sectionSteps) ->
                    val sectionStepsIds = sectionSteps.map { it.id }.toSet()
                    val answeredCount = existingAnswers.keys.count { it in sectionStepsIds }
                    val totalCount = sectionSteps.size
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startSectionEdit(sectionSteps, existingAnswers)
                                activeSection = section
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = section.title.resolve(isArabic)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "أجبت على $answeredCount من $totalCount" else "Answered $answeredCount of $totalCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
