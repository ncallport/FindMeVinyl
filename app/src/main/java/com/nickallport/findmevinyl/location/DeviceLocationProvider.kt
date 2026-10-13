package com.nickallport.findmevinyl.location

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.nickallport.findmevinyl.nominatim.Coordinates
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class DeviceLocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Coordinates? = suspendCancellableCoroutine { continuation ->
        val cancellationSource = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellationSource.cancel() }

        Log.d("DeviceLocation", "Requesting current location")
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSource.token)
            .addOnSuccessListener { location ->
                Log.d("DeviceLocation", "Success, location = $location")
                continuation.resume(location?.let { Coordinates(it.latitude, it.longitude) })
            }
            .addOnFailureListener { error ->
                Log.w("DeviceLocation", "Location lookup failed", error)
                continuation.resumeWithException(error)
            }
    }
}