package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.models.Product
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * مساعد معالجة وضغط وتشفير صور السلع السحابية والمحلية.
 * يتيح ضغط الصور بدقة عالية وحجم خفيف (~15-25KB) بصيغة Base64
 * ليتم مزامنتها لحظياً وفورياً بين هاتف المدير وهواتف العمال التابعين له.
 */
object ProductCloudImageHelper {
    private const val TAG = "ProductCloudImageHelper"
    private const val MAX_DIMENSION = 384
    private const val JPEG_QUALITY = 78
    private const val DATA_PREFIX = "data:image/jpeg;base64,"

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * ضغط صورة المنتج المحلية وتحويلها إلى كود Base64 خفيف للمزامنة السحابية.
     */
    fun compressLocalImageToBase64(context: Context, pathOrUri: String): String? {
        val trimmed = pathOrUri.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith(DATA_PREFIX)) return trimmed

        return try {
            val bitmap = loadScaledBitmap(context, trimmed) ?: return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle()

            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "$DATA_PREFIX$encoded"
        } catch (e: Exception) {
            Log.w(TAG, "فشل ضغط وتحويل الصورة إلى Base64: ${e.message}")
            null
        }
    }

    /**
     * فك تشفير صورة Base64 المستلمة سحابياً وحفظها في الكاش المحلي لهاتف العامل.
     */
    fun decodeBase64ToLocalFile(context: Context, base64String: String, identifier: String): String? {
        val trimmed = base64String.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("data:image/") && !trimmed.contains("base64,")) {
            // مسار ملف عادي أو رابط ويب
            return if (File(trimmed).exists()) trimmed else null
        }

        return try {
            val rawBase64 = if (trimmed.contains(",")) {
                trimmed.substringAfter(",")
            } else {
                trimmed
            }
            val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
            if (decodedBytes.isEmpty()) return null

            val safeId = identifier.replace(Regex("[^a-zA-Z0-9_]"), "_").take(40)
            val fileName = "synced_prod_${safeId}.jpg"
            val targetFile = File(getStorageDir(context), fileName)

            FileOutputStream(targetFile).use { fos ->
                fos.write(decodedBytes)
                fos.flush()
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "فشل فك تشفير وحفظ صورة السلعة محلياً: ${e.message}")
            null
        }
    }

    /**
     * التحقق من توفر مسار محلي صالح لصورة المنتج أو إنشاؤه من الحقل السحابي imageUrl.
     */
    fun ensureLocalProductImage(context: Context, product: Product): String {
        val currentLocalPath = product.imagePath.trim()
        if (currentLocalPath.isNotEmpty() && File(currentLocalPath).exists()) {
            return currentLocalPath
        }

        val cloudUrl = product.imageUrl.trim()
        if (cloudUrl.startsWith("data:image/") || cloudUrl.length > 100) {
            val idKey = if (product.barcode.isNotBlank()) product.barcode else "${product.id}_${product.name}"
            val cachedPath = decodeBase64ToLocalFile(context, cloudUrl, idKey)
            if (!cachedPath.isNullOrBlank()) {
                return cachedPath
            }
        }
        return currentLocalPath
    }

    private fun loadScaledBitmap(context: Context, pathOrUri: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(pathOrUri, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            var sampleSize = 1
            while (width / sampleSize > MAX_DIMENSION || height / sampleSize > MAX_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val originalBitmap = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
            } else {
                BitmapFactory.decodeFile(pathOrUri, decodeOptions)
            } ?: return null

            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            val scale = (MAX_DIMENSION.toFloat() / maxOf(originalWidth, originalHeight)).coerceAtMost(1f)

            if (scale < 1f) {
                val scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
                val scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            Log.w(TAG, "خطأ في تحميل وتصغير الصورة: ${e.message}")
            null
        }
    }
}
