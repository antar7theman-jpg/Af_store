package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.models.ActivityLog
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.Sale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object ProductImageStorageHelper {

    private fun getProductImagesDir(context: Context): File {
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveImageFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            saveBitmapInternal(context, decoded)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveBitmapToStorage(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        saveBitmapInternal(context, bitmap)
    }

    private fun saveBitmapInternal(context: Context, source: Bitmap): String? {
        return try {
            val maxDim = 1400
            val width = source.width.coerceAtLeast(1)
            val height = source.height.coerceAtLeast(1)
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = maxDim.toFloat() / maxOf(width, height).toFloat()
                val targetW = (width * ratio).toInt().coerceAtLeast(1)
                val targetH = (height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(source, targetW, targetH, true)
            } else {
                source
            }

            val destFile = File(
                getProductImagesDir(context),
                "prod_img_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun resolveImageModel(imagePath: String): Any? {
        val trimmed = imagePath.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("data:image/") || (trimmed.contains(";base64,") && trimmed.length > 50)) {
            val raw = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
            return try {
                android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
            } catch (_: Exception) {
                null
            }
        }
        if (trimmed.startsWith("content://") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return Uri.parse(trimmed)
        }
        val file = File(trimmed)
        return if (file.exists()) file else null
    }
}

@Composable
fun ProductImagePickerSection(
    imagePath: String,
    productNameForPreview: String = "السلعة",
    onImagePathChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCameraDialog by remember { mutableStateOf(false) }
    var showFullScreenPreview by remember { mutableStateOf(false) }
    var isSavingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingImage = true
            scope.launch {
                val savedPath = ProductImageStorageHelper.saveImageFromUri(context, uri)
                isSavingImage = false
                if (!savedPath.isNullOrBlank()) {
                    onImagePathChanged(savedPath)
                }
            }
        }
    }

    val resolvedModel = remember(imagePath) {
        ProductImageStorageHelper.resolveImageModel(imagePath)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "صورة السلعة (تظهر كبيرة عند الضغط على الاسم)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isSavingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Camera & Gallery Selection Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showCameraDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("product_image_camera_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "التقاط بالكاميرا",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "التقاط بالكاميرا",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("product_image_gallery_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "اختيار من المعرض",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "من معرض الصور",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Attached Image Preview & Controls
            if (resolvedModel != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showFullScreenPreview = true }
                        ) {
                            AsyncImage(
                                model = resolvedModel,
                                contentDescription = "معاينة صورة السلعة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تم إرفاق صورة السلعة بنجاح ✅",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "اضغط هنا لتكبير ومعاينة الصورة بالحجم الكامل",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onImagePathChanged("") },
                            modifier = Modifier.testTag("product_image_remove_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف الصورة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "لم يتم إرفاق صورة بعد • عند إضافة صورة ستظهر بحجم كبير عند الضغط على اسم السلعة.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCameraDialog) {
        ProductCameraPhotoCaptureDialog(
            onPhotoSaved = { savedPath ->
                showCameraDialog = false
                onImagePathChanged(savedPath)
            },
            onDismiss = { showCameraDialog = false }
        )
    }

    if (showFullScreenPreview && resolvedModel != null) {
        ProductFullScreenImageDialog(
            product = Product(
                name = productNameForPreview.ifBlank { "معاينة صورة السلعة" },
                imagePath = imagePath
            ),
            currencySymbol = "",
            onUpdateProductImage = { updatedPath ->
                onImagePathChanged(updatedPath)
            },
            onDismiss = { showFullScreenPreview = false }
        )
    }
}

@Composable
fun ProductCameraPhotoCaptureDialog(
    onPhotoSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val systemCameraFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                val path = ProductImageStorageHelper.saveBitmapToStorage(context, bitmap)
                if (!path.isNullOrBlank()) {
                    onPhotoSaved(path)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Exception) {
            }
            cameraExecutor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "التقاط صورة للسلعة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = {
                                    val next = !isTorchOn
                                    isTorchOn = next
                                    cameraRef?.cameraControl?.enableTorch(next)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "الفلاش",
                                    tint = if (isTorchOn) Color(0xFFFACC15) else Color.White
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (!hasCameraPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "يلزم منح صلاحية الكاميرا لالتقاط صورة السلعة مباشرة.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 13.5.sp
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("منح صلاحية الكاميرا", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { systemCameraFallbackLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فتح تطبيق الكاميرا الخارجي", color = Color.White)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        cameraProviderRef = cameraProvider
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val capture = ImageCapture.Builder()
                                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                            .build()
                                        imageCaptureRef = capture
                                        cameraProvider.unbindAll()
                                        cameraRef = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            capture
                                        )
                                    } catch (_: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val capture = imageCaptureRef
                                if (capture == null) {
                                    systemCameraFallbackLauncher.launch(null)
                                    return@Button
                                }
                                isCapturing = true
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            scope.launch {
                                                val saved = bitmap?.let {
                                                    ProductImageStorageHelper.saveBitmapToStorage(context, it)
                                                }
                                                isCapturing = false
                                                if (!saved.isNullOrBlank()) {
                                                    onPhotoSaved(saved)
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isCapturing = false
                                            scope.launch(Dispatchers.Main) {
                                                systemCameraFallbackLauncher.launch(null)
                                            }
                                        }
                                    }
                                )
                            },
                            enabled = !isCapturing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("capture_product_photo_confirm_btn")
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "التقاط وحفظ الصورة",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("إلغاء", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Full-screen zoomable product image viewer opened when clicking on the Product Name.
 * Supports pinch-to-zoom, pan, double-tap zoom, and quick photo attachment/updating.
 */
@Composable
fun ProductFullScreenImageDialog(
    product: Product,
    currencySymbol: String,
    onUpdateProductImage: ((String) -> Unit)? = null,
    onOpenMovementHistory: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentImagePath by remember(product.id, product.imagePath) {
        mutableStateOf(product.imagePath)
    }
    var scale by remember(currentImagePath) { mutableFloatStateOf(1f) }
    var offset by remember(currentImagePath) { mutableStateOf(Offset.Zero) }
    var showCameraCapture by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = ProductImageStorageHelper.saveImageFromUri(context, uri)
                if (!savedPath.isNullOrBlank()) {
                    currentImagePath = savedPath
                    onUpdateProductImage?.invoke(savedPath)
                }
            }
        }
    }

    val resolvedModel = remember(currentImagePath) {
        ProductImageStorageHelper.resolveImageModel(currentImagePath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF50B1120),
            modifier = Modifier
                .fillMaxSize()
                .testTag("product_fullscreen_image_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar with Product Name, Price, Stock, and Close Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currencySymbol.isNotBlank()) {
                                Text(
                                    text = "السعر: ${String.format(Locale.US, "%.2f", product.price)} $currencySymbol • المتوفر بالمخزن: ${product.stock} ق",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenMovementHistory != null) {
                                Surface(
                                    onClick = onOpenMovementHistory,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("fullscreen_image_open_history_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "سجل الحركة",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "سجل الحركة",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("close_fullscreen_image_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Large Zoomable Product Image Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (resolvedModel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(resolvedModel) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.05f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else {
                                                scale = 2.5f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(resolvedModel) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        if (newScale > 1f) {
                                            val maxOffset = 450f * (newScale - 1f)
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                                y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = resolvedModel,
                                contentDescription = "صورة السلعة ${product.name}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .testTag("fullscreen_zoomable_product_image")
                            )
                        }

                        // Floating Zoom Control Pills at Bottom of Image
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = (scale - 0.5f).coerceAtLeast(1f)
                                        scale = next
                                        if (next <= 1f) offset = Offset.Zero
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "تصغير",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Text(
                                    text = "${(scale * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )

                                IconButton(
                                    onClick = {
                                        scale = (scale + 0.5f).coerceAtMost(4f)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "تكبير",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                if (scale > 1.05f) {
                                    IconButton(
                                        onClick = {
                                            scale = 1f
                                            offset = Offset.Zero
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = "إعادة الضبط",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State when no image is set yet for this product
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Text(
                                text = "لا توجد صورة مرفقة لـ «${product.name}» حالياً",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "يمكنك التقاط صورة للسلعة بالكاميرا أو اختيارها من معرض الصور الآن لتظهر بالحجم الكبير عند الضغط على اسمها.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )

                            if (onUpdateProductImage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showCameraCapture = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0284C7),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("empty_fullscreen_take_photo_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "التقاط بالكاميرا",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("empty_fullscreen_pick_gallery_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "من المعرض",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Elevated & Symmetrical Bottom Action Bar
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onUpdateProductImage != null) {
                            Button(
                                onClick = { showCameraCapture = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الكاميرا",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المعرض",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إغلاق",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraCapture) {
        ProductCameraPhotoCaptureDialog(
            onPhotoSaved = { savedPath ->
                showCameraCapture = false
                currentImagePath = savedPath
                onUpdateProductImage?.invoke(savedPath)
            },
            onDismiss = { showCameraCapture = false }
        )
    }
}

data class ProductMovementEntry(
    val id: String,
    val type: String, // "SALE" or "ADDITION"
    val title: String,
    val subtitle: String,
    val quantityDelta: Int,
    val totalAmount: Double,
    val timestamp: Long
)

@Composable
fun ProductMovementHistoryDialog(
    product: Product,
    sales: List<Sale>,
    purchaseInvoices: List<PurchaseInvoice>,
    activityLogs: List<ActivityLog>,
    currencySymbol: String,
    onOpenProductImage: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SALES, ADDITIONS

    val allMovements = remember(product, sales, purchaseInvoices, activityLogs) {
        val entries = mutableListOf<ProductMovementEntry>()

        // 1. Sales for this product
        sales.filter { sale ->
            sale.productId == product.id ||
                sale.productName.equals(product.name, ignoreCase = true) ||
                sale.productName.startsWith("${product.name} (")
        }.forEach { sale ->
            val paymentLabel = when (sale.paymentMethod) {
                "DEBT" -> "آجل (دين)"
                "TRANSFER" -> "تحويل"
                else -> "نقدي"
            }
            entries.add(
                ProductMovementEntry(
                    id = "sale_${sale.saleId}_${sale.saleDate}",
                    type = "SALE",
                    title = "عملية بيع • ${sale.customerName.ifBlank { "عميل نقدي" }}",
                    subtitle = "طريقة الدفع: $paymentLabel",
                    quantityDelta = -sale.quantitySold.coerceAtLeast(1),
                    totalAmount = sale.totalPrice,
                    timestamp = sale.saleDate
                )
            )
        }

        // 2. Additions from Purchase Invoices
        purchaseInvoices.forEach { invoice ->
            invoice.getItems().filter { item ->
                (item.productId != null && item.productId == product.id) ||
                    item.productName.equals(product.name, ignoreCase = true) ||
                    (product.barcode.isNotBlank() && item.barcode.equals(product.barcode, ignoreCase = true))
            }.forEachIndexed { idx, item ->
                val addedUnits = if (item.isCarton) {
                    item.quantity * item.unitsPerCarton.coerceAtLeast(1)
                } else {
                    item.quantity
                }
                entries.add(
                    ProductMovementEntry(
                        id = "pur_${invoice.id}_${idx}_${invoice.dateMillis}",
                        type = "ADDITION",
                        title = "إضافة فاتورة شراء (${invoice.invoiceNumber})",
                        subtitle = "المورد: ${invoice.supplierName.ifBlank { "مورد عام" }}",
                        quantityDelta = addedUnits.coerceAtLeast(1),
                        totalAmount = item.subtotal,
                        timestamp = invoice.dateMillis
                    )
                )
            }
        }

        // 3. Quick Restocks & Initial Barcode Creations from ActivityLogs
        activityLogs.filter { log ->
            (log.actionType == "PRODUCT_RESTOCK" || log.actionType == "PRODUCT_CREATED_BARCODE") &&
                (log.metadata.contains("productId=${product.id}") || log.description.contains(product.name))
        }.forEach { log ->
            val parsedAdded = log.metadata
                .split(";")
                .firstOrNull { it.startsWith("addedUnits=") }
                ?.substringAfter("addedUnits=")
                ?.toIntOrNull() ?: 0
            entries.add(
                ProductMovementEntry(
                    id = "log_${log.id}_${log.timestamp}",
                    type = "ADDITION",
                    title = if (log.actionType == "PRODUCT_RESTOCK") "تزويد مخزون سريع" else "تسجيل رصيد أولي للصنف",
                    subtitle = log.description,
                    quantityDelta = parsedAdded,
                    totalAmount = 0.0,
                    timestamp = log.timestamp
                )
            )
        }

        entries.sortedByDescending { it.timestamp }
    }

    val filteredMovements = remember(allMovements, selectedFilter) {
        when (selectedFilter) {
            "SALES" -> allMovements.filter { it.type == "SALE" }
            "ADDITIONS" -> allMovements.filter { it.type == "ADDITION" }
            else -> allMovements
        }
    }

    val totalSoldUnits = remember(allMovements) {
        allMovements.filter { it.type == "SALE" }.sumOf { kotlin.math.abs(it.quantityDelta) }
    }
    val totalAddedUnits = remember(allMovements) {
        allMovements.filter { it.type == "ADDITION" }.sumOf { it.quantityDelta.coerceAtLeast(0) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("product_movement_history_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تفاصيل وسجل حركة السلعة",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp
                            )
                        }
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onOpenProductImage() }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onOpenProductImage,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صورة السلعة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                // Summary Cards (Current Stock | Total Additions | Total Sales)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("الرصيد الحالي", fontSize = 11.sp)
                            Text(
                                "${product.stock} وحدة",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.14f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي الإضافات", fontSize = 11.sp, color = Color(0xFF10B981))
                            Text(
                                "+$totalAddedUnits ق",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.14f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي المبيعات", fontSize = 11.sp, color = Color(0xFFEF4444))
                            Text(
                                "-$totalSoldUnits ق",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل (${allMovements.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "SALES",
                        onClick = { selectedFilter = "SALES" },
                        label = {
                            Text(
                                "المبيعات (${allMovements.count { it.type == "SALE" }})",
                                fontSize = 12.sp
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == "ADDITIONS",
                        onClick = { selectedFilter = "ADDITIONS" },
                        label = {
                            Text(
                                "الإضافات (${allMovements.count { it.type == "ADDITION" }})",
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                HorizontalDivider()

                // Movement Timeline List
                if (filteredMovements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة لهذا الصنف ضمن هذا التصنيف حتى الآن.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = filteredMovements,
                            key = { index, movement -> "${movement.id}_$index" },
                            contentType = { _, movement -> movement.type }
                        ) { _, movement ->
                            val isAddition = movement.type == "ADDITION"
                            val accentColor = if (isAddition) Color(0xFF10B981) else Color(0xFFEF4444)
                            val formattedDate = remember(movement.timestamp) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
                                    .format(Date(movement.timestamp))
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor.copy(alpha = 0.16f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isAddition) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = movement.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = movement.subtitle,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "التاريخ: $formattedDate",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (movement.quantityDelta != 0) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = accentColor.copy(alpha = 0.16f)
                                            ) {
                                                val sign = if (movement.quantityDelta > 0) "+" else ""
                                                Text(
                                                    text = "$sign${movement.quantityDelta} ق",
                                                    color = accentColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (movement.totalAmount > 0.0) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", movement.totalAmount)} $currencySymbol",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
