package labs.lucka.mapler

import android.Manifest
import android.app.WallpaperColors
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.runOnUiThread
import java.util.*
import kotlin.concurrent.timer

/**
 * The service for live wallpaper.
 *
 * @property FOLLOW_LOCATION_RADIUS_BUFFER When the distance between last location and updated
 * position is in the buffer range, remove the old updates and request the regular one.
 *
 * @author lucka-me
 * @since 0.1
 */
class MaplerLiveService : WallpaperService() {

    private inner class MaplerLiveEngine(val context: Context) : Engine() {

        private var timerRandomStyle: Timer = Timer()
        private var timerRandomStyleEnabled: Boolean = false
        private var timerRandomStyleInterval: Int = 0
        private lateinit var timerTaskRandomStyle: TimerTask.() -> Unit

        private val cameraBuilder: CameraPosition.Builder = CameraPosition.Builder()
        private var zoom: Double = -1.0
        private var bearing: Double = -1.0
        private var tilt: Double = -1.0
        private var styleData: StyleData = StyleData("", "", "")
        private lateinit var snapshotKit: SnapshotKit

        private var lastImage: Bitmap? = null
        val onSnapshotReady: (Bitmap) -> Unit = { image ->
            lastImage = image
            redraw(image)
        }
        val onSnapshotError: (String?) -> Unit = { takeSnapshot() }

        private var followLocation: Boolean = false
        private var followLocationRadius: Float = -1.0F
        private val lastLatLng: LatLng = LatLng()
        private var locationManager: LocationManager? = null
        private val locationListenerFirst: LocationListener = object : LocationListener {

            override fun onLocationChanged(location: Location?) {

                if (location != null) {
                    val distance = lastLatLng.distanceTo(LatLng(location))
                    if (distance >= followLocationRadius) {
                        lastLatLng.latitude = location.latitude
                        lastLatLng.longitude = location.longitude
                        requestLocationUpdates()
                    } else {
                        requestLocationUpdates(
                            (followLocationRadius - distance - FOLLOW_LOCATION_RADIUS_BUFFER)
                                .toFloat()
                        )
                    }
                } else {
                    requestLocationUpdates()
                }
                takeSnapshot()

            }

            override fun onProviderDisabled(provider: String?) { }
            override fun onProviderEnabled(provider: String?) { }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }

        }
        private val locationListener: LocationListener = object : LocationListener {

            override fun onLocationChanged(location: Location?) {

                if (location == null) return

                val distance = lastLatLng.distanceTo(LatLng(location))
                if (distance >= followLocationRadius) {
                    lastLatLng.latitude = location.latitude
                    lastLatLng.longitude = location.longitude
                    takeSnapshot()
                } else {
                    locationManager?.removeUpdates(this)
                    if (distance >= (followLocationRadius - FOLLOW_LOCATION_RADIUS_BUFFER)) {
                        lastLatLng.latitude = location.latitude
                        lastLatLng.longitude = location.longitude
                        takeSnapshot()
                        requestLocationUpdates()
                    } else {
                        requestLocationUpdates(
                            (followLocationRadius - distance - FOLLOW_LOCATION_RADIUS_BUFFER)
                                .toFloat()
                        )
                    }
                }

            }

            override fun onProviderDisabled(provider: String?) { }
            override fun onProviderEnabled(provider: String?) { }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            Mapbox.getInstance(context, MapKit.getToken(context))

            snapshotKit = SnapshotKit(context)

            locationManager = getSystemService()

            timerTaskRandomStyle = {
                styleData = MapKit.getRandomStyleData(context)
                runOnUiThread { takeSnapshot() }
            }

            if (checkCameraPreferences()) takeSnapshot()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                if (checkPreferences()) takeSnapshot()
            } else {
                locationManager?.removeUpdates(locationListener)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?, format: Int, width: Int, height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            takeSnapshot()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            timerRandomStyle.cancel()
            snapshotKit.onPause()
            locationManager?.removeUpdates(locationListener)
        }

