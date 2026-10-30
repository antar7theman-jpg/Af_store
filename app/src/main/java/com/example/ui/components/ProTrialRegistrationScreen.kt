package com.example.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.example.ui.theme.LocalAFStoreSemanticColors
import kotlinx.coroutines.launch

/**
 * Full dedicated registration screen for requesting a 7-day Pro Trial Manager Account.
 * On completion, notifies the primary store owner (Deffas Antar) for approval/rejection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTrialRegistrationScreen(
    onBackToLogin: () -> Unit,
    onRegistrationSubmitted: (StaffAccount) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var managerPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var businessType by rememberSaveable { mutableStateOf("") }

    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessConfirmation by remember { mutableStateOf(false) }
    var submittedAccount by remember { mutableStateOf<StaffAccount?>(null) }

    BackHandler {
        if (!isSubmitting) {
            onBackToLogin()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = if (semantic.isDark) Color(0xFF0B1120) else Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD97706).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "باقة Pro",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                        Text(
                            text = "فتح حساب Pro تجريبي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLogin,
                        enabled = !isSubmitting,
                        modifier = Modifier.testTag("pro_trial_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع لشاشة الدخول"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFF0284C7))
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تجربة مجانية لمدة 7 أيام",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (semantic.isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF059669).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "مزامنة سحابية",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "امتلك متجرك الخاص بصلاحيات مدير مستقل وتجربة متكاملة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Feature Badges Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProFeatureBadge(
                                icon = Icons.Default.CloudDone,
                                text = "مزامنة سحابية",
                                modifier = Modifier.weight(1f)
                            )
                            ProFeatureBadge(
                                icon = Icons.Default.Group,
                                text = "حسابات للعمال",
                                modifier = Modifier.weight(1f)
                            )
                            ProFeatureBadge(
                                icon = Icons.Default.Security,
                                text = "تشفير وحماية",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Error Message Notice
                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Registration Form Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "بيانات المدير والفرع الجديد:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Full Name Field
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; errorMessage = null },
                            label = { Text("الاسم الكامل للمدير *") },
                            placeholder = { Text("مثال: عبد الرحمن محمد") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_trial_fullname_input")
                        )

                        // Store / Branch Name Field
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it; errorMessage = null },
                            label = { Text("اسم المتجر أو الفرع *") },
                            placeholder = { Text("مثال: فرع المدينة المنورة / متجر النور") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_trial_storename_input")
                        )

                        // Phone Field
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = null },
                            label = { Text("رقم الهاتف (للتواصل والتأكيد) *") },
                            placeholder = { Text("مثال: 0551234567") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_trial_phone_input")
                        )

                        // Secret PIN Field
                        OutlinedTextField(
                            value = managerPin,
                            onValueChange = {
                                if (it.length <= 8) {
                                    managerPin = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("رمز الدخول السري (4 إلى 8 أرقام) *") },
                            placeholder = { Text("مثال: 1234") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPasswordVisible) "إخفاء الرمز" else "إظهار الرمز"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_trial_pin_input")
                        )

                        // Confirm PIN Field
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = {
                                if (it.length <= 8) {
                                    confirmPin = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("تأكيد الرمز السري *") },
                            placeholder = { Text("أعد كتابة الرمز السري") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pro_trial_confirm_pin_input")
                        )

                        // Business Type (Optional)
                        OutlinedTextField(
                            value = businessType,
                            onValueChange = { businessType = it },
                            label = { Text("نوع النشاط التجاري (اختياري)") },
                            placeholder = { Text("سوبرماركت، ملابس، كافيه، عطارة، إلخ...") },
                            singleLine = true,
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val cleanName = fullName.trim()
                                val cleanStore = storeName.trim()
                                val cleanPhone = phone.trim()
                                val cleanEmail = email.trim()
                                val cleanPin = managerPin.trim()
                                val cleanConfirm = confirmPin.trim()

                                if (cleanName.length < 2) {
                                    errorMessage = "يرجى إدخال اسم المدير كاملاً"
                                    return@Button
                                }
                                if (cleanStore.length < 2) {
                                    errorMessage = "يرجى إدخال اسم المتجر أو المنشأة"
                                    return@Button
                                }
                                if (cleanPhone.length < 8) {
                                    errorMessage = "يرجى إدخال رقم هاتف صحيح ليتواصل معك المالك لتأكيد الحساب"
                                    return@Button
                                }
                                if (cleanPin.length < 4) {
                                    errorMessage = "يرجى تعيين رمز سري لا يقل عن 4 أرقام"
                                    return@Button
                                }
                                if (cleanPin != cleanConfirm) {
                                    errorMessage = "الرمز السري وتأكيد الرمز غير متطابقين!"
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null

                                coroutineScope.launch {
                                    val result = StaffAccountRepository.registerProTrialManagerAccount(
                                        context = context,
                                        fullName = cleanName,
                                        storeName = cleanStore,
                                        phone = cleanPhone,
                                        email = cleanEmail,
                                        managerSecretPin = cleanPin,
                                        businessType = businessType.trim()
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        val account = result.getOrThrow()
                                        submittedAccount = account
                                        showSuccessConfirmation = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage
                                            ?: "تعذر إرسال طلب الحساب التجريبي. يرجى التحقق من الاتصال بالإنترنت."
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pro_trial_submit_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارِ إرسال الطلب وإشعار المالك...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تأكيد وطلب فتح الحساب التجريبي (7 أيام) 🚀",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Back to login button
                        OutlinedButton(
                            onClick = onBackToLogin,
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إلغاء والرجوع لشاشة الدخول",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Safety & Terms Note
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💡 آلية التفعيل الفوري:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "فور إرسال الطلب، يتلقى المالك (Deffas Antar) إشعاراً فورياً على هاتفه بقبول أو رفض فتح حساب التجربة (7 أيام). عند قبول الطلب، ستتمكن من تسجيل الدخول مباشرة برمزك السري والمزامنة سحابياً.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Success Confirmation Dialog
    if (showSuccessConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showSuccessConfirmation = false
                submittedAccount?.let { onRegistrationSubmitted(it) }
                onBackToLogin()
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF059669).copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "🎉 تم إرسال طلب الحساب التجريبي بنجاح!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.5.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تم إنشاء ملفك وإرسال إشعار فوري للمالك (Deffas Antar) لمراجعة طلب باقة Pro التجريبية (7 أيام).",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "• الحساب: ${submittedAccount?.fullName}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• الفرع: ${submittedAccount?.branchName}",
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "• الحالة: بانتظار قبول المالك ⏳",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                    Text(
                        text = "بمجرد قبول المالك للطلب، ستتمكن من الدخول فوراً بالرمز السري الذي اخترته.",
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessConfirmation = false
                        submittedAccount?.let { onRegistrationSubmitted(it) }
                        onBackToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("العودة لشاشة الدخول الرئيسية", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

@Composable
private fun ProFeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
