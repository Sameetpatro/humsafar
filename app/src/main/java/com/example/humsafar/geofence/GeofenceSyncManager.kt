// app/src/main/java/com/example/humsafar/geofence/GeofenceSyncManager.kt

package com.example.humsafar.geofence

import android.content.Context
import android.util.Log
import com.example.humsafar.data.HeritageRepository
import com.example.humsafar.models.HeritageSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "GeofenceSyncManager"

/**
 * Fetches heritage sites (from backend or local cache) and registers them
 * as OS-managed geofences via HumsafarGeofenceManager.
 *
 * Instantiate once per component lifecycle (Activity / BootReceiver).
 * Call cancel() when the owning component is destroyed to avoid leaking
 * the internal coroutine scope.
 *
 * Usage:
 *   private val syncManager = GeofenceSyncManager(context)
 *   syncManager.syncAndRegister()
 *   // In onDestroy / onDispose:
 *   syncManager.cancel()
 */
class GeofenceSyncManager(
    private val context: Context,
    private val geofenceManager: HumsafarGeofenceManager = HumsafarGeofenceManager(context)
) {
    /**
     * FIX ISSUE 7: The previous version created a bare CoroutineScope that was
     * never cancelled, causing coroutine/scope leaks when the manager was
     * instantiated multiple times (e.g. from BootReceiver + Activity + WorkManager).
     *
     * Fix: expose a cancel() function and call it from the owning component's
     * teardown (onDestroy, onDispose, etc.).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncAndRegister() {
        scope.launch {
            try {
                val sites = fetchSites()
                Log.i(TAG, "Fetched ${sites.size} sites for geofence registration")

                geofenceManager.registerGeofences(
                    sites     = sites,
                    onSuccess = { Log.i(TAG, "Geofences registered successfully") },
                    onFailure = { error -> Log.e(TAG, "Geofence registration failed: $error") }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch/register geofences", e)
            }
        }
    }

    /**
     * FIX BUG 5: The previous version called RetrofitClient.api.getHeritageSites()
     * which DOES NOT EXIST in your ChatApiService — it only has sendMessage().
     * Calling a non-existent method causes an immediate compilation error.
     *
     * Fix: use HeritageRepository as the source of truth (which is what you
     * already have). When you add a /sites endpoint to your backend, wire it
     * here by replacing the HeritageRepository.sites line with your Retrofit call
     * and adding the corresponding method to ChatApiService.
     *
     * Example of what to add to ChatApiService when ready:
     *   @GET("sites")
     *   suspend fun getHeritageSites(): List<HeritageSite>
     *
     * Then replace the body below with:
     *   return RetrofitClient.api.getHeritageSites()
     */
    private suspend fun fetchSites(): List<HeritageSite> {
        // Currently returns the local static list.
        // Replace with a Retrofit call when your backend exposes a /sites endpoint.
        return HeritageRepository.sites
    }

    /**
     * Cancel the internal coroutine scope. Call this from the owning component's
     * onDestroy() or DisposableEffect onDispose block to prevent leaks.
     */
    fun cancel() {
        scope.cancel()
    }
}