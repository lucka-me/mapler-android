package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import org.jetbrains.anko.defaultSharedPreferences
import com.mapbox.mapboxsdk.camera.CameraPosition

class SnapshotKit(private val context: Context) {

    private var pixelRatio: Float = context.resources.displayMetrics.density
    private var snapshotter = MapSnapshotter(
        context,  MapSnapshotter.Options(100, 100).withLogo(false).withPixelRatio(pixelRatio)
    )

    fun takeSnapshot(
        width: Int, height: Int, styleIndex: MapStyleIndex, cameraPosition: CameraPosition,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, cameraPosition, {

                if (styleIndex.isLocal) {
                    snapshotter.setStyleJson(handleLabels(context, DataKit.loadStyleJson(context, styleIndex)))
                } else {
                    snapshotter.setStyleUrl(styleIndex.url)
                }

            }, onSnapshotReady, onError
        )
    }

    fun takeSnapshot(
        width: Int, height: Int, styleJson: String, cameraPosition: CameraPosition,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, cameraPosition, {
                snapshotter.setStyleJson(styleJson)
            }, onSnapshotReady, onError
        )
    }

    private fun takeSnapshot(
        width: Int, height: Int, cameraPosition: CameraPosition,
        setStyleCallback: () -> Unit, onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        snapshotter.setSize((width / pixelRatio).toInt(), (height / pixelRatio).toInt())
        snapshotter.setCameraPosition(cameraPosition)
        setStyleCallback()
        snapshotter.start({ mapSnapshot: MapSnapshot -> onSnapshotReady(mapSnapshot.bitmap) }, onError)
    }

    fun onPause() {
        snapshotter.cancel()
    }

    /**
     * Refresh the [snapshotter] so that the next snapshot can be taken immediately.
     *
     * @author lucka-me
     * @since 0.1.1
     */
    fun refresh() {
        snapshotter.cancel()
        pixelRatio = context.resources.displayMetrics.density
        snapshotter = MapSnapshotter(
            context,
            MapSnapshotter.Options(100, 100).withLogo(false).withPixelRatio(pixelRatio)
        )
    }

    companion object {

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