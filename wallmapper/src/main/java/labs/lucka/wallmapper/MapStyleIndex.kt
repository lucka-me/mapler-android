package labs.lucka.wallmapper

import androidx.annotation.Keep

@Keep
data class MapStyleIndex(
    var name: String,
    var author: String,
    var path: String,
    var type: StyleType = StyleType.ONLINE,
    var imagePath: String = "",
    var inRandom: Boolean = true
) {

    @Keep
    enum class StyleType {
        ONLINE, LOCAL, CUSTOMIZED, MAPBOX, LUCKA
    }

    @Keep
    data class Compat(
        var name: String, var author: String, var path: String, var type: StyleType,
        var imagePath: String?, var inRandom: Boolean?
    ) {
        fun toMapStyleIndex() =
            MapStyleIndex(name, author, path, type, imagePath ?: "", inRandom ?: true)
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

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + inRandom.hashCode()
        return result
    }
}