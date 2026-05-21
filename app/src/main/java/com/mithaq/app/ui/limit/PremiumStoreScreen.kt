package com.mithaq.app.ui.limit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mithaq.app.ui.auth.AuthViewModel

data class SubscriptionPlanItem(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val price: String,
    val periodAr: String,
    val periodEn: String,
    val featuresAr: List<String>,
    val featuresEn: List<String>,
    val color: Color,
    val gradient: Brush
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumStoreScreen(
    viewModel: AuthViewModel,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf<SubscriptionPlanItem?>(null) }
    var showCheckout by remember { mutableStateOf(false) }

    val plans = listOf(
        SubscriptionPlanItem(
            id = "silver",
            nameEn = "Silver Plan",
            nameAr = "الباقة الفضية",
            price = "$9.99",
            periodAr = "شهرياً",
            periodEn = "/ month",
            featuresAr = listOf(
                "عرض 20 تقرير توافق يومياً",
                "فتح الحظر المضبب لـ 5 صور شخصية",
                "إرسال دعوة Wali إضافية",
                "دعم الترجمة الفورية في المحادثات"
            ),
            featuresEn = listOf(
                "View 20 compatibility scores daily",
                "Unlock modesty blur for 5 profiles",
                "Invite an additional Wali",
                "Instant bilingual chat translation"
            ),
            color = Color(0xFF9E9E9E),
            gradient = Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C)))
        ),
        SubscriptionPlanItem(
            id = "gold",
            nameEn = "Gold Plan",
            nameAr = "الباقة الذهبية",
            price = "$19.99",
            periodAr = "شهرياً",
            periodEn = "/ month",
            featuresAr = listOf(
                "كل ميزات الفضية",
                "عدد غير محدود من تقارير التوافق",
                "فتح الحظر الكامل للصور بعد موافقة ولي الأمر",
                "أولوية الظهور في نتائج البحث والتصفح",
                "بدون حد أقصى للرسائل اليومية"
            ),
            featuresEn = listOf(
                "All Silver Plan features",
                "Unlimited compatibility reports",
                "Full photo unlock approval by Wali",
                "Priority listing in match searches",
                "No daily message limits"
            ),
            color = Color(0xFFD4AF37),
            gradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000)))
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "متجر ميثاق المميز" else "Mithaq Premium Store",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Promo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "ارتقِ بتجربتك اليوم!" else "Upgrade Your Journey!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isArabic)
                            "احصل على توثيق أسرع، تواصل بلا حدود، وميزات أمان إضافية لضمان العثور على شريك حياتك بوقار وموثوقية."
                        else "Unlock unlimited chats, priority searches, and verified trust badges to find your partner in a respectful manner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Plans listing
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isArabic = isArabic,
                    onSelect = {
                        selectedPlan = plan
                        showCheckout = true
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showCheckout && selectedPlan != null) {
        SimulatedCheckoutDialog(
            plan = selectedPlan!!,
            isArabic = isArabic,
            onDismiss = { showCheckout = false },
            onPaymentSuccess = {
                viewModel.purchasePremiumPlan(selectedPlan!!.id)
                showCheckout = false
                onBack()
            }
        )
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlanItem,
    isArabic: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = plan.gradient,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header with Gradient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) plan.nameAr else plan.nameEn,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = plan.color
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(plan.gradient)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = plan.price,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features checklist
            val features = if (isArabic) plan.featuresAr else plan.featuresEn
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = plan.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = plan.color
                )
            ) {
                Text(
                    text = if (isArabic) "اشترك الآن" else "Subscribe Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SimulatedCheckoutDialog(
    plan: SubscriptionPlanItem,
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
                            text = "${if (isArabic) plan.nameAr else plan.nameEn} - ${plan.price}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = plan.color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(40.dp))
                            CircularProgressIndicator(color = plan.color)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "جاري معالجة الدفع..." else "Processing payment...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        } else {
                            // Fields
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
                                        }, 2500)
                                    },
                                    enabled = cardNumber.length >= 12 && expiry.length >= 4 && cvv.length >= 3 && holderName.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = plan.color),
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
                    // Success View
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
                                "تهانينا! تم تفعيل ${plan.nameAr} لحسابك بنجاح. استمتع الآن بالميزات الحصرية."
                            else "Congratulations! The ${plan.nameEn} is now active on your account. Enjoy exclusive premium perks.",
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
