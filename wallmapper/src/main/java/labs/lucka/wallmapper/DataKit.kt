package labs.lucka.wallmapper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.google.gson.Gson
import org.jetbrains.anko.defaultSharedPreferences
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*

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

        fun loadStylePreviewImage(context: Context, path: String): Bitmap? {
            val file = File(context.filesDir, path)
            return if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }

        fun saveStylePreviewImage(context: Context, style: MapStyleIndex, image: Bitmap) {
            style.imagePath =
                if (style.type == MapStyleIndex.StyleType.LOCAL || style.type == MapStyleIndex.StyleType.CUSTOMIZED) {
                    style.path
                } else {
                    UUID.randomUUID().toString()
                } + ".png"
            val file = File(context.filesDir, style.imagePath)
            val fos = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        }

        fun deleteStylePreviewImage(context: Context, style: MapStyleIndex) {
            if (style.imagePath == null) return
            val file = File(context.filesDir, style.imagePath)
            style.imagePath = null
            file.delete()
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