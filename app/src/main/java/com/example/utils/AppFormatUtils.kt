package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * محول ومنسق الأرقام والتواريخ اللاتينية الموحد للتطبيق (Latin Standard Numerals Engine)
 * يضمن تحويل كافة الأرقام من الشكل العربي المشرقي (٠١٢٣٤٥٦٧٨٩) إلى الشكل القياسي اللاتيني/الإنجليزي (0123456789)
 * في كافة واجهات التطبيق، والأسعار، والمخزون، والتواريخ، وشاشات المزامنة.
 */
object AppFormatUtils {

    /**
     * تحويل أي نص يحتوي على أرقام عربية مشرقية (٠-٩) أو فارسية (۰-۹) إلى أرقام لاتينية قياسية (0-9)
     */
    fun toLatinDigits(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val builder = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '٠', '۰' -> builder.append('0')
                '١', '۱' -> builder.append('1')
                '٢', '۲' -> builder.append('2')
                '٣', '۳' -> builder.append('3')
                '٤', '۴' -> builder.append('4')
                '٥', '۵' -> builder.append('5')
                '٦', '۶' -> builder.append('6')
                '٧', '۷' -> builder.append('7')
                '٨', '۸' -> builder.append('8')
                '٩', '۹' -> builder.append('9')
                '٫' -> builder.append('.')
                '٬' -> builder.append(',')
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * تنسيق وقت وتاريخ المزامنة المعتمد (الوقت أولاً ثم التاريخ: 14:30:00 23-10-2026)
     */
    fun formatSyncDateTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "بانتظار الفحص الأولي"
        val sdf = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.ENGLISH)
        return toLatinDigits(sdf.format(Date(timestampMillis)))
    }

    /**
     * تنسيق التاريخ العام (الوقت ثم التاريخ أو النمط المخصص بأرقام لاتينية)
     */
    fun formatDateTime(timestampMillis: Long, pattern: String = "HH:mm:ss dd-MM-yyyy"): String {
        if (timestampMillis <= 0L) return ""
        val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
        return toLatinDigits(sdf.format(Date(timestampMillis)))
    }

    /**
     * تنسيق الأعداد الصحيحة بأرقام لاتينية
     */
    fun formatInt(value: Int): String {
        return toLatinDigits(value.toString())
    }

    /**
     * تنسيق الأعداد الطويلة بأرقام لاتينية
     */
    fun formatLong(value: Long): String {
        return toLatinDigits(value.toString())
    }

    /**
     * تنسيق الكميات (إذا كان عدداً صحيحاً لا يعرض الفاصلة)
     */
    fun formatQuantity(quantity: Double): String {
        val str = if (quantity % 1.0 == 0.0) {
            String.format(Locale.ENGLISH, "%d", quantity.toLong())
        } else {
            String.format(Locale.ENGLISH, "%.2f", quantity)
        }
        return toLatinDigits(str)
    }

    /**
     * تنسيق الأسعار مع العملة (د.ج) بأرقام لاتينية
     */
    fun formatPrice(amount: Double, currency: String = "د.ج"): String {
        val formatted = String.format(Locale.ENGLISH, "%.2f", amount)
        return "${toLatinDigits(formatted)} $currency"
    }

    /**
     * تنسيق المبلغ العشري بدون العملة
     */
    fun formatAmountOnly(amount: Double): String {
        return toLatinDigits(String.format(Locale.ENGLISH, "%.2f", amount))
    }
}

/**
 * دالة امتداد سريعة لتحويل أي نص للأرقام اللاتينية
 */
fun String.toLatinDigits(): String = AppFormatUtils.toLatinDigits(this)
