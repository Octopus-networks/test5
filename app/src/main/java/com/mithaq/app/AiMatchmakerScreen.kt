package com.mithaq.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mithaq.app.model.UserProfile
import com.mithaq.app.model.Gender
import com.mithaq.app.ui.filter.SearchViewModel
import com.mithaq.app.ui.photo.UserProfileImage
import com.mithaq.app.ui.photo.PhotoAccessState
import com.mithaq.app.ui.theme.SuccessGreen
import com.mithaq.app.service.GeminiService
import kotlinx.coroutines.launch

data class AiChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val recommendedProfiles: List<UserProfile> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMatchmakerScreen(
    currentUser: UserProfile,
    searchViewModel: SearchViewModel,
    isArabic: Boolean,
    onBack: () -> Unit,
    onSelectMatch: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val resolveProfile = rememberUserProfileResolver(searchViewModel, isMock = com.mithaq.app.Config.isMock(), currentUser = currentUser)

    val welcomeMessage = if (isArabic) {
        "السلام عليكم ورحمة الله وبركاته. أنا الخاطبة الإلكترونية لميثاق. يسعدني مساعدتك في استكشاف شريك الحياة المناسب شرعياً وفكرياً. أخبرني، ما هي الصفات الأساسية التي تبحث عنها؟ (مثال: العمر، المدينة، الالتزام بالصلوات، إلخ)"
    } else {
        "Assalamu Alaikum. I am Mithaq's AI Matchmaker. I am here to help you find a compatible spouse. Tell me, what are the most important criteria you are looking for? (e.g., age, location, religiosity, etc.)"
    }

    val chatMessages = remember {
        mutableStateListOf(
            AiChatMessage(role = "model", content = welcomeMessage)
        )
    }

    var userInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Auto scroll to bottom
    LaunchedEffect(chatMessages.size, isThinking) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isArabic) "الخاطبة الذكية (أم أحمد)" else "AI Khattaba Bot",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Intro Warning Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isArabic) {
                        "🔒 الخصوصية والالتزام: محادثتك مع الخاطبة سرية للغاية وتتم معالجتها آمنياً لمساعدتك على البحث فقط وفق الضوابط الإسلامية."
                    } else {
                        "🔒 Privacy & Modesty: Your conversation with Al-Khattaba is fully confidential and used solely to match you with compatible partners."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Message List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chatMessages.forEach { message ->
                    val isMe = message.role == "user"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMe) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier.widthIn(max = 280.dp),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            val bubbleBg = if (isMe) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            }
                            val bubbleTextColor = if (isMe) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val bubbleShape = if (isMe) {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
                            } else {
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                            }

                            Card(
                                shape = bubbleShape,
                                colors = CardDefaults.cardColors(containerColor = bubbleBg)
                            ) {
                                Text(
                                    text = message.content,
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bubbleTextColor,
                                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                                )
                            }

                            // Show recommended profile cards if present
                            if (message.recommendedProfiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isArabic) "الشركاء المرشحون لك:" else "Recommended spouse profiles:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                message.recommendedProfiles.forEach { profile ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSelectMatch(profile) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(40.dp)) {
                                                val isAccessApproved = profile.photoAccessApprovedUsers.contains(currentUser.uid)
                                                UserProfileImage(
                                                    imageUrl = profile.imageUrl,
                                                    gender = profile.gender,
                                                    isBlurred = !isAccessApproved,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = profile.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    VerificationBadge(status = profile.verificationStatus)
                                                }
                                                Text(
                                                    text = "${profile.age} • ${profile.city}, ${profile.country}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isMe) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (isThinking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (isArabic) "الخاطبة تفكر..." else "Al-Khattaba is thinking...",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = {
                        Text(if (isArabic) "اكتب رسالة للبحث..." else "Describe your ideal partner...")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                IconButton(
                    onClick = {
                        val text = userInput.trim()
                        if (text.isNotEmpty() && !isThinking) {
                            userInput = ""
                            chatMessages.add(AiChatMessage(role = "user", content = text))
                            isThinking = true

                            coroutineScope.launch {
                                try {
                                    val apiKey = com.mithaq.app.Config.GEMINI_API_KEY
                                    if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                                        val mockReply = if (isArabic) {
                                            "أهلاً بك. ميزة الذكاء الاصطناعي تحتاج لمفتاح GEMINI_API_KEY في ملف Config.kt لتعمل بشكل كامل وتلقائي."
                                        } else {
                                            "Hello. AI matchmaking requires setting GEMINI_API_KEY in Config.kt to function properly."
                                        }
                                        chatMessages.add(AiChatMessage(role = "model", content = mockReply))
                                    } else {
                                        val service = GeminiService(apiKey)
                                        
                                        // Package candidates
                                        val candidatesList = searchResults.filter { it.gender != currentUser.gender }
                                        
                                        // History conversion
                                        val history = chatMessages.map { Pair(it.role, it.content) }
                                        
                                        val jsonResult = service.converseWithMatchmaker(currentUser, candidatesList, history)
                                        
                                        val responseText = jsonResult.optString("response", "")
                                        val uidsArr = jsonResult.optJSONArray("recommended_uids")
                                        val recommended = mutableListOf<UserProfile>()
                                        
                                        if (uidsArr != null) {
                                            for (i in 0 until uidsArr.length()) {
                                                val uid = uidsArr.getString(i)
                                                recommended.add(resolveProfile(uid))
                                            }
                                        }
                                        
                                        chatMessages.add(
                                            AiChatMessage(
                                                role = "model",
                                                content = responseText,
                                                recommendedProfiles = recommended
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    chatMessages.add(
                                        AiChatMessage(
                                            role = "model",
                                            content = if (isArabic) "عذراً، حدث خطأ أثناء الاتصال بالخاطبة." else "Sorry, an error occurred with the AI."
                                        )
                                    )
                                } finally {
                                    isThinking = false
                                }
                            }
                        }
                    },
                    enabled = userInput.trim().isNotEmpty() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (userInput.trim().isNotEmpty() && !isThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (userInput.trim().isNotEmpty() && !isThinking) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
