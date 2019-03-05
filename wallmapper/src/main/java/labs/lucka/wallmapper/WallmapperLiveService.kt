package labs.lucka.wallmapper

import android.Manifest
import android.app.Service
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
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import org.jetbrains.anko.defaultSharedPreferences

class WallmapperLiveService : WallpaperService() {

    private inner class WallmapperLiveEngine : Engine() {

        private var followLocation: Boolean = false
        private lateinit var locationManager: LocationManager
        private val cameraBuilder: CameraPosition.Builder = CameraPosition.Builder()
        private lateinit var styleIndex: MapStyleIndex
        private lateinit var snapshotKit: SnapshotKit
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
                getString(R.string.pref_live_wallpaper_designate_camera),
                getString(R.string.pref_live_wallpaper_zoom),
                getString(R.string.pref_live_wallpaper_bearing),
                getString(R.string.pref_live_wallpaper_tilt),
                getString(R.string.pref_live_wallpaper_tilt) -> {
                    resetFromPreferences()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            Mapbox.getInstance(this@WallmapperLiveService, getString(R.string.mapbox_default_access_token))
            Mapbox.setAccessToken(MapKit.getToken(this@WallmapperLiveService))

            snapshotKit = SnapshotKit(this@WallmapperLiveService)

            lastLatLng.latitude = defaultSharedPreferences
                .getFloat(getString(R.string.pref_map_last_position_latitude), DefaultValue.Map.LATITUDE.toFloat()).toDouble()
            lastLatLng.longitude = defaultSharedPreferences
                .getFloat(getString(R.string.pref_map_last_position_longitude), DefaultValue.Map.LONGITUDE.toFloat()).toDouble()
            cameraBuilder.target(lastLatLng)

            resetFromPreferences()
            defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

            locationManager = getSystemService(Service.LOCATION_SERVICE) as LocationManager
            refresh()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                resetFromPreferences()
                refresh()
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

        private fun refresh() {
            if (followLocation) {
                requestLocationUpdates()
            } else {
                lastLatLng.latitude = defaultSharedPreferences
                    .getFloat(getString(R.string.pref_map_last_position_latitude), DefaultValue.Map.LATITUDE.toFloat())
                    .toDouble()
                lastLatLng.longitude = defaultSharedPreferences
                    .getFloat(
                        getString(R.string.pref_map_last_position_longitude), DefaultValue.Map.LONGITUDE.toFloat()
                    )
                    .toDouble()
                takeSnapshot()
            }
        }

        private fun requestLocationUpdates() {
            if (
                ContextCompat.checkSelfPermission(
                    this@WallmapperLiveService, Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                var radius = defaultSharedPreferences
                    .getString(getString(R.string.pref_live_wallpaper_radius), "1")?.toFloatOrNull()
                if (radius == null) radius = 1F
                locationManager.requestLocationUpdates(
                    getProvider(), 0L,
                    radius * 1000,
                    locationListener
                )
            } else {
                defaultSharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_live_wallpaper_follow_location), false)
                    .apply()
                followLocation = false
                refresh()
            }
        }

        private fun takeSnapshot() {
            snapshotKit.refresh()
            when (styleIndex.type) {

                MapStyleIndex.StyleType.LOCAL, MapStyleIndex.StyleType.CUSTOMIZED -> {
                    snapshotKit.takeSnapshotJson(
                        desiredMinimumWidth, desiredMinimumHeight,
                        SnapshotKit.handleLabels(
                            this@WallmapperLiveService,
                            DataKit.loadStyleJson(this@WallmapperLiveService, styleIndex.path)
                        ),
                        cameraBuilder.build(), onSnapshotReady, onSnapshotError)
                }

                else -> {
                    snapshotKit.takeSnapshotUrl(
                        desiredMinimumWidth, desiredMinimumHeight,
                        styleIndex.path,
                        cameraBuilder.build(), onSnapshotReady, onSnapshotError)
                }

            }
        }

        private fun redraw(image: Bitmap) {
            val canvas = surfaceHolder.lockCanvas()
            canvas.drawBitmap(image, 0.0F, 0.0F, null)
            surfaceHolder.unlockCanvasAndPost(canvas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                notifyColorsChanged()
            }
        }

        private fun resetFromPreferences() {

            followLocation = defaultSharedPreferences
                .getBoolean(getString(R.string.pref_live_wallpaper_follow_location), false)

            val isCameraPositionDesignated = defaultSharedPreferences
                .getBoolean(getString(R.string.pref_live_wallpaper_designate_camera), true)
            cameraBuilder.zoom(
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_zoom), DefaultValue.Map.ZOOM.toInt()).toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(getString(R.string.pref_map_last_position_zoom), DefaultValue.Map.ZOOM.toFloat()).toDouble()
                }
            )
            cameraBuilder.bearing(
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_bearing), DefaultValue.Map.BEARING.toInt()).toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(getString(R.string.pref_map_last_position_bearing), DefaultValue.Map.BEARING.toFloat()).toDouble()
                }
            )
            cameraBuilder.tilt(
                if (isCameraPositionDesignated) {
                    defaultSharedPreferences
                        .getInt(getString(R.string.pref_live_wallpaper_tilt), DefaultValue.Map.TILT.toInt()).toDouble()
                } else {
                    defaultSharedPreferences
                        .getFloat(getString(R.string.pref_map_last_position_tilt), DefaultValue.Map.TILT.toFloat()).toDouble()
                }
            )

            styleIndex = MapKit.getSelectedStyleIndex(this@WallmapperLiveService)
        }

        private fun getProvider(): String {
            val providerValue = defaultSharedPreferences
                .getString(getString(R.string.pref_live_wallpaper_provider), "1")?.toIntOrNull()
            return when(providerValue) {
                0 -> LocationManager.GPS_PROVIDER
                else -> LocationManager.NETWORK_PROVIDER
            }
        }
    }

    override fun onCreateEngine(): Engine { return WallmapperLiveEngine() }
}