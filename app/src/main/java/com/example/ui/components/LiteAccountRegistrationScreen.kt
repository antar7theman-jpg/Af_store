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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
 * Dedicated registration screen for opening a Free Lite / Offline Account.
 * Immediate instant activation (Status: APPROVED) without requiring store owner approval.
 * Automatically notifies the store owner (Deffas Antar) with account registration metadata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteAccountRegistrationScreen(
    onBackToLogin: () -> Unit,
    onAccountCreatedAndLoggedIn: (StaffAccount) -> Unit
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

    BackHandler {
        if (!isSubmitting) {
            onBackToLogin()
        }
    }

    val emeraldColor = Color(0xFF059669)
    val emeraldDark = Color(0xFF047857)

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
                            color = emeraldColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, emeraldColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = emeraldColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Lite / Offline",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = emeraldColor
                                )
                            }
                        }
                        Text(
                            text = "فتح حساب Lite مجاني",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLogin,
                        enabled = !isSubmitting,
                        modifier = Modifier.testTag("lite_reg_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (semantic.isDark) Color(0xFF0F172A) else Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Visual Hero Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = emeraldDark,
                    border = BorderStroke(1.2.dp, emeraldColor),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669),
                                        Color(0xFF047857)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "حساب Lite / Offline مجاني بالكامل",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.5.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "تفعيل فوري دون انتظار موافقة المالك - ابدأ البيع الآن",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Features highlight chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "تفعيل فوري تلقائي",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "مجاني بنسبة 100%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "إشعار شفاف للمالك",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Message banner
                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Registration Form Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        1.dp,
                        if (semantic.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(
                            text = "بيانات المتجر والمدير للتفعيل الفوري:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Store / Business Name
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it; errorMessage = null },
                            label = { Text("اسم المتجر / النشاط التجاري *") },
                            placeholder = { Text("مثال: سوبرماركت النور، متجر الأناقة") },
                            leadingIcon = {
                                Icon(Icons.Default.Store, contentDescription = null, tint = emeraldColor)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_store_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; errorMessage = null },
                            label = { Text("اسم المستخدم / المدير *") },
                            placeholder = { Text("الاسم الكامل لصاحب الحساب") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = emeraldColor)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_full_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        // Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = null },
                            label = { Text("رقم الهاتف للتواصل *") },
                            placeholder = { Text("05xxxxxxxx أو 06xxxxxxxx") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = emeraldColor)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        // PIN Code
                        OutlinedTextField(
                            value = managerPin,
                            onValueChange = {
                                if (it.length <= 16) {
                                    managerPin = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("رمز الدخول السري (PIN) *") },
                            placeholder = { Text("4 أرقام أو أكثر للدخول لاحقاً") },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = emeraldColor)
                            },
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        // Confirm PIN
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = {
                                if (it.length <= 16) {
                                    confirmPin = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("تأكيد رمز الدخول السري *") },
                            placeholder = { Text("أعد كتابة الرمز السري") },
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = null, tint = emeraldColor)
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_confirm_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        // Business Type (Optional)
                        OutlinedTextField(
                            value = businessType,
                            onValueChange = { businessType = it },
                            label = { Text("نوع النشاط التجاري (اختياري)") },
                            placeholder = { Text("مثال: مواد غذائية، مطعم، أدوات، أخرى") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lite_reg_business_type_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldColor,
                                focusedLabelColor = emeraldColor
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (fullName.trim().length < 2) {
                                    errorMessage = "يرجى إدخال اسم المستخدم / المدير بشكل صحيح"
                                    return@Button
                                }
                                if (storeName.trim().length < 2) {
                                    errorMessage = "يرجى إدخال اسم المتجر أو النشاط"
                                    return@Button
                                }
                                if (phone.trim().length < 8) {
                                    errorMessage = "يرجى إدخال رقم هاتف صحيح للتواصل"
                                    return@Button
                                }
                                if (managerPin.trim().length < 4) {
                                    errorMessage = "يجب أن يتكون رمز الدخول السري من 4 خانات على الأقل"
                                    return@Button
                                }
                                if (managerPin.trim() != confirmPin.trim()) {
                                    errorMessage = "رمز الدخول السري وتأكيده غير متطابقين"
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null

                                coroutineScope.launch {
                                    val result = StaffAccountRepository.createLiteManagerAccount(
                                        context = context,
                                        fullName = fullName.trim(),
                                        storeName = storeName.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        managerSecretPin = managerPin.trim(),
                                        businessType = businessType.trim()
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        val account = result.getOrThrow()
                                        onAccountCreatedAndLoggedIn(account)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage
                                            ?: "تعذر إنشاء الحساب، يرجى التحقق من الاتصال والمحاولة لاحقاً"
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("lite_reg_submit_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جاري تفعيل الحساب فورياً وإشعار المالك...",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تفعيل الحساب فوراً والدخول للبرنامج",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Informational Note
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = emeraldColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, emeraldColor.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = emeraldColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "حسابات Lite مجانية بالكامل ولا تحتاج لموافقة المالك. سيصل إشعار فوري للمالك بمعلومات تسجيلك لتوثيق المتجر فقط.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (semantic.isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