        override fun onComputeColors(): WallpaperColors? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val image = lastImage
                if (image != null) {
                    WallpaperColors.fromBitmap(image)
                } else {
                    null
                }
            } else {
                super.onComputeColors()
            }
        }

        private fun takeSnapshot() {
            snapshotKit.refresh()
            cameraBuilder.target(lastLatLng)
            snapshotKit.takeSnapshot(
                desiredMinimumWidth, desiredMinimumHeight, styleData, cameraBuilder.build(),
                onSnapshotReady, onSnapshotError
            )
        }

        private fun redraw(image: Bitmap) {
            val canvas = surfaceHolder.lockCanvas()
            canvas.drawBitmap(image, 0.0F, 0.0F, null)
            surfaceHolder.unlockCanvasAndPost(canvas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                notifyColorsChanged()
            }
        }

        /**
         * Check preferences and update the members, may request single location update with
         * [checkLocationPreferences].
         *
         * @return Whether should take snapshot or not
         *
         * @author lucka-me
         * @since 0.1.10
         */
        private fun checkPreferences(): Boolean {
            var shouldRefresh = false

            // Check timer
            var shouldResetTimer = false
            val newTimerRandomStyleEnabled = defaultSharedPreferences
                .getBoolean(getString(R.string.pref_live_wallpaper_style_random), false)
            if (timerRandomStyleEnabled != newTimerRandomStyleEnabled) {
                timerRandomStyleEnabled = newTimerRandomStyleEnabled
                if (timerRandomStyleEnabled) {
                    shouldResetTimer = true
                } else {
                    timerRandomStyle.cancel()
                }
            }
            val newTimerRandomStyleInterval = defaultSharedPreferences
                .getString(
                    getString(R.string.pref_live_wallpaper_style_random_interval),
                    DefaultValue.LiveWallpaper.RANDOM_STYLE_INTERVAL.toString()
                )
                ?.toIntOrNull() ?: 0
            if (newTimerRandomStyleInterval != timerRandomStyleInterval) {
                if (newTimerRandomStyleInterval > 0) {
                    timerRandomStyleInterval = newTimerRandomStyleInterval
                    shouldResetTimer = true
                } else {
                    timerRandomStyle.cancel()
                }
            }
            if (shouldResetTimer) {
                timerRandomStyle.cancel()
                timerRandomStyle = timer(
                    initialDelay = timerRandomStyleInterval * 60000L,
                    period = timerRandomStyleInterval * 60000L,
                    action = timerTaskRandomStyle
                )
            }

            // Check style
            if (!timerRandomStyleEnabled) {
                val newStyleData = MapKit.getSelectedStyleData(context)
                if (newStyleData.uid != styleData.uid) {
                    styleData = newStyleData
                    shouldRefresh = true
                }
            } else if (styleData.uri.isEmpty()) {
                styleData = MapKit.getSelectedStyleData(context)
                shouldRefresh = true
            }

            // Check Camera
            if (checkCameraPreferences()) shouldRefresh = true

            // Check location
            return checkLocationPreferences(shouldRefresh)
        }

        private fun checkCameraPreferences(): Boolean {
            var shouldRefresh = false

            val isCameraPositionDesignated = defaultSharedPreferences.getBoolean(
                getString(R.string.pref_live_wallpaper_camera_designate), true
            )

            val newZoom = if (isCameraPositionDesignated) {
                defaultSharedPreferences
                    .getInt(
                        getString(R.string.pref_live_wallpaper_camera_zoom),
                        DefaultValue.Map.ZOOM.toInt()
                    )
                    .toDouble()
            } else {
                defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_camera_zoom),
                        DefaultValue.Map.ZOOM.toFloat()
                    )
                    .toDouble()
            }
            if (zoom != newZoom) {
                zoom = newZoom
                cameraBuilder.zoom(zoom)
                shouldRefresh = true
            }

            val newBearing = if (isCameraPositionDesignated) {
                defaultSharedPreferences
                    .getInt(
                        getString(R.string.pref_live_wallpaper_camera_bearing),
                        DefaultValue.Map.BEARING.toInt()
                    )
                    .toDouble()
            } else {
                defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_camera_bearing),
                        DefaultValue.Map.BEARING.toFloat()
                    )
                    .toDouble()
            }
            if (bearing != newBearing) {
                bearing = newBearing
                cameraBuilder.bearing(bearing)
                shouldRefresh = true
            }

            val newTilt =
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(
                            getString(R.string.pref_live_wallpaper_camera_tilt),
                            DefaultValue.Map.TILT.toInt()
                        )
                        .toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(
                            getString(R.string.pref_map_last_camera_tilt),
                            DefaultValue.Map.TILT.toFloat()
                        )
                        .toDouble()
                }
            if (tilt != newTilt) {
                tilt = newTilt
                cameraBuilder.tilt(tilt)
                shouldRefresh = true
            }

            return shouldRefresh
        }

        /**
         * Check preferences related to location and update the members, may request single location
         * update.
         *
         * @param preShouldRefresh The conclusion of whether should refresh or not before calling
         * this method
         * @return Whether should refresh or not
         *
         * @author lucka-me
         * @since 0.1.10
         */
        private fun checkLocationPreferences(preShouldRefresh: Boolean): Boolean {

            var shouldRequestUpdates = false
            var shouldRefresh = false

            followLocation = defaultSharedPreferences
                .getBoolean(getString(R.string.pref_live_wallpaper_location_follow), false)
            if (followLocation &&
                ContextCompat
                    .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_location_follow), false)
                }
                followLocation = false
            }

            followLocationRadius = defaultSharedPreferences
                .getString(
                    getString(R.string.pref_live_wallpaper_location_radius),
                    DefaultValue.LiveWallpaper.RADIUS.toString()
                )
                ?.toFloatOrNull()
                ?: -1.0F
            followLocationRadius *= 1000.0F
            if (followLocationRadius <= 0.0) followLocation = false

            if (followLocation) {
                shouldRequestUpdates = true
            } else {
                val newLatitude = defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_camera_latitude),
                        DefaultValue.Map.LATITUDE.toFloat()
                    )
                    .toDouble()
                val newLongitude = defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_camera_longitude),
                        DefaultValue.Map.LONGITUDE.toFloat()
                    )
                    .toDouble()
                if (
                    (lastLatLng.latitude != newLatitude) || (lastLatLng.longitude != newLongitude)
                ) {
                    lastLatLng.latitude = newLatitude
                    lastLatLng.longitude = newLongitude
                    shouldRefresh = true
                }
            }

            var hasRequestedUpdate = false
            if (shouldRequestUpdates) {
                if (
                    ContextCompat
                        .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    val provider = getProvider()
                    val newLocation = locationManager?.getLastKnownLocation(provider)
                    if (newLocation != null) {
                        if (lastLatLng.distanceTo(LatLng(newLocation)) >= followLocationRadius) {
                            lastLatLng.longitude = newLocation.longitude
                            lastLatLng.latitude = newLocation.latitude
                            shouldRefresh = true
                        }
                    } else {
                        requestFirstLocationUpdate()
                        hasRequestedUpdate = true
                    }
                }
            }

            return if (hasRequestedUpdate) false else shouldRefresh || preShouldRefresh
        }

        private fun requestFirstLocationUpdate() {
            if (ContextCompat
                    .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                locationManager
                    ?.requestSingleUpdate(getProvider(), locationListenerFirst, null)
            } else {
                defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_location_follow), false)
                }
                followLocation = false
            }
        }

        private fun requestLocationUpdates(distance: Float = followLocationRadius) {
            if (ContextCompat
                    .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                locationManager
                    ?.requestLocationUpdates(getProvider(), 0L, distance, locationListener)
            } else {
                defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_location_follow), false)
                }
                followLocation = false
            }
        }

        private fun getProvider(): String {
            val index = defaultSharedPreferences
                .getString(getString(R.string.pref_live_wallpaper_location_provider), "1")
                ?.toIntOrNull()

            return when (index) {
                0 -> LocationManager.GPS_PROVIDER
                else -> LocationManager.NETWORK_PROVIDER
            }
        }

    }

    override fun onCreateEngine(): Engine { return MaplerLiveEngine(this) }

    companion object {
        const val FOLLOW_LOCATION_RADIUS_BUFFER = 40.0
    }

}