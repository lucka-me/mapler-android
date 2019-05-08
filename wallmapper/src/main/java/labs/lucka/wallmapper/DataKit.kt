package labs.lucka.wallmapper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import org.jetbrains.anko.defaultSharedPreferences
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

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

        private fun getStyleIndexListFile(context: Context) =
            File(context.filesDir, context.getString(R.string.file_style_index_list))

        private fun renameFile(context: Context, oldFilename: String, newFilename: String) {
            val oldFile = File(context.filesDir, oldFilename)
            val newFile = File(context.filesDir, newFilename)
            oldFile.renameTo(newFile)
        }

        private fun upgradeDataStructure(context: Context) {
            val currentVersion =
                context.defaultSharedPreferences.getInt(
                    context.getString(R.string.pref_data_structure_version), DefaultValue.Data.STRUCTURE_VERSION_DEFAULT
                )
            if (currentVersion == DefaultValue.Data.STRUCTURE_VERSION_LATEST) return

            MapStyleIndex.clearIdToZero(context)

            if (currentVersion <= DefaultValue.Data.STRUCTURE_VERSION_3) {

                val mapStyleIndexList: ArrayList<MapStyleIndex> = arrayListOf()

                val oldDataHandler = fun (file: File) {
                    if (file.exists()) {
                        try {
                            val mapStyleIndexCompatList =
                                Gson().fromJson(file.readText(), Array<MapStyleIndex.CompatV3>::class.java)
                            mapStyleIndexCompatList.forEach { item ->
                                val newStyle = item.toMapStyleIndex(context)
                                val imagePath = item.imagePath
                                if (!imagePath.isNullOrEmpty()) {
                                    renameFile(
                                        context, imagePath, newStyle.imagePath
                                    )
                                }
                                if (newStyle.isLocal) {
                                    renameFile(
                                        context, item.path, newStyle.jsonPath
                                    )
                                }
                                mapStyleIndexList.add(newStyle)
                            }
                        } catch (error: Exception) {
                            return
                        }
                    }
                }

                oldDataHandler(File(context.filesDir, context.getString(R.string.file_style_index_list)))
                oldDataHandler(File(context.filesDir, context.getString(R.string.file_style_index_list_own_token_v3)))

                saveStyleIndexList(context, mapStyleIndexList)

                context.defaultSharedPreferences.edit {
                    putInt(context.getString(
                        R.string.pref_data_structure_version), DefaultValue.Data.STRUCTURE_VERSION_LATEST
                    )
                }

                return
            }
        }

        fun loadFullStyleIndexList(context: Context): ArrayList<MapStyleIndex> {

            upgradeDataStructure(context)

            val file = getStyleIndexListFile(context)
            var list: Array<MapStyleIndex> = arrayOf()

            if (file.exists()) {
                try {
                    list = Gson().fromJson(file.readText(), Array<MapStyleIndex>::class.java)
                } catch (error: Exception) {
                    return arrayListOf()
                }
            }

            return list.toCollection(ArrayList())
        }

        fun loadStyleIndexList(context: Context): ArrayList<MapStyleIndex> {

            val list = loadFullStyleIndexList(context)
            if (MapKit.useDefaultToken(context)) {
                list.removeAll { it.type == MapStyleIndex.StyleType.ONLINE }
            } else {
                list.removeAll { it.type == MapStyleIndex.StyleType.LUCKA }
            }
            return list
        }

        fun saveStyleIndexList(context: Context, list: ArrayList<MapStyleIndex>) {

            val file = getStyleIndexListFile(context)

            try {
                file.writeText(Gson().toJson(list.toArray()))
            } catch (error: Exception) {
                throw error
            }
        }

        fun deleteStyleFiles(context: Context, style: MapStyleIndex) {
            deleteStylePreviewImage(context, style)
            deleteStyleJson(context, style)
        }

        fun loadStylePreviewImage(context: Context, style: MapStyleIndex): Bitmap? {
            val file = File(context.filesDir, style.imagePath)
            return if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }

        fun saveStylePreviewImage(context: Context, style: MapStyleIndex, image: Bitmap) {
            val file = File(context.filesDir, style.imagePath)
            val fos = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        }

        private fun deleteStylePreviewImage(context: Context, style: MapStyleIndex) {
            val file = File(context.filesDir, style.imagePath)
            if (file.exists()) file.delete()
        }

        fun loadStyleJson(context: Context, style: MapStyleIndex): String {
            val file = File(context.filesDir, style.jsonPath)
            return if (file.exists()) file.readText() else ""
        }

        fun saveStyleJson(context: Context, json: String, style: MapStyleIndex) {
            File(context.filesDir, style.jsonPath).writeText(json)
        }

        private fun deleteStyleJson(context: Context, style: MapStyleIndex) {
            val file = File(context.filesDir, style.jsonPath)
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
         * @see <a href="https://stackoverflow.com/a/13338647">Convert file uri to content uri | Stack Overflow</a>
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
                val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                val baseUri = "content://media/external/images/media".toUri()
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