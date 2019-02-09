package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import org.jetbrains.anko.defaultSharedPreferences

class MapKit(private val context: Context) {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private val snapshotKit: SnapshotKit
    var snapshot: Bitmap? = null

    init {
        Mapbox.getInstance(context, context.getString(R.string.mapbox_default_access_token))
        setToken(context)
        snapshotKit = SnapshotKit(context)
    }

    fun onCreate(
        savedInstanceState: Bundle?,
        mapView: MapView,
        onMapReady: (newMap: MapboxMap) -> Unit
    ) {
        this.mapView = mapView
        this.mapView.onCreate(savedInstanceState)
        this.mapView.getMapAsync { newMap: MapboxMap ->

            map = newMap


            val centerLat: Float = context.defaultSharedPreferences
                .getFloat(context.getString(R.string.pref_map_last_position_latitude), DefaultValue.Map.LATITUDE.toFloat())
            val centerLng: Float = context.defaultSharedPreferences
                .getFloat(context.getString(R.string.pref_map_last_position_longitude), DefaultValue.Map.LONGITUDE.toFloat())
            val zoom: Float = context.defaultSharedPreferences
                .getFloat(context.getString(R.string.pref_map_last_position_zoom), DefaultValue.Map.ZOOM.toFloat())
            val tilt: Float = context.defaultSharedPreferences
                .getFloat(context.getString(R.string.pref_map_last_position_tilt), DefaultValue.Map.TILT.toFloat())
            val bearing: Float = context.defaultSharedPreferences
                .getFloat(context.getString(R.string.pref_map_last_position_bearing), DefaultValue.Map.BEARING.toFloat())
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(centerLat.toDouble(), centerLng.toDouble()))
                .zoom(zoom.toDouble())
                .tilt(tilt.toDouble())
                .bearing(bearing.toDouble())
                .build()
            onMapReady(map)
            setStyle()

        }
    }

    fun setStyle(callback: () -> Unit = { }) {
        val selectedStyleIndex = getSelectedStyleIndex(context)
        val styleBuilder: Style.Builder =
            when (selectedStyleIndex.type) {

                MapStyleIndex.StyleType.LOCAL, MapStyleIndex.StyleType.CUSTOMIZED -> {
                    Style.Builder().fromJson(DataKit.loadStyleJson(context, selectedStyleIndex.path))
                }

                else -> {
                    Style.Builder().fromUrl(selectedStyleIndex.path)
                }
            }
        map.setStyle(styleBuilder) { style ->
            handleLabels(context, style)
            callback()
        }
    }

    fun takeSnapshot(width: Int, height: Int, onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit) {

        map.getStyle { style: Style ->
            handleLabels(context, style)
            snapshotKit.takeSnapshotJson(
                width, height, style.json, map.cameraPosition, { image ->
                    snapshot = image
                    onSnapshotReady(image)
                }, { error: String? ->

                onError(error)
                }
            )
        }
    }

    fun onStart    () { mapView.onStart    () }
    fun onStop     () { mapView.onStop     () }
    fun onLowMemory() { mapView.onLowMemory() }
    fun onDestroy  () { mapView.onDestroy  () }
    fun onSaveInstanceState(outState: Bundle?) { if (outState != null) mapView.onSaveInstanceState(outState) }
    fun onResume  () {  mapView.onResume   () }

    fun onPause() {
        val position = map.cameraPosition
        context.defaultSharedPreferences.edit()
            .putFloat(context.getString(R.string.pref_map_last_position_latitude), position.target.latitude.toFloat())
            .putFloat(context.getString(R.string.pref_map_last_position_longitude), position.target.longitude.toFloat())
            .putFloat(context.getString(R.string.pref_map_last_position_zoom), position.zoom.toFloat())
            .putFloat(context.getString(R.string.pref_map_last_position_tilt), position.tilt.toFloat())
            .putFloat(context.getString(R.string.pref_map_last_position_bearing), position.bearing.toFloat())
            .apply()
        mapView.onPause()
        snapshotKit.onPause()
    }

    companion object {

        fun getToken(context: Context): String {
            var token: String =
                if (
                    context.defaultSharedPreferences.getBoolean(
                        context.getString(R.string.pref_mapbox_use_default_token), true
                    )
                ) {
                    context.getString(R.string.mapbox_default_access_token)
                } else {
                    context.defaultSharedPreferences
                        .getString(
                            context.getString(R.string.pref_mapbox_token),
                            context.getString(R.string.pref_mapbox_token_default)
                        ) ?: context.getString(R.string.pref_mapbox_token_default)
                }
            if (token.isBlank()) token = context.getString(R.string.pref_mapbox_token_default)
            return token
        }

        fun setToken(context: Context) {
            Mapbox.setAccessToken(getToken(context))
        }

        private fun initializeStyleList(context: Context, list: ArrayList<MapStyleIndex>) {
            list.clear()
            val mapboxStyleNameList = context.resources.getStringArray(R.array.style_mapbox_name)
            list.add(MapStyleIndex(
                mapboxStyleNameList[0],
                context.getString(R.string.map_style_default_author),
                Style.MAPBOX_STREETS, MapStyleIndex.StyleType.MAPBOX
            ))
            list.add(MapStyleIndex(
                mapboxStyleNameList[1],
                context.getString(R.string.map_style_default_author),
                Style.LIGHT, MapStyleIndex.StyleType.MAPBOX
            ))
            list.add(MapStyleIndex(
                mapboxStyleNameList[2],
                context.getString(R.string.map_style_default_author),
                Style.DARK, MapStyleIndex.StyleType.MAPBOX
            ))
            list.add(MapStyleIndex(
                mapboxStyleNameList[3],
                context.getString(R.string.map_style_default_author),
                Style.OUTDOORS, MapStyleIndex.StyleType.MAPBOX
            ))
            list.add(MapStyleIndex(
                mapboxStyleNameList[4],
                context.getString(R.string.map_style_default_author),
                Style.SATELLITE, MapStyleIndex.StyleType.MAPBOX
            ))
            list.add(MapStyleIndex(
                mapboxStyleNameList[5],
                context.getString(R.string.map_style_default_author),
                Style.SATELLITE_STREETS, MapStyleIndex.StyleType.MAPBOX
            ))
            if (context.defaultSharedPreferences.getBoolean(
                    context.getString(R.string.pref_mapbox_use_default_token), true
                )) {
                val luckaStyleNameList = context.resources.getStringArray(R.array.style_lucka_name)
                val luckaStyleAuthorList = context.resources.getStringArray(R.array.style_lucka_author)
                val luckaStyleUrlList = context.resources.getStringArray(R.array.style_lucka_url)
                val count = luckaStyleNameList.size
                for (i in 0 until count) {
                    list.add(MapStyleIndex(
                        luckaStyleNameList[i], luckaStyleAuthorList[i], luckaStyleUrlList[i],
                        MapStyleIndex.StyleType.LUCKA
                    ))
                }
            }
            DataKit.saveStyleIndexList(context, list)
        }

        fun checkAndUpdateStyleList(context: Context) {
            val list = DataKit.loadStyleIndexList(context)
            if (list.size == 0) {
                initializeStyleList(context, list)
                return
            }
            if (
                context.defaultSharedPreferences.getInt(context.getString(R.string.pref_data_version), 1)
                == DataKit.CURRENT_DATA_VERSION
            ) return

            // Update list when using default token
            if (
                !context.defaultSharedPreferences.getBoolean(
                    context.getString(R.string.pref_mapbox_use_default_token), true
                )
            ) return

            var startInsertPosition = list.size - 1
            for (i in (list.size - 1) downTo 0) {
                if (list[i].type == MapStyleIndex.StyleType.LUCKA) {
                    list.removeAt(i)
                    startInsertPosition = i
                }
            }
            val luckaStyleNameList = context.resources.getStringArray(R.array.style_lucka_name)
            val luckaStyleAuthorList = context.resources.getStringArray(R.array.style_lucka_author)
            val luckaStyleUrlList = context.resources.getStringArray(R.array.style_lucka_url)
            val count = luckaStyleNameList.size
            for (i in 0 until count) {
                list.add(startInsertPosition + i, MapStyleIndex(
                    luckaStyleNameList[i], luckaStyleAuthorList[i], luckaStyleUrlList[i],
                    MapStyleIndex.StyleType.LUCKA
                ))

            }
            DataKit.saveStyleIndexList(context, list)
            context.defaultSharedPreferences.edit()
                .putInt(context.getString(R.string.pref_data_version), DataKit.CURRENT_DATA_VERSION)
                .apply()
        }

        fun getSelectedStyleIndex(context: Context): MapStyleIndex {
            val selectedStyleIndex: Int = context.defaultSharedPreferences.getInt(
                context.getString(R.string.pref_style_manager_selected_index), 0
            )
            MapKit.checkAndUpdateStyleList(context)
            val mapStyleIndexList = DataKit.loadStyleIndexList(context)
            return mapStyleIndexList[if (selectedStyleIndex >= mapStyleIndexList.size) 0 else selectedStyleIndex]
        }

        fun handleLabels(context: Context, style: Style) {
            if (
                !context.defaultSharedPreferences
                    .getBoolean(context.getString(R.string.pref_display_label), true)
            ) {
                style.layers.forEach { layer ->
                    if (layer is SymbolLayer && !layer.textField.isNull) { style.removeLayer(layer) }
                }
            }
        }
    }
}