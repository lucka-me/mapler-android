package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import org.jetbrains.anko.defaultSharedPreferences
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import com.mapbox.mapboxsdk.camera.CameraPosition
import java.io.File


class SnapshotKit(private val context: Context) {

    private val pixelRatio: Float = context.resources.displayMetrics.density
    private val snapshotter = MapSnapshotter(
        context,  MapSnapshotter.Options(100, 100).withLogo(false).withPixelRatio(pixelRatio)
    )
    private var isShotting = false

    private fun takeSnapshot(
        width: Int, height: Int, setCallback: () -> Unit,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        if (isShotting) { return }
        snapshotter.setSize((width / pixelRatio).toInt(), (height / pixelRatio).toInt())
        setCallback()
        snapshotter.cancel()
        isShotting = true
        snapshotter.start(
            { mapSnapshot: MapSnapshot ->
                isShotting = false
                onSnapshotReady(mapSnapshot.bitmap)
            }, { error: String? ->
                isShotting = false
                onError(error)
            }
        )
    }

    fun takeSnapshotJson(
        width: Int, height: Int, styleJson: String, region: LatLngBounds,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, {
                snapshotter.setRegion(region)
                snapshotter.setStyleJson(handleLabels(context, styleJson))
            }, onSnapshotReady, onError
        )
    }

    fun takeSnapshotJson(
        width: Int, height: Int, styleJson: String, cameraPosition: CameraPosition,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, {
                snapshotter.setCameraPosition(cameraPosition)
                snapshotter.setStyleJson(handleLabels(context, styleJson))
            }, onSnapshotReady, onError
        )
    }

    fun takeSnapshotUrl(
        width: Int, height: Int, styleUrl: String, region: LatLngBounds,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, {
                snapshotter.setRegion(region)
                snapshotter.setStyleUrl(styleUrl)
            }, onSnapshotReady, onError
        )
    }

    fun takeSnapshotUrl(
        width: Int, height: Int, styleUrl: String, cameraPosition: CameraPosition,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, {
                snapshotter.setCameraPosition(cameraPosition)
                snapshotter.setStyleUrl(styleUrl)
            }, onSnapshotReady, onError
        )
    }

    fun onPause() {
        snapshotter.cancel()
        isShotting = false
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
            // Get value of "layer", it should be a list
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

        /**
         * Convert image file to content URI, which is required by WallpaperManager.getCropAndSetWallpaperIntent().
         *
         * @param [context] The context.
         * @param [imageFile] Target file.
         *
         * @return The content URI of [imageFile].
         *
         * @author lucka-me
         * @since 0.1
         * @see <a href="https://stackoverflow.com/a/13338647/10276204">Convert file uri to content uri | Stack Overflow</a>
         */
        fun getImageContentUri(context: Context, imageFile: File): Uri? {
            val filePath = imageFile.absolutePath
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                MediaStore.Images.Media.DATA + "=? ",
                arrayOf(filePath), null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getInt(
                    cursor
                        .getColumnIndex(MediaStore.MediaColumns._ID)
                )
                val baseUri = Uri.parse("content://media/external/images/media")
                cursor.close()
                return Uri.withAppendedPath(baseUri, "" + id)
            } else {
                cursor?.close()
                return if (imageFile.exists()) {
                    val values = ContentValues()
                    values.put(MediaStore.Images.Media.DATA, filePath)
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )
                } else {
                    null
                }
            }
        }

    }
}