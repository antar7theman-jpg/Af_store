package com.example.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import com.example.data.database.AntarDatabase
import com.example.data.firebase.StaffAccountRepository
import com.example.data.models.WorkerLocation
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import java.util.UUID

class WorkerLocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("worker_tracking", "Worker Tracking", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, "worker_tracking")
            .setContentTitle("تتبع مواقع العمال الميدانية GPS")
            .setContentText("المزامنة التلقائية للموقع نشطة مع الإدارة السحابية")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        try {
            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val db = StaffAccountRepository.getFirestoreDb(applicationContext)
                    val auth = StaffAccountRepository.getFirebaseAuth(applicationContext)
                    val activeSession = StaffAccountRepository.loadPersistentStaffSession(applicationContext)
                    val user = auth.currentUser

                    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val location = try {
                        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } catch (_: SecurityException) {
                        null
                    }

                    if (location != null) {
                        val userId = activeSession?.userId?.filter { it.isDigit() }?.toIntOrNull() ?: 1001
                        val workerName = activeSession?.fullName?.ifBlank { activeSession.email } ?: "عامل ميداني"
                        val targetOwnerUid = activeSession?.parentManagerUid?.ifBlank { activeSession.userId } ?: (user?.uid ?: "")

                        val workerLoc = WorkerLocation(
                            userId = userId,
                            workerName = workerName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            statusNote = "نشط وميداني (مزامنة دورية)",
                            timestamp = System.currentTimeMillis()
                        )

                        // 1. Update local Room database
                        try {
                            AntarDatabase.getDatabase(applicationContext).storeDao().insertOrUpdateWorkerLocation(workerLoc)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val data = hashMapOf(
                            "userId" to userId,
                            "workerName" to workerName,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "statusNote" to "نشط وميداني (مزامنة دورية)",
                            "timestamp" to System.currentTimeMillis()
                        )

                        // 2. Write to root worker_locations
                        db.collection("worker_locations").document(userId.toString()).set(data, SetOptions.merge())

                        // 3. Write to manager-scoped worker_locations
                        if (targetOwnerUid.isNotBlank()) {
                            db.collection("users").document(targetOwnerUid)
                                .collection("worker_locations")
                                .document(userId.toString())
                                .set(data, SetOptions.merge())
                        }

                        // 4. Also record history if user is authenticated
                        if (user != null) {
                            db.collection("users").document(user.uid)
                                .collection("location_history")
                                .document(UUID.randomUUID().toString())
                                .set(data)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3 * 60 * 1000L) // 3 minutes interval
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
