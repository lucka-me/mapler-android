package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import androidx.core.content.edit
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.windowManager
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class MapKit(private val context: Context) {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private var mapInitialized: Boolean = false
    private val preInitializationTaskList: ArrayList<() -> Unit> = arrayListOf()
    private var styleDataUid = ""
    private val snapshotKit: SnapshotKit
    var snapshot: Bitmap? = null

    private var useDefaultToken: Boolean
    private var displayLabels: Boolean

    init {
        Mapbox.getInstance(context, getToken(context))
        snapshotKit = SnapshotKit(context)
        useDefaultToken = useDefaultToken(context)
        displayLabels = displayLabels(context)
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
            val preferences = context.defaultSharedPreferences
            val centerLat: Float = preferences
                .getFloat(
                    context.getString(R.string.pref_map_last_position_latitude),
                    DefaultValue.Map.LATITUDE.toFloat()
                )
            val centerLng: Float = preferences
                .getFloat(
                    context.getString(R.string.pref_map_last_position_longitude),
                    DefaultValue.Map.LONGITUDE.toFloat()
                )
            val zoom: Float = preferences
                .getFloat(
                    context.getString(R.string.pref_map_last_position_zoom),
                    DefaultValue.Map.ZOOM.toFloat()
                )
            val tilt: Float = preferences
                .getFloat(
                    context.getString(R.string.pref_map_last_position_tilt),
                    DefaultValue.Map.TILT.toFloat()
                )
            val bearing: Float = preferences
                .getFloat(
                    context.getString(R.string.pref_map_last_position_bearing),
                    DefaultValue.Map.BEARING.toFloat()
                )
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

    fun onStart() { mapView.onStart() }

    fun onResume() { mapView.onResume() }

    fun onPause() {
        if (mapInitialized) {
            val position = map.cameraPosition
            context.defaultSharedPreferences.edit {
                putFloat(
                    context.getString(R.string.pref_map_last_position_latitude),
                    position.target.latitude.toFloat()
                )
                putFloat(
                    context.getString(R.string.pref_map_last_position_longitude),
                    position.target.longitude.toFloat()
                )
                putFloat(
                    context.getString(R.string.pref_map_last_position_zoom),
                    position.zoom.toFloat()
                )
                putFloat(
                    context.getString(R.string.pref_map_last_position_tilt),
                    position.tilt.toFloat()
                )
                putFloat(
                    context.getString(R.string.pref_map_last_position_bearing),
                    position.bearing.toFloat()
                )
            }
        }
        mapView.onPause()
        snapshotKit.onPause()
    }

    fun onStop() { mapView.onStop() }

    fun onSaveInstanceState(outState: Bundle?) {
        if (outState != null) mapView.onSaveInstanceState(outState)
    }

    fun onLowMemory() { mapView.onLowMemory() }

    fun onDestroy() { mapView.onDestroy() }


    fun setStyle(
        newStyleData: StyleData, force: Boolean = false, onFinished: () -> Unit = { }
    ) {
        if (!mapInitialized) {
            preInitializationTaskList.add { setStyle(newStyleData, force, onFinished) }
            return
        }
        val newDisplayLabels = displayLabels(context)
        if (newDisplayLabels != displayLabels) {
            displayLabels = newDisplayLabels
        } else if (!force && newStyleData.uid == styleDataUid) {
            onFinished()
            return
        }
        styleDataUid = newStyleData.uid
        val styleBuilder = Style.Builder()
//        if (styleData.isLocal) {
//            styleBuilder.fromJson(DataKit.loadStyleJson(context, styleData))
//        } else {
//            styleBuilder.fromUri(styleData.uri)
//        }
        styleBuilder.fromUri(newStyleData.uri)
        map.setStyle(styleBuilder) { style ->
            if (!displayLabels) removeLabels(style)
            onFinished()
        }
    }

    fun takeSnapshot(
        width: Int, height: Int, onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {

        if (!mapInitialized) {
            preInitializationTaskList.add { takeSnapshot(width, height, onSnapshotReady, onError) }
            return
        }

        map.getStyle { style: Style ->
            if (!displayLabels) removeLabels(style)
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

    fun getPreviewImage(
        target: StyleData, onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        snapshotKit.refresh()
        val size = Point()
        context.windowManager.defaultDisplay.getSize(size)
        snapshotKit.takeSnapshot(
            size.x, size.y, target, DefaultValue.Map.CAMERA_POSITION, onSnapshotReady, onError
        )
    }

    fun deactivateMap() { map.uiSettings.setAllGesturesEnabled(false) }
    fun activateMap() { map.uiSettings.setAllGesturesEnabled(true) }

    companion object {

        fun getToken(context: Context): String {
            var token: String =
                if (
                    context.defaultSharedPreferences
                        .getBoolean(context.getString(R.string.pref_mapbox_use_default_token), true)
                ) {
                    context.getString(R.string.mapbox_default_access_token)
                } else {
                    context.defaultSharedPreferences
                        .getString(
                            context.getString(R.string.pref_mapbox_token),
                            context.getString(R.string.pref_mapbox_token_default)
                        )
                        ?: context.getString(R.string.pref_mapbox_token_default)
                }
            if (token.isBlank()) token = context.getString(R.string.pref_mapbox_token_default)
            return token
        }

        fun setToken(context: Context) {
            Mapbox.setAccessToken(getToken(context))
        }

        fun useDefaultToken(context: Context) = context.defaultSharedPreferences
            .getBoolean(context.getString(R.string.pref_mapbox_use_default_token), true)

        fun displayLabels(context: Context) = context.defaultSharedPreferences
            .getBoolean(context.getString(R.string.pref_display_label), true)

        fun initStyleIndexList(context: Context): ArrayList<StyleData> {
            val list: ArrayList<StyleData> = arrayListOf()
            val mapboxStyleNameList = context.resources.getStringArray(R.array.style_mapbox_name)
            val authorMapbox = context.getString(R.string.map_style_default_author)
            list.add(StyleData(
                name = mapboxStyleNameList[0], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.MAPBOX_STREETS
            ))
            list.add(StyleData(
                name = mapboxStyleNameList[1], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.LIGHT
            ))
            list.add(StyleData(
                name = mapboxStyleNameList[2], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.DARK
            ))
            list.add(StyleData(
                name = mapboxStyleNameList[3], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.OUTDOORS
            ))
            list.add(StyleData(
                name = mapboxStyleNameList[4], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.SATELLITE
            ))
            list.add(StyleData(
                name = mapboxStyleNameList[5], author = authorMapbox,
                type = StyleData.Type.MAPBOX, uri = Style.SATELLITE_STREETS
            ))
            list.addAll(generateLuckaStyleDataList(context))
            DataKit.saveStyleIndexList(context, list)
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_data_version), DataKit.CURRENT_DATA_VERSION)
            }
            return list
        }

        fun generateLuckaStyleDataList(context: Context): ArrayList<StyleData> {
            val list = arrayListOf<StyleData>()
            val luckaStyleNameList = context.resources.getStringArray(R.array.style_lucka_name)
            val luckaStyleAuthorList = context.resources.getStringArray(R.array.style_lucka_author)
            val luckaStyleUrlList = context.resources.getStringArray(R.array.style_lucka_url)
            val count = luckaStyleNameList.size
            for (i in 0 until count) {
                list.add(
                    StyleData(
                        name = luckaStyleNameList[i], author = luckaStyleAuthorList[i],
                        type = StyleData.Type.LUCKA, uri = luckaStyleUrlList[i]
                    )
                )
            }
            return list
        }

        fun getSelectedStyleData(context: Context): StyleData {

            val mapStyleIndexList = DataKit.loadStyleIndexList(context)

            val selectedStyleUid = context.defaultSharedPreferences
                .getString(
                    context.getString(R.string.pref_style_manager_selected_uid),
                    mapStyleIndexList[0].uid
                )
            mapStyleIndexList.forEach {
                if (it.uid == selectedStyleUid) return it
            }
            // Not found
            context.defaultSharedPreferences.edit {
                putString(
                    context.getString(R.string.pref_style_manager_selected_uid),
                    mapStyleIndexList[0].uid
                )
            }
            return mapStyleIndexList[0]
        }

        fun getRandomStyleData(context: Context): StyleData {

            val mapStyleIndexList = DataKit.loadStyleIndexList(context)
            val selectedStyleId = context.defaultSharedPreferences
                .getString(
                    context.getString(R.string.pref_style_manager_selected_uid),
                    mapStyleIndexList[0].uid
                )
            val onRandomIndexList = arrayListOf<Int>()
            for (i in 0 until mapStyleIndexList.size) {
                val style = mapStyleIndexList[i]
                if (style.inRandom && style.uid != selectedStyleId) onRandomIndexList.add(i)
            }
            if (onRandomIndexList.isEmpty()) {
                return getSelectedStyleData(context)
            }
            if (onRandomIndexList.size == 1) {
                val style = mapStyleIndexList[onRandomIndexList[0]]
                context.defaultSharedPreferences.edit {
                    putString(
                        context.getString(R.string.pref_style_manager_selected_uid), style.uid
                    )
                }
                return style
            }
            val randomIndex = onRandomIndexList[
                    Random(Date().time).nextInt(0, onRandomIndexList.size - 1)
            ]
            val style = mapStyleIndexList[randomIndex]
            context.defaultSharedPreferences.edit {
                putString(context.getString(R.string.pref_style_manager_selected_uid), style.uid)
            }
            return style
        }

        fun removeLabels(style: Style) {
            style.layers.forEach { layer ->
                if (layer is SymbolLayer && !layer.textField.isNull) { style.removeLayer(layer) }
            }
        }

//        fun removeLabels(context: Context, json: String): String {
//
//            // { key: value } -> object
//            // Parse to JsonObject
//            val jsonObject = JsonParser().parse(json).asJsonObject
//            // Get value of "layers", it should be a list
//            val layers = jsonObject.getAsJsonArray("layers")
//            // Scan the layer list and remove label layers
//            for (i in layers.size() - 1 downTo 0) {
//                // Every object in the list is considered as value
//                val element = layers[i]
//                // Convert the value to object
//                val elementJsonObject = element.asJsonObject
//                val type = elementJsonObject["type"]
//                val layout = elementJsonObject["layout"]
//                if (layout != null) {
//                    val textField = layout.asJsonObject["text-field"]
//                    if (type.asString == "symbol" && textField != null) {
//                        layers.remove(i)
//                    }
//                }
//            }
//            return jsonObject.toString()
//        }
    }
}