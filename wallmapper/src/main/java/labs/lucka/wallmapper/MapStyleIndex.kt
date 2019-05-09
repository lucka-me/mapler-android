package labs.lucka.wallmapper

import android.content.Context
import androidx.annotation.Keep
import androidx.core.content.edit
import org.jetbrains.anko.defaultSharedPreferences

@Keep
data class MapStyleIndex(
    var id: Int,
    var name: String,
    var author: String,
    var fileId: String = DataKit.getUUID(),
    var type: StyleType = StyleType.ONLINE,
    var url: String = "",
    var inRandom: Boolean = true
) {

    @Keep
    enum class StyleType {
        ONLINE, LOCAL, DOWNLOADED, MAPBOX, LUCKA
    }

    @Keep
    data class CompatV3(
        var name: String, var author: String, var path: String, var type: StyleType,
        var imagePath: String?, var inRandom: Boolean?
    ) {
        fun toMapStyleIndex(context: Context): MapStyleIndex {
            var url = ""
            val fileId: String
            if (type == StyleType.LOCAL || type == StyleType.DOWNLOADED) {
                fileId = path
            } else {
                fileId = DataKit.getUUID()
                url = path
            }
            return MapStyleIndex(
                id = generateNewId(context), name = name, author = author,
                fileId = fileId, type = type, url = url,
                inRandom = inRandom ?: true
            )
        }

    }

    val imagePath: String
        get() = fileId + PNG_SUFFIX

    val jsonPath: String
        get() = fileId + JSON_SUFFIX

    val isLocal: Boolean
        get() = (type == StyleType.LOCAL || type == StyleType.DOWNLOADED)

    override fun equals(other: Any?) =
        if (other is MapStyleIndex) {
            id == other.id
                    && name == other.name
                    && author == other.author
                    && fileId == other.fileId
        } else {
            false
        }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + fileId.hashCode()
        return result
    }

    companion object {

        const val PNG_SUFFIX: String = ".png"
        const val JSON_SUFFIX: String = ".json"

        fun generateNewId(context: Context): Int {
            val key = context.getString(R.string.pref_data_last_style_id)
            val id = context.defaultSharedPreferences.getInt(key, 0)
            context.defaultSharedPreferences.edit { putInt(key, id + 1) }
            return id
        }

        fun clearIdToZero(context: Context) {
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_data_last_style_id), 0)
            }
        }
    }
}