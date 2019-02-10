package labs.lucka.wallmapper

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng

class DefaultValue {

    class Map {
        companion object {

            const val LONGITUDE: Double = 114.174
            const val LATITUDE: Double = 22.308
            const val ZOOM: Double = 15.0
            const val BEARING: Double = 0.0
            const val TILT: Double = 0.0
            val CAMERA_POSITION: CameraPosition = CameraPosition.Builder()
                .target(LatLng(LATITUDE, LONGITUDE))
                .zoom(ZOOM)
                .bearing(BEARING)
                .tilt(TILT)
                .build()

        }
    }

    enum class Request(val code: Int) {
        RequestPermissionWriteExternalStorage(1001),
        RequestPermissionFineLocation(1002),
        ManageMapStyle(2001),
        SetPreference(2002),
        OpenJsonFile(3001)
    }

}