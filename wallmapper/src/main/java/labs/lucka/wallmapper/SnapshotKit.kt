package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Bitmap
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.mapboxsdk.camera.CameraPosition

class SnapshotKit(private val context: Context) {

    private var pixelRatio: Float = context.resources.displayMetrics.density
    private var snapshotter = MapSnapshotter(
        context,
        MapSnapshotter.Options(100, 100)
            .withLogo(false)
            .withPixelRatio(pixelRatio)
    )

    fun takeSnapshot(
        width: Int, height: Int, styleData: StyleData, cameraPosition: CameraPosition,
        onSnapshotReady: (Bitmap) -> Unit, onError: (String?) -> Unit
    ) {
        takeSnapshot(
            width, height, cameraPosition, {

//                if (styleData.isLocal) {
//                    snapshotter.setStyleJson(
//                        MapKit.removeLabels(context, DataKit.loadStyleJson(context, styleData))
//                    )
//                } else {
//                    snapshotter.setStyleUrl(styleData.uri)
//                }
                snapshotter.setStyleUrl(styleData.uri)

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
        snapshotter.start(
            { mapSnapshot: MapSnapshot -> onSnapshotReady(mapSnapshot.bitmap) }, onError
        )
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
        snapshotter =
            MapSnapshotter(
                context,
                MapSnapshotter.Options(100, 100)
                    .withLogo(false)
                    .withPixelRatio(pixelRatio)
            )
    }
}