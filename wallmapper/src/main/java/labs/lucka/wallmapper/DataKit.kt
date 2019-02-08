package labs.lucka.wallmapper

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import org.jetbrains.anko.defaultSharedPreferences
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DataKit {
    companion object {

        const val CURRENT_DATA_VERSION: Int = 1

        fun readFile(context: Context, uri: Uri?): String {
            var result = ""
            if (uri == null) return  result
            val inputStream = context.contentResolver.openInputStream(uri)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                result += line + "\n"
                line = bufferedReader.readLine()
            }
            return result
        }

        private fun getStyleIndexListFile(context: Context): File {
            return File(
                context.filesDir,
                context.getString(
                    if (
                        context.defaultSharedPreferences.getBoolean(
                            context.getString(R.string.pref_mapbox_use_default_token), true
                        )
                    ) {
                        R.string.file_style_index_list_default_token
                    } else {
                        R.string.file_style_index_list_own_token
                    }
                )
            )
        }

        fun loadStyleIndexList(context: Context): ArrayList<MapStyleIndex> {

            val file = getStyleIndexListFile(context)
            var mapStyleIndexList: Array<MapStyleIndex> = arrayOf()

            if (file.exists()) {
                try {
                    mapStyleIndexList = Gson().fromJson(file.readText(), Array<MapStyleIndex>::class.java)
                } catch (error: Exception) {
                    return arrayListOf()
                }
            }
            return mapStyleIndexList.toCollection(ArrayList())
        }

        fun saveStyleIndexList(context: Context, list: ArrayList<MapStyleIndex>) {

            val file = getStyleIndexListFile(context)

            try {
                file.writeText(Gson().toJson(list.toArray()))
            } catch (error: Exception) {
                throw error
            }
        }

        fun loadStyleJson(context: Context, path: String): String {
            val file = File(context.filesDir, path)
            return if (file.exists()) file.readText() else ""
        }

        fun saveStyleJson(context: Context, json: String, path: String) {
            File(context.filesDir, path).writeText(json)
        }

        fun deleteStyleJson(context: Context, path: String) {
            val file = File(context.filesDir, path)
            file.delete()
        }

    }
}