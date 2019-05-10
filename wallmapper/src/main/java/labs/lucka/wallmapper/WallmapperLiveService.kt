package labs.lucka.wallmapper

import android.Manifest
import android.app.WallpaperColors
import android.content.SharedPreferences
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
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.runOnUiThread
import java.util.*
import kotlin.concurrent.timer

class WallmapperLiveService : WallpaperService() {

    private inner class WallmapperLiveEngine : Engine() {

        private var timerRandomStyle: Timer = Timer()
        private var timerRandomStyleEnabled: Boolean = false
        private var timerRandomStyleInterval: Int = 0
        private val  timerTaskRandomStyle: TimerTask.() -> Unit =  {
            styleIndex = MapKit.getRandomStyleIndex(this@WallmapperLiveService)
            runOnUiThread { takeSnapshot() }
        }

        private var followLocation: Boolean = false
        private lateinit var locationManager: LocationManager

        private val cameraBuilder: CameraPosition.Builder = CameraPosition.Builder()
        private var zoom: Double = -1.0
        private var bearing: Double = -1.0
        private var tilt: Double = -1.0
        private var styleIndex: MapStyleIndex = MapKit.getSelectedStyleIndex(this@WallmapperLiveService)
        private val snapshotKit: SnapshotKit = SnapshotKit(this@WallmapperLiveService)

        private val lastLatLng: LatLng = LatLng()
        private var lastImage: Bitmap? = null

        val onSnapshotReady: (Bitmap) -> Unit = { image ->
            lastImage = image
            redraw(image)
        }
        val onSnapshotError: (String?) -> Unit = { error ->
            System.out.println(error)
            takeSnapshot()
        }

        val locationListener: LocationListener = object : LocationListener {

            override fun onLocationChanged(location: Location?) {
                if (location == null) return
                lastLatLng.latitude = location.latitude
                lastLatLng.longitude = location.longitude
                takeSnapshot()
            }

            override fun onProviderDisabled(provider: String?) { }
            override fun onProviderEnabled(provider: String?) { }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        }

        val onSharedPreferenceChangeListener: (SharedPreferences, String) -> Unit = { _, key ->
            when (key) {
                getString(R.string.pref_live_wallpaper_random_style),
                getString(R.string.pref_live_wallpaper_random_style_interval) -> {
                    updateRandomStyleTimerFromPreferences()
                }

                getString(R.string.pref_live_wallpaper_follow_location) -> {
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            Mapbox.getInstance(this@WallmapperLiveService, MapKit.getToken(this@WallmapperLiveService))

            lastLatLng.latitude = defaultSharedPreferences
                .getFloat(getString(R.string.pref_map_last_position_latitude), DefaultValue.Map.LATITUDE.toFloat()).toDouble()
            lastLatLng.longitude = defaultSharedPreferences
                .getFloat(getString(R.string.pref_map_last_position_longitude), DefaultValue.Map.LONGITUDE.toFloat()).toDouble()
            cameraBuilder.target(lastLatLng)

            resetAllFromPreferences()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

            locationManager = getSystemService(LocationManager::class.java)
            refresh(isFirst = true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                var preferencesChanged = resetCameraFromPreferences()
                if (
                    !defaultSharedPreferences
                        .getBoolean(getString(R.string.pref_live_wallpaper_random_style), false)
                )
                    preferencesChanged = resetStyleIndexFromPreferences()
                refresh(preferencesChanged)
            } else {
                locationManager.removeUpdates(locationListener)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            takeSnapshot()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            timerRandomStyle.cancel()
            snapshotKit.onPause()
            locationManager.removeUpdates(locationListener)
            defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
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

        private fun refresh(preferencesChanged: Boolean = true, isFirst: Boolean = false) {
            var shouldTakeSnapshot = preferencesChanged
            if (followLocation) {
                requestLocationUpdates(isFirst)
                if (isFirst) shouldTakeSnapshot = false // Will take snapshot
            } else {
                val newLatitude = defaultSharedPreferences
                    .getFloat(getString(R.string.pref_map_last_position_latitude), DefaultValue.Map.LATITUDE.toFloat())
                    .toDouble()
                val newLongitude = defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_position_longitude), DefaultValue.Map.LONGITUDE.toFloat()
                    )
                    .toDouble()
                if (newLatitude != lastLatLng.latitude || newLongitude != newLongitude) {
                    lastLatLng.latitude = newLatitude
                    lastLatLng.longitude = newLongitude
                    shouldTakeSnapshot = true
                }
            }
            if (shouldTakeSnapshot) takeSnapshot()
        }

        private fun requestLocationUpdates(getLocationImmediately: Boolean = false) {
            if (
                ContextCompat.checkSelfPermission(
                    this@WallmapperLiveService, Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                var radius = defaultSharedPreferences
                    .getString(getString(R.string.pref_live_wallpaper_radius), DefaultValue.LiveWallpaper.RADIUS.toString())
                    ?.toFloatOrNull()
                if (radius == null) radius = DefaultValue.LiveWallpaper.RADIUS
                val provider = getProvider()
                locationManager.requestLocationUpdates(
                    provider, 0L,
                    radius * 1000,
                    locationListener
                )
                // May not return a location immediately
                if (getLocationImmediately)
                    locationListener.onLocationChanged(locationManager.getLastKnownLocation(provider))
            } else {
                defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_follow_location), false)
                }
                followLocation = false
            }
        }

        private fun takeSnapshot() {
            snapshotKit.refresh()
            snapshotKit.takeSnapshot(
                desiredMinimumWidth, desiredMinimumHeight, styleIndex, cameraBuilder.build(),
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

        private fun resetAllFromPreferences() {
            updateRandomStyleTimerFromPreferences()
            resetCameraFromPreferences()
            resetStyleIndexFromPreferences()
        }

        private fun updateRandomStyleTimerFromPreferences() {
            if (
                defaultSharedPreferences
                    .getBoolean(getString(R.string.pref_live_wallpaper_random_style), false)
            ) {
                val newInterval =
                    defaultSharedPreferences.getString(
                        getString(R.string.pref_live_wallpaper_random_style_interval),
                        DefaultValue.LiveWallpaper.RANDOM_STYLE_INTERVAL.toString()
                    )?.toIntOrNull()
                if (newInterval == null || newInterval == 0) {
                    timerRandomStyleInterval = 0
                    timerRandomStyle.cancel()
                    timerRandomStyleEnabled = false
                } else if (!timerRandomStyleEnabled || newInterval != timerRandomStyleInterval) {
                    timerRandomStyle.cancel()
                    timerRandomStyle =
                        timer(
                            initialDelay = newInterval * 60000L, period = newInterval * 60000L,
                            action = timerTaskRandomStyle
                        )
                    timerRandomStyleInterval = newInterval
                    timerRandomStyleEnabled = true
                }
            } else {
                if (timerRandomStyleEnabled) {
                    timerRandomStyle.cancel()
                    timerRandomStyleEnabled = false
                }
            }
        }

        /**
         * Reset [followLocation] and [cameraBuilder] from preferences
         *
         * @return [Boolean] Whether the values are reset or not
         * @author lucka-me
         * @since 0.1.4
         */
        private fun resetCameraFromPreferences(): Boolean {
            var isReset = false
            val newFollowLocation =
                defaultSharedPreferences
                    .getBoolean(getString(R.string.pref_live_wallpaper_follow_location), false)
            if (followLocation != newFollowLocation) {
                followLocation = newFollowLocation
                isReset = true
            }

            val isCameraPositionDesignated = defaultSharedPreferences
                .getBoolean(getString(R.string.pref_live_wallpaper_designate_camera), true)

            val newZoom =
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_zoom), DefaultValue.Map.ZOOM.toInt()).toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(getString(R.string.pref_map_last_position_zoom), DefaultValue.Map.ZOOM.toFloat())
                        .toDouble()
                }
            if (zoom != newZoom) {
                zoom = newZoom
                cameraBuilder.zoom(zoom)
                isReset = true
            }

            val newBearing =
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_bearing), DefaultValue.Map.BEARING.toInt())
                        .toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(
                            getString(R.string.pref_map_last_position_bearing), DefaultValue.Map.BEARING.toFloat()
                        )
                        .toDouble()
                }
            if (bearing != newBearing) {
                bearing = newBearing
                cameraBuilder.bearing(bearing)
                isReset = true
            }

            val newTilt =
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_tilt), DefaultValue.Map.TILT.toInt()).toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(getString(R.string.pref_map_last_position_tilt), DefaultValue.Map.TILT.toFloat())
                        .toDouble()
                }
            if (tilt != newTilt) {
                tilt = newTilt
                cameraBuilder.tilt(tilt)
                isReset = true
            }

            return isReset
        }

        /**
         * Reset [styleIndex] from preferences
         *
         * @return [Boolean] Whether the [styleIndex] is reset or not
         * @author lucka-me
         * @since 0.1.4
         */
        private fun resetStyleIndexFromPreferences(): Boolean {
            val newStyleIndex = MapKit.getSelectedStyleIndex(this@WallmapperLiveService)
            return if (styleIndex != newStyleIndex) {
                styleIndex = newStyleIndex
                true
            } else {
                false
            }
        }

        private fun getProvider(): String =
            when (defaultSharedPreferences
                .getString(getString(R.string.pref_live_wallpaper_provider), "1")?.toIntOrNull()) {
                0 -> LocationManager.GPS_PROVIDER
                else -> LocationManager.NETWORK_PROVIDER
            }
    }

    override fun onCreateEngine(): Engine { return WallmapperLiveEngine() }
}