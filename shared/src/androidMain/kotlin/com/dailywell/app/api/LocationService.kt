package com.dailywell.app.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class LatLng(val latitude: Double, val longitude: Double)

class LocationService(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getLastLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        return try {
            val location: Location? = fusedLocationClient.lastLocation.await()
            location?.let { LatLng(it.latitude, it.longitude) }
                ?: getCurrentLocation()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        return try {
            val cancellationToken = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
