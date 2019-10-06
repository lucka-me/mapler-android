package labs.lucka.mapler

import androidx.annotation.Keep
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@Keep
data class StyleData(
    var name: String,
    var author: String,
    var uid: String = DataKit.getUUID(),
    var type: Type = Type.ONLINE,
    var uri: String = "",
    var inRandom: Boolean = true,
    var themeType: ThemeType = ThemeType.DEFAULT
) {

    @Keep
    enum class Type {
        ONLINE, LOCAL, DOWNLOADED, MAPBOX, EXTRA
    }

    @Keep
    enum class ThemeType {
        DEFAULT, DAY, NIGHT
    }

    @Keep
    class ArrayReader : TypeAdapter<Array<StyleData>>() {

        override fun read(reader: JsonReader?): Array<StyleData> {
            if (reader == null) return arrayOf()

            val list: ArrayList<StyleData> = arrayListOf()
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                val styleData = StyleData("", "")
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name"      -> styleData.name       = reader.nextString()
                        "author"    -> styleData.author     = reader.nextString()
                        "uid"       -> styleData.uid        = reader.nextString()
                        "type"      -> styleData.type       = Type.valueOf(reader.nextString())
                        "uri"       -> styleData.uri        = reader.nextString()
                        "inRandom"  -> styleData.inRandom   = reader.nextBoolean()
                        "themeType" -> styleData.themeType  = ThemeType.valueOf(reader.nextString())
                    }
                }
                list.add(styleData)
                reader.endObject()
            }
            reader.endArray()
            return list.toTypedArray()
        }

        override fun write(writer: JsonWriter?, list: Array<StyleData>?) { }
    }

    val imagePath: String
        get() = uid + PNG_SUFFIX

//    val jsonPath: String
//        get() = uid + JSON_SUFFIX

    val isLocal: Boolean
        get() = (type == Type.LOCAL || type == Type.DOWNLOADED)

    override fun equals(other: Any?) = if (other is StyleData) (uid == other.uid) else false

    override fun hashCode(): Int {
        var result =  name.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + uid.hashCode()
        return result
    }

    companion object {

        const val PNG_SUFFIX: String = ".png"
//        const val JSON_SUFFIX: String = ".json"

    }
}