package labs.lucka.wallmapper

import androidx.annotation.Keep

@Keep
data class StyleData(
    var name: String,
    var author: String,
    var id: String = DataKit.getUUID(),
    var type: StyleType = StyleType.ONLINE,
    var uri: String = "",
    var inRandom: Boolean = true,
    var themeType: ThemeType = ThemeType.DEFAULT
) {

    @Keep
    enum class StyleType {
        ONLINE, LOCAL, DOWNLOADED, MAPBOX, LUCKA
    }

    @Keep
    enum class ThemeType {
        DEFAULT, DAY, NIGHT
    }

    val imagePath: String
        get() = id + PNG_SUFFIX

    val jsonPath: String
        get() = id + JSON_SUFFIX

    val isLocal: Boolean
        get() = (type == StyleType.LOCAL || type == StyleType.DOWNLOADED)

    override fun equals(other: Any?) =
        if (other is StyleData) {
            (id == other.id) && (name == other.name) && (author == other.author)
        } else {
            false
        }

    override fun hashCode(): Int {
        var result =  name.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {

        const val PNG_SUFFIX: String = ".png"
        const val JSON_SUFFIX: String = ".json"

    }
}