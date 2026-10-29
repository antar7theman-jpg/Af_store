package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Expense
import com.example.data.models.ExpenseCategory
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: AntarSalesViewModel,
    onNavigateBack: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeframe by remember { mutableStateOf("THIS_MONTH") } // ALL, TODAY, LAST_7_DAYS, THIS_MONTH
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Time calculations
    val now = remember { System.currentTimeMillis() }
    val todayStart = remember(now) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val weekAgo = remember(now) { now - (7 * 86400000L) }
    val monthStart = remember(now) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // Filtered expenses
    val filteredExpenses = remember(expenses, searchQuery, selectedTimeframe, selectedCategoryFilter) {
        expenses.filter { exp ->
            val matchesTime = when (selectedTimeframe) {
                "TODAY" -> exp.dateMillis >= todayStart
                "LAST_7_DAYS" -> exp.dateMillis >= weekAgo
                "THIS_MONTH" -> exp.dateMillis >= monthStart
                else -> true
            }
            val matchesCategory = selectedCategoryFilter == null || exp.category == selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                exp.title.contains(searchQuery, ignoreCase = true) ||
                exp.notes.contains(searchQuery, ignoreCase = true) ||
                exp.paidBy.contains(searchQuery, ignoreCase = true) ||
                exp.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                exp.category.labelAr.contains(searchQuery, ignoreCase = true)

            matchesTime && matchesCategory && matchesSearch
        }
    }

    // High level metrics
    val totalThisMonth = remember(expenses, monthStart) {
        expenses.filter { it.dateMillis >= monthStart }.sumOf { it.amount }
    }
    val totalToday = remember(expenses, todayStart) {
        expenses.filter { it.dateMillis >= todayStart }.sumOf { it.amount }
    }
    val totalFiltered = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    // Category breakdown
    val categoryTotals = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val topCategory = categoryTotals.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المصاريف والنفقات التشغيلية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تسجيل ومتابعة سندات الصرف ومصاريف المحل",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareSummary = buildExpensesShareSummary(
                                storeName = viewModel.storeName.value,
                                currency = currencySymbol,
                                expenses = filteredExpenses,
                                timeframe = selectedTimeframe,
                                total = totalFiltered
                            )
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير المصاريف"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة التقرير"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = semantic.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("تسجيل مصروف جديد", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_expense_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(semantic.background),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metrics Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseKpiCard(
                        title = "مصاريف الشهر",
                        value = "${String.format(Locale.US, "%.0f", totalThisMonth)} $currencySymbol",
                        subtitle = "خلال الشهر الحالي",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    ExpenseKpiCard(
                        title = "مصاريف اليوم",
                        value = "${String.format(Locale.US, "%.0f", totalToday)} $currencySymbol",
                        subtitle = "سندات اليوم",
                        icon = Icons.Default.Today,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    ExpenseKpiCard(
                        title = "أعلى بند إنفاق",
                        value = topCategory?.first?.labelAr?.take(10) ?: "لا يوجد",
                        subtitle = topCategory?.let { "${String.format(Locale.US, "%.0f", it.second)} $currencySymbol" } ?: "0 $currencySymbol",
                        icon = Icons.Default.TrendingUp,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Breakdown Progress Card
            if (categoryTotals.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.surface),
                        border = BorderStroke(1.dp, semantic.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                                        Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "توزيع النفقات حسب البنود:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "الإجمالي: ${String.format(Locale.US, "%.2f", totalFiltered)} $currencySymbol",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            categoryTotals.take(4).forEach { (cat, sum) ->
                                val pct = if (totalFiltered > 0.0) (sum / totalFiltered).toFloat() else 0f
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.labelAr,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", sum)} $currencySymbol (${(pct * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            color = semantic.textSecondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = getCategoryColor(cat),
                                        trackColor = semantic.cardSubtle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search and Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expenses_search_input"),
                        placeholder = { Text("بحث باسم المصروف، السند، الدافع، أو الملاحظات...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = semantic.textSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = semantic.surface,
                            unfocusedContainerColor = semantic.surface
                        )
                    )

                    // Time Range Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "THIS_MONTH" to "هذا الشهر",
                            "TODAY" to "اليوم",
                            "LAST_7_DAYS" to "آخر 7 أيام",
                            "ALL" to "كل الأوقات"
                        ).forEach { (key, label) ->
                            val isSelected = selectedTimeframe == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeframe = key },
                                label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    // Category Filter Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("جميع البنود (${filteredExpenses.size})", fontSize = 11.sp) }
                        )
                        ExpenseCategory.entries.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (isSelected) null else cat
                                },
                                label = { Text(cat.labelAr, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        getCategoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = getCategoryColor(cat)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Expense Items List
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = semantic.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "لم يتم العثور على مصاريف تطابق بحثك" else "لا توجد مصاريف مسجلة في هذا النطاق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary
                            )
                            Text(
                                text = "اضغط على زر (تسجيل مصروف جديد) لإضافة فواتير، إيجار، أو نفقات المتجر",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        currencySymbol = currencySymbol,
                        onEdit = {
                            expenseToEdit = expense
                            showAddEditDialog = true
                        },
                        onDelete = {
                            expenseToDelete = expense
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    if (showAddEditDialog) {
        AddEditExpenseDialog(
            initialExpense = expenseToEdit,
            currencySymbol = currencySymbol,
            defaultPayerName = currentUser?.fullName?.ifBlank { currentUser?.username } ?: "Antar",
            onDismiss = {
                showAddEditDialog = false
                expenseToEdit = null
            },
            onSave = { savedExpense ->
                viewModel.saveExpense(savedExpense)
                showAddEditDialog = false
                expenseToEdit = null
                Toast.makeText(context, "✓ تم حفظ سند الصرف بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("حذف سند الصرف", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("هل أنت متأكد من حذف مصروف \"${exp.title}\" بقيمة ${String.format(Locale.US, "%.2f", exp.amount)} $currencySymbol؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(exp.id)
                        expenseToDelete = null
                        Toast.makeText(context, "تم حذف المصروف", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ExpenseKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = semantic.textSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = semantic.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = semantic.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val catColor = getCategoryColor(expense.category)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = catColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, catColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = expense.category.labelAr,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                // Sub details: Date, Payer, Receipt
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${expense.formattedDate} • ${expense.formattedTime}",
                        fontSize = 11.sp,
                        color = semantic.textSecondary
                    )
                    if (expense.paidBy.isNotBlank()) {
                        Text(
                            text = "بواسطة: ${expense.paidBy}",
                            fontSize = 11.sp,
                            color = semantic.textSecondary
                        )
                    }
                }

                if (expense.receiptNumber.isNotBlank() || expense.notes.isNotBlank()) {
                    val extra = listOfNotNull(
                        if (expense.receiptNumber.isNotBlank()) "وصل #${expense.receiptNumber}" else null,
                        expense.notes.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    Text(
                        text = extra,
                        fontSize = 10.5.sp,
                        color = semantic.textSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount and Action Buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", expense.amount)} $currencySymbol",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEF4444)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = semantic.cardSubtle.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = expense.paymentMethodAr,
                        fontSize = 9.sp,
                        color = semantic.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditExpenseDialog(
    initialExpense: Expense?,
    currencySymbol: String,
    defaultPayerName: String,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var selectedCategory by remember { mutableStateOf(initialExpense?.category ?: ExpenseCategory.OTHER) }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: "CASH") }
    var paidBy by remember { mutableStateOf(initialExpense?.paidBy ?: defaultPayerName) }
    var receiptNumber by remember { mutableStateOf(initialExpense?.receiptNumber ?: "") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickSuggestedTitles = listOf(
        "فاتورة كهرباء" to ExpenseCategory.UTILITIES,
        "فاتورة إنترنت وهاتف" to ExpenseCategory.UTILITIES,
        "فاتورة مياه" to ExpenseCategory.UTILITIES,
        "إيجار المحل" to ExpenseCategory.RENT,
        "أكياس ومواد تغليف" to ExpenseCategory.SUPPLIES,
        "وقود ونقل طلبيات" to ExpenseCategory.TRANSPORT,
        "رواتب ومستحقات عمال" to ExpenseCategory.SALARIES,
        "صيانة أجهزة ومعدات" to ExpenseCategory.MAINTENANCE,
        "نثريات وضيافة" to ExpenseCategory.OTHER
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (initialExpense == null) "تسجيل سند صرف / مصروف جديد" else "تعديل سند الصرف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = semantic.divider)

                // Quick Suggestion Chips
                Text(
                    text = "اقتراحات شائعة سريعة:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSuggestedTitles.forEach { (suggestedTitle, suggestedCat) ->
                        Surface(
                            onClick = {
                                title = suggestedTitle
                                selectedCategory = suggestedCat
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = semantic.cardSubtle.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, semantic.cardBorder)
                        ) {
                            Text(
                                text = suggestedTitle,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = semantic.textPrimary
                            )
                        }
                    }
                }

                // Expense Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("بيان المصروف (عنوان النفقة) *") },
                    placeholder = { Text("مثال: فاتورة الكهرباء لشهر أكتوبر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("المبلغ المالي ($currencySymbol) *") },
                    placeholder = { Text("مثال: 3500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection
                Text("تصنيف وبند المصروف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) getCategoryColor(cat).copy(alpha = 0.18f) else semantic.cardSubtle.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) getCategoryColor(cat) else semantic.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    tint = if (isSelected) getCategoryColor(cat) else semantic.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = cat.labelAr,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) getCategoryColor(cat) else semantic.textPrimary
                                )
                            }
                        }
                    }
                }

                // Payment Method
                Text("طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CASH" to "نقداً (كاش)",
                        "BANK_TRANSFER" to "تحويل بنكي",
                        "CHECK" to "شيك / صك"
                    ).forEach { (methodKey, methodLabel) ->
                        val isSelected = paymentMethod == methodKey
                        Surface(
                            onClick = { paymentMethod = methodKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else semantic.cardSubtle.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = methodLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else semantic.textPrimary
                                )
                            }
                        }
                    }
                }

                // Payer and Receipt Reference in 2 columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = { paidBy = it },
                        label = { Text("المسدد / الصارف") },
                        placeholder = { Text("اسم الشخص") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = receiptNumber,
                        onValueChange = { receiptNumber = it },
                        label = { Text("رقم الوصل/السند") },
                        placeholder = { Text("اختياري") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    placeholder = { Text("أي تفاصيل أخرى تخص هذا المصروف...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val cleanAmount = amountText.toDoubleOrNull()
                            if (cleanTitle.isEmpty()) {
                                errorMessage = "يرجى كتابة بيان المصروف"
                                return@Button
                            }
                            if (cleanAmount == null || cleanAmount <= 0.0) {
                                errorMessage = "يرجى إدخال مبلغ مالي صحيح أكبر من الصفر"
                                return@Button
                            }
                            val result = initialExpense?.copy(
                                title = cleanTitle,
                                amount = cleanAmount,
                                category = selectedCategory,
                                paymentMethod = paymentMethod,
                                paidBy = paidBy.trim(),
                                receiptNumber = receiptNumber.trim(),
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            ) ?: Expense(
                                title = cleanTitle,
                                amount = cleanAmount,
                                category = selectedCategory,
                                dateMillis = System.currentTimeMillis(),
                                paymentMethod = paymentMethod,
                                paidBy = paidBy.trim(),
                                receiptNumber = receiptNumber.trim(),
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(result)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ السند", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: ExpenseCategory): ImageVector {
    return when (category) {
        ExpenseCategory.RENT -> Icons.Default.Store
        ExpenseCategory.UTILITIES -> Icons.Default.Bolt
        ExpenseCategory.SALARIES -> Icons.Default.Badge
        ExpenseCategory.TRANSPORT -> Icons.Default.LocalShipping
        ExpenseCategory.SUPPLIES -> Icons.Default.Inventory2
        ExpenseCategory.MAINTENANCE -> Icons.Default.Build
        ExpenseCategory.TAXES_FEES -> Icons.Default.AccountBalance
        ExpenseCategory.MARKETING -> Icons.Default.Campaign
        ExpenseCategory.OTHER -> Icons.Default.Receipt
    }
}

private fun getCategoryColor(category: ExpenseCategory): Color {
    return when (category) {
        ExpenseCategory.RENT -> Color(0xFF6366F1)
        ExpenseCategory.UTILITIES -> Color(0xFFF59E0B)
        ExpenseCategory.SALARIES -> Color(0xFF10B981)
        ExpenseCategory.TRANSPORT -> Color(0xFF06B6D4)
        ExpenseCategory.SUPPLIES -> Color(0xFF8B5CF6)
        ExpenseCategory.MAINTENANCE -> Color(0xFFEC4899)
        ExpenseCategory.TAXES_FEES -> Color(0xFF64748B)
        ExpenseCategory.MARKETING -> Color(0xFFF97316)
        ExpenseCategory.OTHER -> Color(0xFF6B7280)
    }
}

private fun buildExpensesShareSummary(
    storeName: String,
    currency: String,
    expenses: List<Expense>,
    timeframe: String,
    total: Double
): String {
    val timeframeLabel = when (timeframe) {
        "TODAY" -> "سندات اليوم"
        "LAST_7_DAYS" -> "آخر 7 أيام"
        "THIS_MONTH" -> "الشهر الحالي"
        else -> "جميع الفترات"
    }
    val sb = StringBuilder()
    sb.appendLine("📊 تقرير المصاريف والنفقات التشغيلية")
    sb.appendLine("🏢 المتجر: $storeName")
    sb.appendLine("📅 النطاق: $timeframeLabel")
    sb.appendLine("💰 إجمالي المصاريف: ${String.format(Locale.US, "%.2f", total)} $currency")
    sb.appendLine("📄 عدد السندات: ${expenses.size}")
    sb.appendLine("─────────────────────────")
    expenses.take(20).forEachIndexed { index, exp ->
        sb.appendLine("${index + 1}. ${exp.title}")
        sb.appendLine("   المبلغ: ${String.format(Locale.US, "%.2f", exp.amount)} $currency (${exp.category.labelAr})")
        sb.appendLine("   التاريخ: ${exp.formattedDate} • ${exp.paymentMethodAr}")
        if (exp.receiptNumber.isNotBlank()) sb.appendLine("   رقم الوصل: ${exp.receiptNumber}")
    }
    if (expenses.size > 20) {
        sb.appendLine("... و ${expenses.size - 20} مصروفات أخرى مسجلة.")
    }
    sb.appendLine("─────────────────────────")
    sb.appendLine("تم التصدير من تطبيق AF store")
    return sb.toString()
}
