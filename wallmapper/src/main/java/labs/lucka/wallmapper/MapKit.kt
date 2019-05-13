package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.content.edit
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class MapKit(private val context: Context) {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private var mapInitialized: Boolean = false
    private val preInitializationTaskList: ArrayList<() -> Unit> = arrayListOf()
    private var selectedStyleIndex: MapStyleIndex = MapStyleIndex(-1, "", "", fileId = "")
    private val snapshotKit: SnapshotKit
    var snapshot: Bitmap? = null

    init {
        Mapbox.getInstance(context, getToken(context))
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
            mapInitialized = true
            preInitializationTaskList.forEach { task -> task() }
            preInitializationTaskList.clear()
            onMapReady(map)
        }
    }

    fun setStyle(callback: () -> Unit = { }) {
        if (!mapInitialized) {
            preInitializationTaskList.add { setStyle(callback) }
            return
        }
        val newSelectedStyleIndex = getSelectedStyleIndex(context)
        if (newSelectedStyleIndex == selectedStyleIndex) {
            callback()
            return
        }
        selectedStyleIndex = newSelectedStyleIndex
        val styleBuilder: Style.Builder =
            if (selectedStyleIndex.isLocal) {
                Style.Builder().fromJson(DataKit.loadStyleJson(context, selectedStyleIndex))
            } else {
                Style.Builder().fromUrl(selectedStyleIndex.url)
            }
        map.setStyle(styleBuilder) { style ->
            handleLabels(context, style)
            callback()
        }
    }

    fun takeSnapshot(width: Int, height: Int, onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit) {

        if (!mapInitialized) {
            preInitializationTaskList.add { takeSnapshot(width, height, onSnapshotReady, onError) }
            return
        }

        map.getStyle { style: Style ->
            handleLabels(context, style)
            snapshotKit.takeSnapshot(
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
        if (mapInitialized) {
            val position = map.cameraPosition
            context.defaultSharedPreferences.edit {
                putFloat(
                    context.getString(R.string.pref_map_last_position_latitude), position.target.latitude.toFloat()
                )
                putFloat(
                    context.getString(R.string.pref_map_last_position_longitude), position.target.longitude.toFloat()
                )
                putFloat(context.getString(R.string.pref_map_last_position_zoom), position.zoom.toFloat())
                putFloat(context.getString(R.string.pref_map_last_position_tilt), position.tilt.toFloat())
                putFloat(context.getString(R.string.pref_map_last_position_bearing), position.bearing.toFloat())
            }
        }
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

        fun useDefaultToken(context: Context) =
            context.defaultSharedPreferences
                .getBoolean(context.getString(R.string.pref_mapbox_use_default_token), true)

        private fun getLuckaStyleIndexList(context: Context): ArrayList<MapStyleIndex> {
            val list = arrayListOf<MapStyleIndex>()
            val luckaStyleNameList = context.resources.getStringArray(R.array.style_lucka_name)
            val luckaStyleAuthorList = context.resources.getStringArray(R.array.style_lucka_author)
            val luckaStyleUrlList = context.resources.getStringArray(R.array.style_lucka_url)
            val count = luckaStyleNameList.size
            for (i in 0 until count) {
                list.add(
                    MapStyleIndex(
                        id = MapStyleIndex.generateNewId(context),
                        name = luckaStyleNameList[i], author = luckaStyleAuthorList[i],
                        type = MapStyleIndex.StyleType.LUCKA, url = luckaStyleUrlList[i]
                    )
                )
            }
            return list
        }

        private fun initializeStyleIndexList(context: Context, list: ArrayList<MapStyleIndex>) {
            list.clear()
            MapStyleIndex.clearIdToZero(context)
            val mapboxStyleNameList = context.resources.getStringArray(R.array.style_mapbox_name)
            val authorMapbox = context.getString(R.string.map_style_default_author)
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[0], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.MAPBOX_STREETS
            ))
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[1], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.LIGHT
            ))
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[2], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.DARK
            ))
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[3], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.OUTDOORS
            ))
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[4], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.SATELLITE
            ))
            list.add(MapStyleIndex(
                id = MapStyleIndex.generateNewId(context),
                name = mapboxStyleNameList[5], author = authorMapbox,
                type = MapStyleIndex.StyleType.MAPBOX, url = Style.SATELLITE_STREETS
            ))
            list.addAll(getLuckaStyleIndexList(context))
            DataKit.saveStyleIndexList(context, list)
        }

        fun checkAndUpdateStyleIndexList(context: Context) {
            val list = DataKit.loadFullStyleIndexList(context)
            if (list.size == 0) {
                initializeStyleIndexList(context, list)
                return
            }
            if (
                context.defaultSharedPreferences
                    .getInt(context.getString(R.string.pref_data_version), DefaultValue.Data.VERSION)
                == DataKit.CURRENT_DATA_VERSION
            ) return

            var startInsertPosition = list.size - 1
            for (i in (list.size - 1) downTo 0) {
                if (list[i].type == MapStyleIndex.StyleType.LUCKA) {
                    list.removeAt(i)
                    startInsertPosition = i
                }
            }
            list.addAll(startInsertPosition, getLuckaStyleIndexList(context))

            DataKit.saveStyleIndexList(context, list)
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_data_version), DataKit.CURRENT_DATA_VERSION)
            }
        }

        fun getSelectedStyleIndex(context: Context): MapStyleIndex {
            checkAndUpdateStyleIndexList(context)
            val mapStyleIndexList = DataKit.loadStyleIndexList(context)

            val selectedStyleId =
                context.defaultSharedPreferences.getInt(
                    context.getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[0].id
                )
            mapStyleIndexList.forEach {
                if (it.id == selectedStyleId) return it
            }
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[0].id)
            }
            return mapStyleIndexList[0]
        }

        fun getRandomStyleIndex(context: Context): MapStyleIndex {
            checkAndUpdateStyleIndexList(context)
            val mapStyleIndexList = DataKit.loadStyleIndexList(context)
            val selectedStyleId = context.defaultSharedPreferences.getInt(
                context.getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[0].id
            )
            val onRandomIndexList = arrayListOf<Int>()
            for (i in 0 until mapStyleIndexList.size) {
                val style = mapStyleIndexList[i]
                if (style.inRandom && style.id != selectedStyleId) onRandomIndexList.add(i)
            }
            if (onRandomIndexList.isEmpty()) {
                return getSelectedStyleIndex(context)
            }
            if (onRandomIndexList.size == 1) {
                val style = mapStyleIndexList[onRandomIndexList[0]]
                context.defaultSharedPreferences.edit {
                    putInt(context.getString(R.string.pref_style_manager_selected_id), style.id)
                }
                return style
            }
            val randomIndex = onRandomIndexList[Random(Date().time).nextInt(0, onRandomIndexList.size - 1)]
            val style = mapStyleIndexList[randomIndex]
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_style_manager_selected_id), style.id)
            }
            return style
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

        fun handleLabels(context: Context, json: String): String {
            if (
                context.defaultSharedPreferences
                    .getBoolean(context.getString(R.string.pref_display_label), true)
            ) return json

            // { key: value } -> object
            // Parse to JsonObject
            val jsonObject = JsonParser().parse(json).asJsonObject
            // Get value of "layers", it should be a list
            val layers = jsonObject.getAsJsonArray("layers")
            // Scan the layer list and remove label layers
            for (i in layers.size() - 1 downTo 0) {
                // Every object in the list is considered as value
                val element = layers[i]
                // Convert the value to object
                val elementJsonObject = element.asJsonObject
                val type = elementJsonObject["type"]
                val layout = elementJsonObject["layout"]
                if (layout != null) {
                    val textField = layout.asJsonObject["text-field"]
                    if (type.asString == "symbol" && textField != null) {
                        layers.remove(i)
                    }
                }
            }
            return jsonObject.toString()
        }
    }
}