package com.mithaq.app.ui.limit

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mithaq.app.ui.auth.AuthViewModel

data class StoreFeatureItem(
    val icon: ImageVector,
    val titleEn: String,
    val titleAr: String,
    val isGold: Boolean = false // If true, unlocked in Gold too
)

data class HighlightItem(
    val icon: ImageVector,
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PremiumStoreScreen(
    viewModel: AuthViewModel,
    isArabic: Boolean,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTabState by remember { mutableStateOf(initialTab) } // 0: Gold, 1: Platinum
    var showCheckout by remember { mutableStateOf(false) }

    val currentUserProfile by viewModel.currentUserProfile.collectAsState()

    // 50% discount prices
    val goldPriceLabel = if (isArabic) "احصل على ٣ أشهر مقابل ٣٩٩.٩٩ ج.م" else "Get 3 months for EGP 399.99"
    val platinumPriceLabel = if (isArabic) "احصل على شهر واحد مقابل ٢٥٨.٥٠ ج.م" else "Get 1 month for EGP 258.50"

    val features = listOf(
        StoreFeatureItem(Icons.Default.Chat, "Send Unlimited Communications", "إرسال رسائل غير محدودة", isGold = true),
        StoreFeatureItem(Icons.Default.LockOpen, "Unlock Your Messages", "فتح الرسائل المغلقة", isGold = true),
        StoreFeatureItem(Icons.Default.Block, "Say Goodbye to Ads", "تصفح بدون إعلانات", isGold = true),
        StoreFeatureItem(Icons.Default.VisibilityOff, "Hide Your Profile and Photos", "إخفاء الملف الشخصي والصور", isGold = true),
        StoreFeatureItem(Icons.Default.TrendingUp, "Rank Above Other Members", "أولوية الظهور في البحث"),
        StoreFeatureItem(Icons.Default.AutoAwesome, "Instantly Translate Messages", "ترجمة فورية للرسائل"),
        StoreFeatureItem(Icons.Default.CropFree, "Double Your Profile Space", "مساحة إضافية للملف الشخصي"),
        StoreFeatureItem(Icons.Default.Favorite, "Get Better Matches", "الحصول على مطابقات أفضل"),
        StoreFeatureItem(Icons.Default.LocalFireDepartment, "View Popular Members", "مشاهدة الأعضاء الأكثر شعبية"),
        StoreFeatureItem(Icons.Default.FavoriteBorder, "See who liked you", "رؤية من أعجب بك"),
        StoreFeatureItem(Icons.Default.RemoveRedEye, "See who viewed you", "رؤية من زار ملفك"),
        StoreFeatureItem(Icons.Default.StarBorder, "See who favorited you", "رؤية من أضافك للمفضلة"),
        StoreFeatureItem(Icons.Default.Tune, "Advanced Filters", "فلاتر بحث متقدمة")
    )

    val highlights = listOf(
        HighlightItem(
            Icons.Default.LockOpen,
            "Unlock Your Messages",
            "فتح قفل رسائلك",
            "Premium members can send and receive unlimited messages to all members on Mithaq.",
            "يمكن للأعضاء المميزين إرسال واستقبال رسائل غير محدودة مع جميع المشتركين."
        ),
        HighlightItem(
            Icons.Default.TrendingUp,
            "Rank Above Other Members",
            "أولوية الظهور فوق الأعضاء الآخرين",
            "As a premium member, your profile will rank above standard members in search results.",
            "بصفتك عضواً مميزاً، سيظهر ملفك الشخصي في أعلى نتائج البحث قبل الأعضاء العاديين."
        ),
        HighlightItem(
            Icons.Default.AutoAwesome,
            "Instantly Translate Messages",
            "ترجمة فورية للرسائل",
            "Don't let language barriers get in the way of love with messages translated instantly.",
            "لا تدع حواجز اللغة تقف عائقاً في طريق تواصلك، مع ترجمة فورية وفورية للرسائل."
        )
    )

    val pagerState = rememberPagerState(pageCount = { highlights.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "ترقية الحساب" else "Upgrade",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main content layout with weight
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Sliding Pager / Highlights Card at the Top
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTabState == 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            val item = highlights[page]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isArabic) item.titleAr else item.titleEn,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isArabic) item.descAr else item.descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Page indicators (dots)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(highlights.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // 2. Gold vs Platinum Plan Switches
                TabRow(
                    selectedTabIndex = selectedTabState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                            color = if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabState == 0,
                        onClick = { selectedTabState = 0 },
                        text = {
                            Text(
                                if (isArabic) "ذهبية Gold" else "Gold",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabState == 1,
                        onClick = { selectedTabState = 1 },
                        text = {
                            Text(
                                if (isArabic) "بلاتينية Platinum" else "Platinum",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabState == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // 3. Feature list with dynamic checkmarks
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "الميزات المتاحة:" else "Features:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    features.forEach { feature ->
                        // Check if unlocked in the selected tier
                        val isUnlocked = if (selectedTabState == 0) feature.isGold else true
                        val tintColor = if (isUnlocked) {
                            if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = if (isArabic) feature.titleAr else feature.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                            if (isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Available",
                                    tint = tintColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Footer & Purchase button
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) {
                            "سيتم تجديد هذا الاشتراك تلقائياً بنفس السعر والمدة عند انتهائه. يمكنك الإلغاء بسهولة عبر إعدادات متجر Google Play في أي وقت."
                        } else {
                            "This subscription will be automatically renewed for the same price and membership length once it expires. You can easily cancel via Google Play settings at any time."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { showCheckout = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (selectedTabState == 0) goldPriceLabel else platinumPriceLabel,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showCheckout) {
        val planName = if (selectedTabState == 0) {
            if (isArabic) "الباقة الذهبية (Gold)" else "Gold Plan"
        } else {
            if (isArabic) "الباقة البلاتينية (Platinum)" else "Platinum Plan"
        }
        val planPrice = if (selectedTabState == 0) "EGP 399.99" else "EGP 258.50"
        val planId = if (selectedTabState == 0) "gold" else "platinum"

        SimulatedCheckoutDialog2(
            planName = planName,
            planPrice = planPrice,
            planColor = if (selectedTabState == 0) Color(0xFFD4AF37) else MaterialTheme.colorScheme.secondary,
            isArabic = isArabic,
            onDismiss = { showCheckout = false },
            onPaymentSuccess = {
                viewModel.purchasePremiumPlan(planId)
                showCheckout = false
                onBack()
            }
        )
    }
}

@Composable
fun SimulatedCheckoutDialog2(
    planName: String,
    planPrice: String,
    planColor: Color,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = { if (!isProcessing && !isSuccess) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                if (!isSuccess) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "إتمام الدفع الآمن" else "Secure Checkout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$planName - $planPrice",
                            style = MaterialTheme.typography.bodyMedium,
                            color = planColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(40.dp))
                            CircularProgressIndicator(color = planColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "جاري معالجة الدفع..." else "Processing payment...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        } else {
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.length <= 16) cardNumber = it },
                                label = { Text(if (isArabic) "رقم البطاقة" else "Card Number") },
                                placeholder = { Text("4000 1234 5678 9010") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = expiry,
                                    onValueChange = { if (it.length <= 5) expiry = it },
                                    label = { Text(if (isArabic) "تاريخ الانتهاء" else "Expiry") },
                                    placeholder = { Text("MM/YY") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { if (it.length <= 3) cvv = it },
                                    label = { Text("CVV") },
                                    placeholder = { Text("123") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = holderName,
                                onValueChange = { holderName = it },
                                label = { Text(if (isArabic) "اسم حامل البطاقة" else "Cardholder Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isProcessing = true
                                        // Simulate bank validation delay
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            isProcessing = false
                                            isSuccess = true
                                        }, 2000)
                                    },
                                    enabled = cardNumber.length >= 12 && expiry.length >= 4 && cvv.length >= 3 && holderName.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = planColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = if (isArabic) "ادفع الآن" else "Pay Now", color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = if (isArabic) "إلغاء" else "Cancel")
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007A3E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) "تم الاشتراك بنجاح!" else "Payment Successful!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isArabic)
                                "تهانينا! تم تفعيل الاشتراك بنجاح لحسابك. استمتع الآن بالميزات الحصرية."
                            else "Congratulations! The premium subscription is now active on your account. Enjoy exclusive premium perks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )
                        Button(
                            onClick = onPaymentSuccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (isArabic) "ابدأ الآن" else "Let's Go")
                        }
                    }
                }
            }
        }
    }
}
