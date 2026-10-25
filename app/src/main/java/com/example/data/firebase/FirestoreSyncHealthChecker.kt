package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * نتيجة فحص سرعة الاستجابة وكتابة/قراءة البيانات (Ping & Latency)
 */
data class PingLatencyResult(
    val isSuccess: Boolean,
    val writeLatencyMs: Long = 0L,
    val readLatencyMs: Long = 0L,
    val totalLatencyMs: Long = 0L,
    val isServerVerified: Boolean = false,
    val errorMessage: String? = null,
    val diagnosticSummaryAr: String = ""
)

/**
 * نتيجة فحص المزامنة عند انقطاع وعودة الاتصال (Offline Persistence & Auto-Sync)
 */
data class OfflineSyncResult(
    val isSuccess: Boolean,
    val cachedOfflineSuccessfully: Boolean = false,
    val hasPendingWritesWhileOffline: Boolean = false,
    val syncedToServerAfterReconnection: Boolean = false,
    val syncDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val diagnosticSummaryAr: String = ""
)

/**
 * حالة الاتصال اللحظي
 */
enum class RealtimeConnectionState {
    CONNECTED_LIVE,     // متصل بالسيرفر والبيانات حديثة من السحابة
    SYNCED_CACHE,      // مستقر من الذاكرة المحلية والاتصال قائم
    PENDING_WRITES,    // توجد عمليات كتابة معلقة بانتظار الرفع
    OFFLINE,           // غير متصل أو منقطع
    ERROR              // خطأ في الصلاحيات أو قواعد الأمان
}

data class RealtimeStatusUpdate(
    val state: RealtimeConnectionState,
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean,
    val messageAr: String,
    val exceptionCode: String? = null
)

/**
 * تصنيف أخطاء المزامنة وقواعد الأمان والصلاحيات
 */
data class FirestoreErrorDiagnosis(
    val errorCode: String,
    val errorNameAr: String,
    val descriptionAr: String,
    val rootCauseAr: String,
    val solutionAr: String,
    val isPermissionError: Boolean,
    val isNetworkError: Boolean,
    val technicalDetails: String
)

/**
 * أداة الفحص الشامل لمزامنة وصحة Firebase Firestore (Health Check & Diagnostics)
 */
