package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.firebase.ProUpgradeRequest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffApprovalStatus
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top bar badge component for Primary Owner (Deffas Antar) showing pending manager requests & active trial accounts.
 */
@Composable
fun OwnerManagerRequestsNotificationBadge(
    pendingCount: Int,
    trialCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = pendingCount + trialCount
    if (totalCount <= 0) return

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
        modifier = modifier.testTag("owner_manager_requests_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "طلبات المدراء الجدد",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                ) {}
            }
            Text(
                text = "$totalCount طلب/حساب تجريبي",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFB45309)
            )
        }
    }
}

/**
 * Full-screen non-dismissible Access Denied screen displayed to managers whose
 * 7-day trial period has expired and have not been permanently activated by the owner.
 * Blocks application access until activated by the primary owner (Deffas Antar),
 * and provides direct action buttons to request activation and contact the owner.
 */
@Composable
fun TrialExpiredOverlay(
    currentUser: User,
    onRequestApprovalFromOwner: () -> Unit,
    onRefreshStatus: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val semantic = LocalAFStoreSemanticColors.current

    var requestSentState by remember { mutableStateOf(false) }

    // Intercept back button to strictly enforce access denial
    BackHandler {
        Toast.makeText(
            context,
            "تم إغلاق الوصول: انتهت فترة التجربة ويجب تفعيل الحساب من المالك (Deffas Antar) للمتابعة.",
            Toast.LENGTH_LONG
        ).show()
    }

    val expiryDateStr = remember(currentUser.trialEndsAt) {
        if (currentUser.trialEndsAt > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(currentUser.trialEndsAt))
        } else {
            "غير محدد"
        }
    }

    val ownerPhone = "0662931569"
    val ownerName = "Deffas Antar"

    val whatsappRequestMessage = remember(currentUser.fullName, currentUser.storeName) {
        "السلام عليكم ورحمة الله، أنا المدير ${currentUser.fullName} لمتجر (${currentUser.storeName}). انتهت فترة التجربة المجانية لحسابي وأطلب تفعيل الحساب بشكل دائم للاستمرار في العمل. شكراً لك."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17).copy(alpha = 0.94f))
            .padding(16.dp)
            .testTag("access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, Color(0xFFEF4444)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Badge: ACCESS DENIED
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "إغلاق الوصول • ACCESS DENIED",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                item {
                    // Shield Icon with Red Circle
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "تم إغلاق الوصول - انتهاء التجربة",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.5.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Text(
                        text = "مرحباً ${currentUser.fullName}، لقد تم إغلاق الوصول إلى النظام والمبيعات لمتجرك (${currentUser.storeName}) نظراً لانتهاء فترة التجربة الأسبوعية المجانية بتاريخ $expiryDateStr.\nيرجى طلب التفعيل من المالك الرئيسي مباشرة لفتح الحساب واستئناف العمل بشكل دائم.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }

                item {
                    // Account info details card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تفاصيل الحساب المعلق:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("اسم المدير:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("اسم المتجر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser.storeName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("تاريخ الانتهاء:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(expiryDateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("حالة الحساب:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("بانتظار تفعيل المالك", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }

                item {
                    // Owner direct info card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "بيانات المالك الرئيسي للاعتماد والتفعيل:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("المالك:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$ownerName (المالك والمطور)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("الهاتف المباشر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ownerPhone, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // If request was sent, show confirmation card
                if (requestSentState) {
                    item {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                Text(
                                    text = "تم إرسال طلب التفعيل المباشر إلى المالك سحابياً بنجاح! يمكنك أيضاً التواصل المباشر أدناه للتفعيل الفوري.",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                item {
                    // Main action button: Request activation from owner directly
                    Button(
                        onClick = {
                            onRequestApprovalFromOwner()
                            requestSentState = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_owner_activation_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (requestSentState) "إعادة إرسال طلب التفعيل للمالك" else "طلب التفعيل من المالك مباشرة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                item {
                    // Contact owner directly action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ownerPhone"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "هاتف المالك: $ownerPhone", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_owner_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال هاتفياً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val waUri = Uri.parse("https://api.whatsapp.com/send?phone=213${ownerPhone.removePrefix("0")}&text=${Uri.encode(whatsappRequestMessage)}")
                                    val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح واتساب، يرجى الاتصال هاتفياً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("whatsapp_owner_btn")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    // Copy message and check status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(whatsappRequestMessage))
                                Toast.makeText(context, "تم نسخ رسالة طلب التفعيل للحافظة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_request_msg_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الرسالة", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = onRefreshStatus,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refresh_activation_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحقق من التفعيل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    // Logout button
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("access_denied_logout_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الخروج والتبديل لحساب آخر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for regular users or cashier staff to request opening a new Manager account
 * with automatic 1-week cloud synchronized trial period.
 */
@Composable
fun RequestTrialManagerAccountDialog(
    onDismiss: () -> Unit,
    onSubmitRequest: (fullName: String, phone: String, storeName: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "يرجى كتابة الاسم الكامل"
                        return@Button
                    }
                    if (storeName.isBlank()) {
                        errorMessage = "يرجى كتابة اسم المتجر / الفرع"
                        return@Button
                    }
                    onSubmitRequest(fullName.trim(), phone.trim(), storeName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد وفتح حساب تجريبي (7 أيام)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("طلب فتح حساب مدير جديد (تجربة أسبوع)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سيتم إنشاء حساب مدير جديد معزول تماماً وبفترة تجريبية تلقائية لمدة 7 أيام مع تزامن سحابي، وسيتم إرسال إشعار فوري للمالك الرئيسي (Deffas Antar) لتفعيل حسابك بشكل دائم.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = { Text("الاسم الكامل للمدير") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it; errorMessage = null },
                    label = { Text("اسم المتجر / الفرع المستقل") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { err ->
                    Text(text = err, color = Color(0xFFEF4444), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

/**
 * Control Card for Owner (Deffas Antar) in Settings to review, approve, extend, or reject
 * Trial Managers and pending account requests.
 */
@Composable
fun OwnerManagerRequestsAndTrialsCard(
    allAccounts: List<StaffAccount>,
    onApprovePermanently: (StaffAccount) -> Unit,
    onExtendTrialSevenDays: (StaffAccount) -> Unit,
    onSuspendAccount: (StaffAccount) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    val pendingOrTrialList = remember(allAccounts) {
        allAccounts.filter { acc ->
            !acc.isDeleted &&
                acc.approvalStatus != StaffApprovalStatus.DELETED &&
                acc.role.lowercase() == "manager" &&
                (!acc.isApproved || acc.isTrial || !acc.isApprovedByOwner || acc.isTrialExpired || acc.approvalStatus == StaffApprovalStatus.PENDING)
        }
    }

    var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isManagerPickerMenuOpen by remember { mutableStateOf(false) }
    var selectedManagerUid by rememberSaveable { mutableStateOf("") }

    if (pendingOrTrialList.isEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text("جميع حسابات المدراء مفعّلة ومستقرة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text("لا توجد طلبات مدراء جديدة أو حسابات تجريبية بانتظار المراجعة.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    val displayedList = remember(pendingOrTrialList, selectedManagerUid) {
        if (selectedManagerUid.isBlank()) {
            pendingOrTrialList
        } else {
            pendingOrTrialList.filter { it.userId == selectedManagerUid }
        }
    }
    val selectedManagerObj = remember(pendingOrTrialList, selectedManagerUid) {
        pendingOrTrialList.firstOrNull { it.userId == selectedManagerUid }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clickable Header to toggle the collapsible dropdown list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDropdownExpanded = !isDropdownExpanded }
                    .testTag("toggle_manager_requests_dropdown"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "طلبات المدراء والحسابات التجريبية (${pendingOrTrialList.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isDropdownExpanded) "اضغط لطي القائمة المنسدلة أو اختر مديراً محدداً" else "قائمة منسدلة • اضغط هنا لعرض ومراجعة الطلبات (${pendingOrTrialList.size})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isDropdownExpanded) "إخفاء" else "عرض (${pendingOrTrialList.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isDropdownExpanded) "إخفاء القائمة" else "عرض القائمة",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isDropdownExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dropdown Menu Selector to quickly pick a specific manager when list is long
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = semantic.cardElevated,
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isManagerPickerMenuOpen = true }
                                .testTag("manager_requests_filter_dropdown_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedManagerObj != null) {
                                        "المدير المحدد: ${selectedManagerObj.fullName.ifBlank { selectedManagerObj.email }} (${selectedManagerObj.branchName.ifBlank { "غير مسمى" }})"
                                    } else {
                                        "اختر من القائمة المنسدلة: عرض جميع المدراء (${pendingOrTrialList.size})"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "فتح القائمة المنسدلة",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isManagerPickerMenuOpen,
                            onDismissRequest = { isManagerPickerMenuOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .heightIn(max = 300.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "عرض جميع الطلبات والحسابات (${pendingOrTrialList.size})",
                                        fontWeight = if (selectedManagerUid.isBlank()) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    selectedManagerUid = ""
                                    isManagerPickerMenuOpen = false
                                }
                            )
                            HorizontalDivider()
                            pendingOrTrialList.forEach { acc ->
                                val statusLabel = when {
                                    acc.isTrialExpired -> "منتهي التجربة"
                                    acc.isTrial -> "تجريبي"
                                    else -> StaffApprovalStatus.labelAr(acc.approvalStatus)
                                }
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${acc.fullName.ifBlank { acc.email }} • $statusLabel",
                                                fontWeight = if (selectedManagerUid == acc.userId) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "المتجر: ${acc.branchName.ifBlank { "غير مسمى" }} • ${acc.phone.ifBlank { acc.email }}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedManagerUid = acc.userId
                                        isManagerPickerMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Bounded scrollable container so the list never stretches the page
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayedList.forEach { acc ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = acc.fullName.ifBlank { acc.email },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            TrialManagerUnderNameCountdownBadge(account = acc)
                                            Text(
                                                text = "المتجر: ${acc.branchName.ifBlank { "غير مسمى" }} • ${acc.phone.ifBlank { acc.email }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val badgeColor = when {
                                            acc.isTrialExpired -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                            acc.isTrial -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            acc.isApproved -> Color(0xFF10B981).copy(alpha = 0.15f)
                                            else -> Color(0xFF6B7280).copy(alpha = 0.15f)
                                        }
                                        val textColor = when {
                                            acc.isTrialExpired -> Color(0xFFDC2626)
                                            acc.isTrial -> Color(0xFFB45309)
                                            acc.isApproved -> Color(0xFF065F46)
                                            else -> Color(0xFF374151)
                                        }
                                        val badgeText = when {
                                            acc.isTrialExpired -> "منتهي التجربة (مغلق)"
                                            acc.isTrial -> "تجريبي (أسبوع)"
                                            else -> StaffApprovalStatus.labelAr(acc.approvalStatus)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onApprovePermanently(acc) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (acc.isTrialExpired || acc.approvalStatus == StaffApprovalStatus.SUSPENDED) "سماح وتفعيل دائم" else "تفعيل دائم",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onExtendTrialSevenDays(acc) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (acc.isPending) "قبول تجريبي (7 أيام)" else "+7 أيام تجربة",
                                                fontSize = 11.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onSuspendAccount(acc) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("إيقاف", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Concise live countdown timer badge displayed directly under a Trial Independent Manager's name
 * in the Owner's management views.
 */
@Composable
fun TrialManagerUnderNameCountdownBadge(
    account: StaffAccount,
    modifier: Modifier = Modifier
) {
    if (!account.isTrial || account.isOwner || (account.role != "manager" && account.requestedRole != "manager")) {
        return
    }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(account.trialEndsAtMillis, account.approvalStatus, account.isTrial) {
        if (account.trialEndsAtMillis > 0L && account.isApproved) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    val remainingMs = if (account.trialEndsAtMillis > 0L) {
        (account.trialEndsAtMillis - nowMillis).coerceAtLeast(0L)
    } else {
        0L
    }

    val isExpired = (account.trialEndsAtMillis > 0L && remainingMs <= 0L) ||
        (account.approvalStatus == StaffApprovalStatus.SUSPENDED && account.isTrial)

    val days = remainingMs / (24L * 3600L * 1000L)
    val hours = (remainingMs / (3600L * 1000L)) % 24L
    val minutes = (remainingMs / (60L * 1000L)) % 60L
    val seconds = (remainingMs / 1000L) % 60L

    val badgeBgColor = when {
        account.isPending -> Color(0xFFF59E0B).copy(alpha = 0.14f)
        isExpired -> Color(0xFFEF4444).copy(alpha = 0.16f)
        days <= 1L -> Color(0xFFD97706).copy(alpha = 0.18f)
        else -> Color(0xFF0284C7).copy(alpha = 0.14f)
    }
    val badgeBorderColor = when {
        account.isPending -> Color(0xFFF59E0B).copy(alpha = 0.55f)
        isExpired -> Color(0xFFEF4444).copy(alpha = 0.65f)
        days <= 1L -> Color(0xFFD97706).copy(alpha = 0.65f)
        else -> Color(0xFF0284C7).copy(alpha = 0.55f)
    }
    val badgeTextColor = when {
        account.isPending -> Color(0xFFB45309)
        isExpired -> Color(0xFFDC2626)
        days <= 1L -> Color(0xFFB45309)
        else -> Color(0xFF0369A1)
    }

    val timerLabelText = when {
        account.isPending -> "⏱ حساب Pro تجريبي (7 أيام) • يبدأ المؤقت فور القبول"
        isExpired -> "⛔ انتهت مدة التجربة (7 أيام) • متوقف بانتظار سماح المالك"
        account.trialEndsAtMillis > 0L -> String.format(
            Locale.US,
            "⏱ مؤقت التجربة: متبقي %d يوم • %02d:%02d:%02d",
            days,
            hours,
            minutes,
            seconds
        )
        else -> "⏱ حساب Pro تجريبي (7 أيام)"
    }

    Surface(
        shape = RoundedCornerShape(7.dp),
        color = badgeBgColor,
        border = BorderStroke(0.8.dp, badgeBorderColor),
        modifier = modifier.testTag("owner_under_name_trial_timer_${account.userId}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isExpired) Icons.Default.LockClock else Icons.Default.Timer,
                contentDescription = null,
                tint = badgeTextColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = timerLabelText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Live 7-day countdown timer bar shown to the Trial Independent Manager during their session.
 * Automatically triggers [onTrialExpiredAutoTerminate] the moment the 7-day timer reaches zero.
 */
@Composable
fun ActiveTrialManagerSessionCountdownBar(
    account: StaffAccount,
    onTrialExpiredAutoTerminate: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!account.isTrial || account.isOwner || !account.isApproved || account.trialEndsAtMillis <= 0L) {
        return
    }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasTriggeredAutoTermination by remember(account.userId, account.trialEndsAtMillis) {
        mutableStateOf(false)
    }

    LaunchedEffect(account.userId, account.trialEndsAtMillis, account.isTrial, account.isApproved) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            if (current >= account.trialEndsAtMillis && !hasTriggeredAutoTermination) {
                hasTriggeredAutoTermination = true
                onTrialExpiredAutoTerminate()
                break
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    val remainingMs = (account.trialEndsAtMillis - nowMillis).coerceAtLeast(0L)
    val totalSevenDaysMs = 7L * 24L * 3600L * 1000L
    val progress = (remainingMs.toFloat() / totalSevenDaysMs.toFloat()).coerceIn(0f, 1f)

    val days = remainingMs / (24L * 3600L * 1000L)
    val hours = (remainingMs / (3600L * 1000L)) % 24L
    val minutes = (remainingMs / (60L * 1000L)) % 60L
    val seconds = (remainingMs / 1000L) % 60L

    val isUrgent = days < 1L
    val accentColor = if (isUrgent) Color(0xFFDC2626) else Color(0xFFD97706)

    Surface(
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("trial_manager_live_countdown_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "حساب Pro تجريبي (7 أيام):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "متبقي %d يوم • %02d:%02d:%02d",
                            days,
                            hours,
                            minutes,
                            seconds
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Text(
                text = "عند انتهاء مدة 7 أيام يتم إنهاء الجلسة تلقائياً ويتوقف الدخول للتطبيق حتى يسمح المالك (Deffas antar).",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for a manager to request an upgrade to Pro mode by providing their contact phone number.
 */
@Composable
fun ManagerProUpgradeRequestDialog(
    initialPhone: String,
    onDismiss: () -> Unit,
    onSubmit: (phone: String, note: String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone) }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val cleanPhone = phone.trim()
                    if (cleanPhone.length < 8) {
                        validationError = "يرجى كتابة رقم هاتف صحيح (8 أرقام على الأقل) ليتواصل معك المالك."
                    } else {
                        onSubmit(cleanPhone, note.trim())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال طلب الترقية للمالك 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "طلب ترقية الحساب إلى نسخة Pro ⭐",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "تنويه هام للمدير:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "سيصل طلبك مباشرة إلى مالك النظام (Deffas antar) مرفقاً برقم هاتفك ليتواصل معك لتفعيل المزامنة اللحظية الشاملة وإتاحة فتح حسابات لعمالك.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validationError = null
                    },
                    label = { Text("رقم هاتفك المحمول (إلزامي للاتصال) *") },
                    placeholder = { Text("مثال: 0661234567") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = validationError?.let { err -> { Text(err, color = Color(0xFFEF4444), fontSize = 11.sp) } },
                    modifier = Modifier.fillMaxWidth().testTag("pro_upgrade_phone_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة أو استفسار للمالك (اختياري)") },
                    placeholder = { Text("مثال: أحتاج إضافة 3 عمال لفرعنا الجديد...") },
                    leadingIcon = {
                        Icon(Icons.Default.Chat, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/**
 * Card inside Settings showing manager subscription status (Lite vs Pro) and feature breakdown,
 * with a button to request upgrade to Pro mode with manager phone number.
 */
@Composable
fun ManagerSubscriptionStatusCard(
    myStaffAccount: StaffAccount?,
    myProUpgradeRequest: ProUpgradeRequest?,
    onRequestUpgrade: (phone: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (myStaffAccount == null) return
    val semantic = LocalAFStoreSemanticColors.current
    val isPro = myStaffAccount.syncMode == "pro" || myStaffAccount.isOwner
    var showUpgradeDialog by remember { mutableStateOf(false) }

    if (showUpgradeDialog) {
        ManagerProUpgradeRequestDialog(
            initialPhone = myProUpgradeRequest?.phone?.ifBlank { myStaffAccount.phone } ?: myStaffAccount.phone,
            onDismiss = { showUpgradeDialog = false },
            onSubmit = { phone, note ->
                onRequestUpgrade(phone, note)
            }
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFF3B82F6).copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isPro) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF3B82F6).copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth().testTag("manager_subscription_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon + Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPro) Icons.Default.CloudSync else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isPro) Color(0xFF10B981) else Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "حالة الاشتراك وخطة التشغيل",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "الحساب: ${myStaffAccount.fullName} • ${myStaffAccount.branchName.ifBlank { "الفرع الحالي" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPro) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(
                        1.dp,
                        if (isPro) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFF59E0B).copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPro) Icons.Default.Star else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isPro) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPro) "نسخة Pro نشطة 🚀" else "نسخة Lite (أوفلاين) ⚡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPro) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }
            }

            // Description banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.dp, semantic.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isPro) {
                            "حسابك مفعّل بكامل خصائص المزامنة السحابية الفورية. يتم مزامنة المخزون، المبيعات، ومواقع وفواتير العمال لحظياً وبشكل تلقائي."
                        } else {
                            "أنت تستخدم وضع العمل دون إنترنت (Just offline / Lite). يمكنك استخدام كافة خصائص المبيعات والفواتير محلياً على جهازك، بينما تتطلب مزامنة السحابة وفتح حسابات العمال الترقية إلى Pro."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )

                    // Features Comparison Grid / Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Lite Features Column
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "باقة Lite (الحالية)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("✓ تسجيل ومتابعة المبيعات", fontSize = 10.sp)
                                Text("✓ إدارة المخزون محلياً", fontSize = 10.sp)
                                Text("✓ طباعة فواتير البلوتوث", fontSize = 10.sp)
                                Text("✗ لا تشمل مزامنة العمال", fontSize = 10.sp, color = Color(0xFFEF4444))
                                Text("✗ منع إنشاء حسابات عمال", fontSize = 10.sp, color = Color(0xFFEF4444))
                            }
                        }

                        // Pro Features Column
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "باقة Pro الكاملة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Text("⭐ مزامنة سحابية لحظية", fontSize = 10.sp, color = Color(0xFF047857))
                                Text("⭐ إنشاء حسابات العمال برمز سري", fontSize = 10.sp, color = Color(0xFF047857))
                                Text("⭐ ربط الفروع وتعدد الأجهزة", fontSize = 10.sp, color = Color(0xFF047857))
                                Text("⭐ تتبع مواقع ومبيعات العمال", fontSize = 10.sp, color = Color(0xFF047857))
                            }
                        }
                    }
                }
            }

            // Upgrade Request State (for Lite managers)
            if (!isPro) {
                val req = myProUpgradeRequest
                when {
                    req != null && req.isPending -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "طلبك لترقية Pro قيد المراجعة لدى المالك (Deffas antar) ⏳",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = "رقم هاتفك المسجل للاتصال: ${req.phone}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (req.note.isNotBlank()) {
                                    Text(
                                        text = "ملاحظتك: ${req.note}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "سيقوم المالك بالتواصل معك عبر هاتفك والموافقة على طلبك لتفعيل Pro مباشرة دون الحاجة لإعادة تثبيت التطبيق.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { showUpgradeDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل رقم الهاتف أو الملاحظة", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    req != null && req.isApproved -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "تمت الموافقة على ترقية حسابك إلى Pro! جاري تحديث الجلسة والمزامنة...",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    req != null && req.isRejected -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "تم رفض طلب الترقية السابق من قبل المالك.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Text(
                                    text = "يمكنك تقديم طلب جديد برقم هاتف صحيح أو التواصل هاتفياً مع المالك.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showUpgradeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة تقديم طلب ترقية Pro", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    else -> {
                        // Standard state: button to request Pro upgrade
                        Button(
                            onClick = { showUpgradeDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("request_pro_upgrade_btn")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⭐ طلب ترقية الحساب إلى نسخة Pro وتفعيل العمال",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Control Card for Store Owner (Deffas Antar) in Settings to review, call, approve, or reject
 * managers' Pro Upgrade requests.
 */
@Composable
fun OwnerProUpgradeRequestsCard(
    upgradeRequests: List<ProUpgradeRequest>,
    onApproveRequest: (ProUpgradeRequest) -> Unit,
    onRejectRequest: (ProUpgradeRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val semantic = LocalAFStoreSemanticColors.current

    val pendingRequests = remember(upgradeRequests) {
        upgradeRequests.filter { it.isPending }
    }

    if (pendingRequests.isEmpty()) {
        return
    }

    var isProRequestsExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.10f)),
        border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().testTag("owner_pro_upgrade_requests_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with badge (clickable dropdown toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isProRequestsExpanded = !isProRequestsExpanded }
                    .testTag("toggle_pro_upgrade_requests_dropdown"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "طلبات ترقية المدراء إلى Pro (${pendingRequests.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = if (isProRequestsExpanded) "اضغط لطي القائمة المنسدلة" else "قائمة منسدلة • اضغط لعرض طلبات الترقية (${pendingRequests.size})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isProRequestsExpanded) "إخفاء" else "جديد (${pendingRequests.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Icon(
                            imageVector = if (isProRequestsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Collapsible list of pending upgrade requests
            AnimatedVisibility(visible = isProRequestsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingRequests.forEach { req ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Manager info row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = req.managerName.ifBlank { "مدير بدون اسم" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "المتجر: ${req.branchName.ifBlank { "الفرع الرئيسي" }} • ${req.managerEmail}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "طلب ترقية Pro ⏳",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Prominent Phone Number Container with Direct Call & Copy
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "رقم هاتف المدير للتواصل:",
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFF047857),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = req.phone.ifBlank { "غير مسجل" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (req.phone.isNotBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${req.phone}"))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = "اتصال",
                                                        tint = Color(0xFF047857),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(req.phone))
                                                        Toast.makeText(context, "تم نسخ رقم الهاتف: ${req.phone}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "نسخ",
                                                        tint = Color(0xFF047857),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Optional note
                                if (req.note.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ملاحظة المدير: ${req.note}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Action buttons: Approve vs Reject
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApproveRequest(req) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبول وتفعيل Pro ✓", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onRejectRequest(req) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("رفض", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

