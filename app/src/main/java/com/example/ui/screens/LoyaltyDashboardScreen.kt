package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Customer
import com.example.data.models.RecordSyncState
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.ui.screens.UniversalReportHeaderButton
import com.example.ui.screens.UniversalSectionPdfReportDialog
import com.example.ui.screens.UniversalPdfReportExporter
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatryk.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatryk.vico.core.entry.entryModelOf
import com.patrykandpatryk.vico.core.entry.FloatEntry
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyDashboardScreen(
    viewModel: AntarSalesViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle(initialValue = emptyList())
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()
    val semantic = LocalAFStoreSemanticColors.current

    // Summary calculations
    val totalPointsDistributed = remember(customers) { customers.sumOf { it.loyaltyPoints } }
    val enrolledCustomersCount = remember(customers) { customers.size }
    val activeGoalAchievers = remember(customers) { customers.count { it.loyaltyPoints >= it.loyaltyGoal } }
    
    val rankedCustomers = remember(customers) {
        customers.sortedByDescending { it.loyaltyPoints }
    }

    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) }
    var showPdfDialog by remember { mutableStateOf<UniversalPdfReportPayload?>(null) }
    var showEditCampaignDialog by remember { mutableStateOf(false) }

    val loyaltyGoalDefault by viewModel.loyaltyGoalDefault.collectAsStateWithLifecycle()
    val loyaltyRewardDefault by viewModel.loyaltyRewardDefault.collectAsStateWithLifecycle()
    val loyaltySpendPerPoint by viewModel.loyaltySpendPerPoint.collectAsStateWithLifecycle()

    if (showPdfDialog != null) {
        UniversalSectionPdfReportDialog(
            payload = showPdfDialog!!,
            onDismiss = { showPdfDialog = null }
        )
    }

    if (showEditCampaignDialog) {
        EditLoyaltyCampaignDialog(
            initialGoal = loyaltyGoalDefault,
            initialReward = loyaltyRewardDefault,
            initialSpendPerPoint = loyaltySpendPerPoint,
            currencySymbol = currencySymbol,
            onDismiss = { showEditCampaignDialog = false },
            onSave = { goal, reward, spend, applyAll ->
                viewModel.updateLoyaltyCampaignSettings(goal, reward, spend, applyAll)
                showEditCampaignDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "لوحة تحكم ولاء العملاء",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إحصائيات النقاط وأهداف حملات المبيعات الحالية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("loyalty_dashboard_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    UniversalReportHeaderButton(
                        title = "تصدير PDF",
                        onClick = {
                            showPdfDialog = UniversalPdfReportExporter.buildLoyaltyPayload(
                                storeName = storeName,
                                storePhone = storePhone,
                                currencySymbol = currencySymbol,
                                topCustomers = rankedCustomers,
                                totalPoints = totalPointsDistributed
                            )
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اتجاه تراكم نقاط الولاء (آخر 30 يوم)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val chartEntryModel = remember(customers) {
                            val trend: List<Pair<Long, Int>> = viewModel.getLoyaltyPointsTrend()
                            entryModelOf(*trend.mapIndexed { index, pair ->
                                index.toFloat() to pair.second.toFloat()
                            }.toTypedArray())
                        }
                        Chart(
                            chart = lineChart(),
                            model = chartEntryModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis()
                        )
                    }
                }
            }
            // 1. High-Fidelity Loyalty Summary Cards (Dashboard Banner)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Points Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("النقاط الموزعة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = totalPointsDistributed.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Enrolled Customers Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A).copy(alpha = if (semantic.isDark) 0.5f else 0.05f)
                        ),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = semantic.infoBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("العملاء المنضمون", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = enrolledCustomersCount.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.infoBlue
                            )
                        }
                    }

                    // Goal Achievers Card
                    Card(
                        modifier = Modifier.weight(1.1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF065F46).copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF047857).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("مكافآت جاهزة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$activeGoalAchievers عملاء",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // 2. Active Redemption Campaign Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "الحملة النشطة لتسييل النقاط",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF047857).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "نشط حالياً ✓",
                                        color = Color(0xFF047857),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable { showEditCampaignDialog = true }.testTag("edit_loyalty_campaign_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الشروط والمبالغ",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "تعديل الشروط والمبالغ",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = semantic.cardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "قسيمة الخصم المالي للعملاء الأوفياء",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "يحصل العميل على قسيمة خصم مالي قابلة للخصم التلقائي مباشرة من فاتورة المبيعات القادمة عند تسييل الرمز داخل شاشة مراجعة المبيعات والكاشير.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("شروط التسييل", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$loyaltyGoalDefault نقطة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp), color = semantic.cardBorder)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("قيمة كوبون الخصم", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format(Locale.US, "%.2f", loyaltyRewardDefault)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                VerticalDivider(modifier = Modifier.height(24.dp), color = semantic.cardBorder)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("معدل الكسب", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("1 نقطة / ${loyaltySpendPerPoint.toInt()} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }



            // 3. Top Customers standings list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "قائمة ترتيب العملاء ببرنامج الولاء",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "الترتيب بالنقاط",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (rankedCustomers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GroupOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا يوجد عملاء مسجلون حالياً",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = rankedCustomers,
                    key = { index, customer -> "loyalty_${customer.id}_$index" }
                ) { index, customer ->
                    val progress = (customer.loyaltyPoints.toFloat() / customer.loyaltyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    val isGoalMet = customer.loyaltyPoints >= customer.loyaltyGoal

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerForEdit = customer }
                            .testTag("loyalty_customer_card_${customer.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isGoalMet) 1.2.dp else 0.8.dp,
                            color = if (isGoalMet) Color(0xFF10B981).copy(alpha = 0.6f) else semantic.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left section (Badge & Details)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Podium Rank Badge
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            color = when (index) {
                                                0 -> Color(0xFFFEF3C7) // Gold
                                                1 -> Color(0xFFF1F5F9) // Silver
                                                2 -> Color(0xFFFFEDD5) // Bronze
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when (index) {
                                                0 -> Color(0xFFF59E0B)
                                                1 -> Color(0xFF94A3B8)
                                                2 -> Color(0xFFD97706)
                                                else -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (index) {
                                            0 -> "🥇"
                                            1 -> "🥈"
                                            2 -> "🥉"
                                            else -> "#${index + 1}"
                                        },
                                        fontSize = if (index < 3) 14.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index >= 3) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                                    )
                                }

                                // Name and Points Progress bar
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGoalMet) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right section (Points counter & Rewards status)
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${customer.loyaltyPoints} / ${customer.loyaltyGoal} نقطة",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )

                                if (isGoalMet) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFD1FAE5)
                                    ) {
                                        Text(
                                            text = "مكافأة جاهزة 🎉",
                                            color = Color(0xFF065F46),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "المشتريات: ${String.format(Locale.US, "%.0f", customer.totalPurchases)} د.ج",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Inline Quick Loyalty Editor Dialog Modal
    selectedCustomerForEdit?.let { target ->
        val coroutineScope = rememberCoroutineScope()
        var editPoints by remember { mutableStateOf(target.loyaltyPoints.toString()) }
        var editGoal by remember { mutableStateOf(target.loyaltyGoal.toString()) }
        var editReward by remember { mutableStateOf(target.loyaltyRewardValue.toString()) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { selectedCustomerForEdit = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تعديل نقاط ولاء: ${target.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { selectedCustomerForEdit = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                        }
                    }

                    Divider(color = semantic.cardBorder.copy(alpha = 0.5f))

                    // Points Input Field
                    OutlinedTextField(
                        value = editPoints,
                        onValueChange = { editPoints = it },
                        label = { Text("نقاط الولاء الحالية", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_points_input"),
                        singleLine = true
                    )

                    // Goal Input Field
                    OutlinedTextField(
                        value = editGoal,
                        onValueChange = { editGoal = it },
                        label = { Text("الهدف المطلوب لتسييل المكافأة (نقطة)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_goal_input"),
                        singleLine = true
                    )

                    // Reward Value Field
                    OutlinedTextField(
                        value = editReward,
                        onValueChange = { editReward = it },
                        label = { Text("قيمة الخصم النقدي المكافئ ($currencySymbol)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_reward_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { selectedCustomerForEdit = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val pts = editPoints.toIntOrNull()
                                val gl = editGoal.toIntOrNull()
                                val rwd = editReward.toDoubleOrNull()
                                if (pts == null || gl == null || rwd == null || gl <= 0) {
                                    Toast.makeText(context, "الرجاء إدخال قيم صالحة للمدخلات ⚠️", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        val updated = target.copy(
                                            loyaltyPoints = pts,
                                            loyaltyGoal = gl,
                                            loyaltyRewardValue = rwd,
                                            updatedAt = System.currentTimeMillis(),
                                            syncStatus = RecordSyncState.PENDING
                                        )
                                        viewModel.repository.saveCustomer(updated)
                                        Toast.makeText(context, "تم حفظ إعدادات ولاء العميل بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        selectedCustomerForEdit = null
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f).testTag("save_loyalty_settings_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ التغييرات", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditLoyaltyCampaignDialog(
    initialGoal: Int,
    initialReward: Double,
    initialSpendPerPoint: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (goal: Int, rewardValue: Double, spendPerPoint: Double, applyToAll: Boolean) -> Unit
) {
    var goalInput by remember { mutableStateOf(initialGoal.toString()) }
    var rewardInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialReward)) }
    var spendInput by remember { mutableStateOf(initialSpendPerPoint.toInt().toString()) }
    var applyToAllCustomers by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "تعديل شروط ومبالغ الولاء",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Field 1: Points Goal Threshold
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "عدد النقاط المطلوب للتسييل (الهدف):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            Text("نقطة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_goal_input")
                    )
                    // Preset chips for goal
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(250, 500, 1000, 2000).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (goalInput == preset.toString()) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (goalInput == preset.toString()) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.clickable { goalInput = preset.toString() }
                            ) {
                                Text(
                                    text = "$preset",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (goalInput == preset.toString()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Field 2: Reward Discount Money Value
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "قيمة كوبون الخصم المالي عند التسييل:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = rewardInput,
                        onValueChange = { rewardInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        trailingIcon = {
                            Text(currencySymbol, fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_reward_input")
                    )
                    // Preset chips for reward value
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(50.0, 100.0, 200.0, 500.0).forEach { preset ->
                            val presetStr = String.format(Locale.US, "%.2f", preset)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (rewardInput == presetStr || rewardInput == preset.toInt().toString()) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (rewardInput == presetStr || rewardInput == preset.toInt().toString()) Color(0xFF10B981) else Color.Transparent),
                                modifier = Modifier.clickable { rewardInput = presetStr }
                            ) {
                                Text(
                                    text = "${preset.toInt()} $currencySymbol",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rewardInput == presetStr || rewardInput == preset.toInt().toString()) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Field 3: Earning Rate (Spend per 1 point)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "معدل الكسب (مبلغ المبيعات لكل نقطة واحدة):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = spendInput,
                        onValueChange = { spendInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            Text("$currencySymbol / نقطة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_loyalty_spend_input")
                    )
                }

                // Info card for applying to all customers
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = applyToAllCustomers,
                            onCheckedChange = { applyToAllCustomers = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                        )
                        Column {
                            Text(
                                text = "تطبيق فوري على جميع العملاء الحاليين",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "تحديث أهداف ونسب شريط التقدم وقيمة المكافأة لحسابات العملاء بالقاعدة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val gl = goalInput.toIntOrNull()?.coerceAtLeast(1) ?: 500
                            val rwd = rewardInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 50.0
                            val spnd = spendInput.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                            onSave(gl, rwd, spnd, applyToAllCustomers)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1.4f).height(44.dp).testTag("confirm_save_loyalty_campaign_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتطبيق", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
