package labs.lucka.wallmapper

import androidx.annotation.Keep

@Keep
data class MapStyleIndex(
    var name: String,
    var author: String,
    var path: String,
    var type: StyleType = StyleType.ONLINE,
    var imagePath: String? = null
) {

    @Keep
    enum class StyleType {
        ONLINE, LOCAL, CUSTOMIZED, MAPBOX, LUCKA
    }

    override fun equals(other: Any?): Boolean {
        return if (other is MapStyleIndex) {
            name == other.name
                    && author == other.author
                    && path == other.path
                    && type == other.type
                    && imagePath == other.imagePath
        } else {
            false
        }
    }

}