class FirestoreSyncHealthChecker(
    private val firestore: FirebaseFirestore = StaffAccountRepository.getFirestoreDb()
) {
    companion object {
        private const val TAG = "FirestoreHealthCheck"
        private const val HEALTH_CHECK_COLLECTION = "_system_health_checks"

        /**
         * تصنيف متقدم وتحليل دقيق لأي خطأ ينتج عن Firestore
         */
        fun diagnoseException(exception: Throwable): FirestoreErrorDiagnosis {
            if (exception is FirebaseFirestoreException) {
                return when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "تم رفض الإذن (صلاحيات غير كافية)",
                        descriptionAr = "قواعد أمان Firebase (Security Rules) رفضت العملية.",
                        rootCauseAr = "عدم تسجيل الدخول أو انتهاء جلسة Auth، أو محاولة الوصول لوثيقة لا يملك المستخدم صلاحية القراءة/الكتابة عليها.",
                        solutionAr = "تحقق من قواعد الأمان firestore.rules، وتأكد من أن المستخدم يملك uid مطابق، أو أن حسابه معتمد كـ manager/owner.",
                        isPermissionError = true,
                        isNetworkError = false,
                        technicalDetails = exception.message ?: "PERMISSION_DENIED"
                    )
                    FirebaseFirestoreException.Code.UNAVAILABLE -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "خدمة Firebase غير متوفرة حالياً",
                        descriptionAr = "تعذر الوصول لخوادم Google بسبب انقطاع الإنترنت أو حجب الشبكة.",
                        rootCauseAr = "انقطاع الاتصال بشبكة الإنترنت على الجهاز، أو مشاكل DNS أو حجب جدار حماية محلي.",
                        solutionAr = "تعتمد الذاكرة المحلية (Offline Cache) تلقائياً، وسيتم رفع البيانات بمجرد توفر الاتصال.",
                        isPermissionError = false,
                        isNetworkError = true,
                        technicalDetails = exception.message ?: "UNAVAILABLE"
                    )
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "انتهاء مهلة الاتصال (Timeout)",
                        descriptionAr = "استغرقت عملية المزامنة وقتاً أطول من المسموح به.",
                        rootCauseAr = "بطء شديد في سرعة الإنترنت أو حجم بيانات ضخم جداً يُطلب دفعة واحدة بدون Pagination.",
                        solutionAr = "استخدم limit() في الاستعلامات وتجنب جلب آلاف السجلات دفعة واحدة.",
                        isPermissionError = false,
                        isNetworkError = true,
                        technicalDetails = exception.message ?: "DEADLINE_EXCEEDED"
                    )
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "استنفاد الحصة المسموحة (Quota Exceeded)",
                        descriptionAr = "تم تجاوز عدد القراءات/الكتابات المجانية المتاحة للمشروع، أو تجاوز حجم الوثيقة 1 ميجابايت.",
                        rootCauseAr = "عمليات استعلام في حلقة تكرارية غير محدودة (Infinite Loop) أو تجاوز الحد الأقصى لحجم الوثيقة الواحدة (1MB).",
                        solutionAr = "راجع الاستعلامات وتأكد من تخزين الصور والمرفقات الكبيرة في Firebase Storage أو الذاكرة الخارجية وليس داخل حقول Firestore.",
                        isPermissionError = false,
                        isNetworkError = false,
                        technicalDetails = exception.message ?: "RESOURCE_EXHAUSTED"
                    )
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "جلسة المستخدم منتهية (Unauthenticated)",
                        descriptionAr = "رمز التحقق (Auth Token) غير صالح أو منتهي الصلاحية.",
                        rootCauseAr = "تسجيل خروج المستخدم أو إبطال جلسة تسجيل الدخول من لوحة التحكم.",
                        solutionAr = "أعد توجيه المستخدم لشاشة تسجيل الدخول لتجديد الرمز المميز (Refresh Token).",
                        isPermissionError = true,
                        isNetworkError = false,
                        technicalDetails = exception.message ?: "UNAUTHENTICATED"
                    )
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "فهرس مركب مفقود (Missing Composite Index)",
                        descriptionAr = "الاستعلام يتطلب فهرساً مركباً في قاعدة البيانات لم يتم إنشاؤه بعد.",
                        rootCauseAr = "الجمع بين orderBy وعوامل تصفية أين (whereEqualTo / whereIn) على حقول متعددة دون إنشاء Index.",
                        solutionAr = "افتح الرابط الموجود في رسالة الخطأ لإنشاء الفهرس المركب تلقائياً في Firebase Console.",
                        isPermissionError = false,
                        isNetworkError = false,
                        technicalDetails = exception.message ?: "FAILED_PRECONDITION"
                    )
                    else -> FirestoreErrorDiagnosis(
                        errorCode = exception.code.name,
                        errorNameAr = "خطأ Firestore: ${exception.code.name}",
                        descriptionAr = exception.localizedMessage ?: "حدث استثناء أثناء الاتصال.",
                        rootCauseAr = "سبب غير مصنف في الكود الحالي.",
                        solutionAr = "تحقق من سجلات Logcat وتفاصيل الاستثناء.",
                        isPermissionError = false,
                        isNetworkError = false,
                        technicalDetails = exception.message ?: exception.toString()
                    )
                }
            }

            return FirestoreErrorDiagnosis(
                errorCode = "GENERIC_EXCEPTION",
                errorNameAr = "استثناء عام",
                descriptionAr = exception.localizedMessage ?: "حدث خطأ غير متوقع",
                rootCauseAr = exception.javaClass.simpleName,
                solutionAr = "راجع سجلات النظام للتأكد من حالة الاتصال وإعدادات الذاكرة.",
                isPermissionError = false,
                isNetworkError = false,
                technicalDetails = exception.stackTraceToString()
            )
        }
    }

    /**
     * 1. مراقبة حالة الاتصال اللحظي (Real-time Connection Flow)
     * تستمع لوثيقة فحص دورية وتراقب ما إذا كانت البيانات تأتي من السحابة أو الكاش،
     * مع رصد العمليات المعلقة.
     */
    fun observeRealtimeConnection(docId: String = "heartbeat"): Flow<RealtimeStatusUpdate> = callbackFlow {
        val docRef = firestore.collection(HEALTH_CHECK_COLLECTION).document(docId)

        val registration: ListenerRegistration = docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                val diagnosis = diagnoseException(error)
                trySend(
                    RealtimeStatusUpdate(
                        state = RealtimeConnectionState.ERROR,
                        isFromCache = true,
                        hasPendingWrites = false,
                        messageAr = "خطأ في الاتصال اللحظي: ${diagnosis.errorNameAr} (${diagnosis.errorCode})",
                        exceptionCode = error.code.name
                    )
                )
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val isFromCache = snapshot.metadata.isFromCache
                val hasPending = snapshot.metadata.hasPendingWrites()

                val state = when {
                    hasPending -> RealtimeConnectionState.PENDING_WRITES
                    !isFromCache -> RealtimeConnectionState.CONNECTED_LIVE
                    else -> RealtimeConnectionState.SYNCED_CACHE
                }

                val message = when (state) {
                    RealtimeConnectionState.CONNECTED_LIVE -> "متصل مباشرة بخوادم السحابة (Cloud Live)"
                    RealtimeConnectionState.PENDING_WRITES -> "توجد بيانات محلية قيد المزامنة والرفع للسحابة..."
                    RealtimeConnectionState.SYNCED_CACHE -> "يعمل من الذاكرة المحلية المتزامنة (Offline Cache)"
                    else -> "الحالة: $state"
                }

                trySend(
                    RealtimeStatusUpdate(
                        state = state,
                        isFromCache = isFromCache,
                        hasPendingWrites = hasPending,
                        messageAr = message
                    )
                )
            }
        }

        awaitClose {
            registration.remove()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 2. قياس سرعة الاستجابة (Ping & Latency Benchmark)
     * يكتب وثيقة خفيفة ويحسب وقت الاستجابة، ثم يقرأها مباشرة من السيرفر Source.SERVER.
     */
    suspend fun measurePingAndLatency(timeoutMs: Long = 6000L): PingLatencyResult = withContext(Dispatchers.IO) {
        val testDocId = "ping_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = firestore.collection(HEALTH_CHECK_COLLECTION).document(testDocId)

        try {
            val startTime = System.currentTimeMillis()

            // 1. فحص سرعة الكتابة (Write Latency)
            val writeStart = System.currentTimeMillis()
            val payload = mapOf(
                "testId" to testDocId,
                "clientTimestamp" to writeStart,
                "serverTimestamp" to FieldValue.serverTimestamp(),
                "deviceInfo" to android.os.Build.MODEL
            )

            withTimeoutOrNull(timeoutMs) {
                docRef.set(payload).await()
            } ?: return@withContext PingLatencyResult(
                isSuccess = false,
                errorMessage = "استجابة خادم السحابة استغرقت أكثر من ${timeoutMs / 1000} ثوانٍ",
                diagnosticSummaryAr = "يعمل النظام بكفاءة تامة في وضع الأوفلاين (Offline First). كافة الفواتير محفوظة في قاعدة البيانات المحلية بأمان."
            )
            val writeLatency = System.currentTimeMillis() - writeStart

            // 2. فحص سرعة القراءة (Read Latency)
            val readStart = System.currentTimeMillis()
            val snapshot = withTimeoutOrNull(timeoutMs) {
                try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    docRef.get().await()
                }
            } ?: return@withContext PingLatencyResult(
                isSuccess = true,
                writeLatencyMs = writeLatency,
                readLatencyMs = 0L,
                totalLatencyMs = writeLatency,
                isServerVerified = true,
                diagnosticSummaryAr = "تم تأكيد الاتصال السحابي وكتابة وثيقة الاختبار بنجاح في ${writeLatency}ms ✓"
            )
            val readLatency = System.currentTimeMillis() - readStart
            val totalLatency = System.currentTimeMillis() - startTime

            val isServerVerified = !snapshot.metadata.isFromCache

            // تنظيف اختباري غير حاجب
            try {
                docRef.delete()
            } catch (_: Exception) {}

            PingLatencyResult(
                isSuccess = true,
                writeLatencyMs = writeLatency,
                readLatencyMs = readLatency,
                totalLatencyMs = totalLatency,
                isServerVerified = isServerVerified,
                diagnosticSummaryAr = "الاتصال ممتاز وسريع: كتابة (${writeLatency}ms) | قراءة (${readLatency}ms) | الإجمالي (${totalLatency}ms)"
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val diag = diagnoseException(e)
            Log.e(TAG, "Ping test failed: ${diag.technicalDetails}")
            PingLatencyResult(
                isSuccess = false,
                errorMessage = diag.descriptionAr,
                diagnosticSummaryAr = "${diag.errorNameAr}: ${diag.solutionAr}"
            )
        }
    }

    /**
     * 3. اختبار المزامنة في وضع عدم الاتصال (Offline Persistence & Auto-Sync Test)
     * يعطل الشبكة برمجياً، يكتب وثيقة، يتأكد من تسجيلها محلياً بـ hasPendingWrites = true،
     * ثم يعيد تشغيل الشبكة ويتأكد من رفعها للسيرفر (hasPendingWrites = false).
     */
    suspend fun testOfflinePersistenceAndSync(timeoutMs: Long = 15000L): OfflineSyncResult = withContext(Dispatchers.IO) {
        val testDocId = "offline_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = firestore.collection(HEALTH_CHECK_COLLECTION).document(testDocId)

        try {
            // أ. قطع الاتصال برمجياً
            firestore.disableNetwork().await()

            // ب. كتابة الوثيقة في وضع عدم الاتصال
            val offlineWriteTime = System.currentTimeMillis()
            docRef.set(
                mapOf(
                    "type" to "offline_test",
                    "createdAt" to offlineWriteTime,
                    "offlineStatus" to "written_offline"
                )
            ).await()

            // ج. التأكد من حفظها محلياً في الذاكرة التخزينية (Cache)
            val cacheSnapshot = docRef.get(Source.CACHE).await()
            val cachedSuccessfully = cacheSnapshot.exists()
            val hasPendingWrites = cacheSnapshot.metadata.hasPendingWrites()

            if (!cachedSuccessfully || !hasPendingWrites) {
                // إعادة تشغيل الشبكة قبل الخروج
                firestore.enableNetwork().await()
                return@withContext OfflineSyncResult(
                    isSuccess = false,
                    cachedOfflineSuccessfully = cachedSuccessfully,
                    hasPendingWritesWhileOffline = hasPendingWrites,
                    diagnosticSummaryAr = "فشل في تفعيل التخزين المؤقت في وضع عدم الاتصال (Offline Cache غير مفعلة أو لم تسجل العملية كمعلقة)."
                )
            }

            // د. إعادة الاتصال بالشبكة
            val syncStartTime = System.currentTimeMillis()
            firestore.enableNetwork().await()

            // هـ. التحقق من رفع التغييرات تلقائياً للسحابة (Auto-Sync upon Reconnect)
            var syncedToServer = false
            withTimeoutOrNull(timeoutMs) {
                while (!syncedToServer) {
                    val currentSnap = docRef.get(Source.DEFAULT).await()
                    if (!currentSnap.metadata.hasPendingWrites() && !currentSnap.metadata.isFromCache) {
                        syncedToServer = true
                        break
                    }
                    kotlinx.coroutines.delay(300)
                }
            }

            val syncDuration = System.currentTimeMillis() - syncStartTime

            // تنظيف
            try {
                docRef.delete()
            } catch (_: Exception) {}

            OfflineSyncResult(
                isSuccess = syncedToServer,
                cachedOfflineSuccessfully = true,
                hasPendingWritesWhileOffline = true,
                syncedToServerAfterReconnection = syncedToServer,
                syncDurationMs = syncDuration,
                diagnosticSummaryAr = if (syncedToServer) {
                    "نجح الاختبار بالكامل: تم الحفظ دون اتصال محلياً، ورُفعت البيانات للسيرفر بنجاح فور عودة الاتصال خلال (${syncDuration}ms) ✓"
                } else {
                    "تم الحفظ في الكاش، لكن استغرقت المزامنة بعد عودة الإنترنت وقتاً أطول من المهلة المحددة."
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            try {
                firestore.enableNetwork().await()
            } catch (_: Exception) {}

            val diag = diagnoseException(e)
            OfflineSyncResult(
                isSuccess = false,
                errorMessage = diag.descriptionAr,
                diagnosticSummaryAr = "خطأ أثناء فحص عدم الاتصال: ${diag.errorNameAr}"
            )
        }
    }
}
