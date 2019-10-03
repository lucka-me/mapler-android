package labs.lucka.wallmapper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jetbrains.anko.defaultSharedPreferences
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

class DataKit {
    companion object {

        const val CURRENT_DATA_VERSION: Int = 1

//        fun readFile(context: Context, uri: Uri?): String {
//            var result = ""
//            if (uri == null) return  result
//            val inputStream = context.contentResolver.openInputStream(uri)
//            if (inputStream != null) {
//                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
//                var line: String? = bufferedReader.readLine()
//                while (line != null) {
//                    result += line + "\n"
//                    line = bufferedReader.readLine()
//                }
//            }
//            return result
//        }

        private fun getStyleDataListFile(context: Context) =
            File(context.filesDir, context.getString(R.string.file_style_data_list))

        fun loadStyleIndexList(context: Context): ArrayList<StyleData> {

            val file = getStyleDataListFile(context)
            var list: ArrayList<StyleData>

            list = if (file.exists()) {
                val type = Array<StyleData>::class.java
                try {
                    GsonBuilder()
                        .registerTypeAdapter(type, StyleData.ArrayReader())
                        .create()
                        .fromJson(FileReader(file), type)
                        .toCollection(ArrayList())
                } catch (error: Exception) {
                    MapKit.initStyleIndexList(context)
                }
            } else {
                MapKit.initStyleIndexList(context)
            }

            if (list.isEmpty()) list = MapKit.initStyleIndexList(context)

            checkStyleDataVersion(context, list)

            return list
        }

        private fun checkStyleDataVersion(context: Context, list: ArrayList<StyleData>) {

            if (context.defaultSharedPreferences
                    .getInt(
                        context.getString(R.string.pref_data_version), DefaultValue.Data.VERSION
                    ) ==
                CURRENT_DATA_VERSION
            ) return

            var startInsertPosition = list.size - 1
            for (i in (list.size - 1) downTo 0) {
                if (list[i].type == StyleData.Type.LUCKA) {
                    list.removeAt(i)
                    startInsertPosition = i
                }
            }
            list.addAll(startInsertPosition, MapKit.generateLuckaStyleDataList(context))

            saveStyleIndexList(context, list)
            context.defaultSharedPreferences.edit {
                putInt(context.getString(R.string.pref_data_version), CURRENT_DATA_VERSION)
            }
        }

        fun saveStyleIndexList(context: Context, list: ArrayList<StyleData>) {

            val file = getStyleDataListFile(context)

            file.writeText(Gson().toJson(list))
        }

        fun deleteStyleFiles(context: Context, style: StyleData) {
            deleteStylePreviewImage(context, style)
            //deleteStyleJson(context, style)
        }

        fun loadStylePreviewImage(context: Context, style: StyleData): Bitmap? {
            val file = File(context.filesDir, style.imagePath)
            return if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }

        fun saveStylePreviewImage(context: Context, style: StyleData, image: Bitmap) {
            val file = File(context.filesDir, style.imagePath)
            val fos = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        }

        private fun deleteStylePreviewImage(context: Context, style: StyleData) {
            val file = File(context.filesDir, style.imagePath)
            if (file.exists()) file.delete()
        }

//        fun loadStyleJson(context: Context, style: StyleData): String {
//            val file = File(context.filesDir, style.jsonPath)
//            return if (file.exists()) file.readText() else ""
//        }
//
//        fun saveStyleJson(context: Context, json: String, style: StyleData) {
//            File(context.filesDir, style.jsonPath).writeText(json)
//        }
//
//        private fun deleteStyleJson(context: Context, style: StyleData) {
//            val file = File(context.filesDir, style.jsonPath)
//            file.delete()
//        }

        fun saveImage(context: Context, image: Bitmap, onSaved: (File) -> Unit, onError: (Exception) -> Unit) {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + File.separator + context.getString(R.string.path_save_folder))
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory.absolutePath, UUID.randomUUID().toString() + StyleData.PNG_SUFFIX)
            try {
                val fos = FileOutputStream(file)
                image.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                onSaved(file)
            } catch (error: Exception) {
                onError(error)
            }
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
         * @see <a href="https://stackoverflow.com/a/13338647">Convert file uri to content uri | Stack Overflow</a>
         */
        fun getImageContentUri(context: Context, imageFile: File): Uri? {
            val filePath = imageFile.absolutePath
            val externalContenUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                externalContenUri,
                arrayOf(MediaStore.Images.Media._ID),
                MediaStore.Images.Media.DATA + "=? ",
                arrayOf(filePath), null
            )

            return if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                cursor.close()
                Uri.withAppendedPath(externalContenUri, id.toString())
            } else {
                cursor?.close()
                if (imageFile.exists()) {
                    val values = ContentValues()
                    values.put(MediaStore.Images.Media.DATA, filePath)
                    context.contentResolver.insert(externalContenUri, values)
                } else {
                    null
                }
            }
        }

        /**
         * Get UUID encoded with Base64
         *
         * @return The UUID encoded with Base64
         *
         * @author lucka-me
         * @since 0.1.7
         * @see <a href="https://stackoverflow.com/a/15013205">Storing UUID as base64 String | Stack Overflow</a>
         */
        fun getUUID(): String {
            val uuid = UUID.randomUUID()
            val byteBuffer = ByteBuffer.wrap(ByteArray(16))
            byteBuffer.putLong(uuid.mostSignificantBits)
            byteBuffer.putLong(uuid.leastSignificantBits)
            return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT)
                .replace("=", "").replace("/", "-")
        }

    }
}