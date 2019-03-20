package labs.lucka.wallmapper

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

/**
 * Display dialogs in a bit better way.
 *
 * ## Public Methods
 * - [showDialog]
 * - [showSimpleAlert]
 * - [showSaveImageDialog]
 * - [showAddNewStyleTypeSelectDialog]
 *
 * @author lucka-me
 * @since 0.1
 */
class DialogKit {

    companion object {

        /**
         * Display a dialog
         *
         * @param [context] The context
         * @param [titleId] Resource ID for Title
         * @param [message] String for message
         * @param [positiveButtonTextId] Resource ID for PositiveButton text, CONFIRM for default
         * @param [positiveButtonListener] OnClickListener for PositiveButton, nullable
         * @param [negativeButtonTextId] Resource ID for NegativeButton text, nullable
         * @param [negativeButtonListener] OnClickListener for NegativeButton, nullable
         * @param [icon] Icon for dialog, nullable
         * @param [cancelable] Could dialog canceled by tapping outside or back button, nullable
         *
         * @see <a href="https://www.jianshu.com/p/6bd7dd1cd491">使用着色器修改 Drawable 颜色 | 简书</a>
         *
         * @author lucka-me
         * @since 0.1
         */
        fun showDialog(
            context: Context, titleId: Int, message: String?,
            positiveButtonTextId: Int = R.string.button_confirm,
            positiveButtonListener: ((DialogInterface, Int) -> (Unit))? = null,
            negativeButtonTextId: Int? = null,
            negativeButtonListener: ((DialogInterface, Int) -> (Unit))? = null,
            icon: Drawable? = null,
            cancelable: Boolean? = null
        ) {

            val builder = AlertDialog.Builder(context)
                .setTitle(titleId)
                .setIcon(icon)
                .setMessage(message)
                .setPositiveButton(positiveButtonTextId, positiveButtonListener)

            if (negativeButtonTextId != null)
                builder.setNegativeButton(negativeButtonTextId, negativeButtonListener)
            if (cancelable != null) builder.setCancelable(cancelable)

            builder.show()

        }

        /**
         * Display a dialog
         *
         * @param [context] The context
         * @param [titleId] Resource ID for Title
         * @param [messageId] Resource ID for message
         * @param [positiveButtonTextId] Resource ID for PositiveButton text, CONFIRM for default
         * @param [positiveButtonListener] OnClickListener for PositiveButton, nullable
         * @param [negativeButtonTextId] Resource ID for NegativeButton text, nullable
         * @param [negativeButtonListener] OnClickListener for NegativeButton, nullable
         * @param [icon] Icon for dialog, nullable
         * @param [cancelable] Could dialog canceled by tapping outside or back button, nullable
         *
         * @author lucka-me
         * @since 0.1
         */
        fun showDialog(
            context: Context, titleId: Int, messageId: Int,
            positiveButtonTextId: Int = R.string.button_confirm,
            positiveButtonListener: ((DialogInterface, Int) -> (Unit))? = null,
            negativeButtonTextId: Int? = null,
            negativeButtonListener: ((DialogInterface, Int) -> (Unit))? = null,
            icon: Drawable? = null,
            cancelable: Boolean? = null
        ) {

            showDialog(
                context, titleId, context.getString(messageId), positiveButtonTextId,
                positiveButtonListener,
                negativeButtonTextId, negativeButtonListener,
                icon,
                cancelable
            )

        }

        /**
         * Display a simple alert with a CONFIRM button and un-cancelable
         *
         * @param [context] The context
         * @param [message] String for message to display
         *
         * @author lucka-me
         * @since 0.1
         */
        fun showSimpleAlert(context: Context, message: String?) {
            showDialog(context, R.string.dialog_title_error, message, cancelable = false)
        }

        /**
         * Display a simple alert with a CONFIRM button and un-cancelable
         *
         * @param [context] The context
         * @param [messageId] Resource ID for message to display
         *
         * @author lucka-me
         * @since 0.1
         */
        fun showSimpleAlert(context: Context, messageId: Int) {
            showSimpleAlert(context, context.getString(messageId))
        }

        fun showSaveImageDialog(context: Context, image: Bitmap, onSaveButtonClick: () -> Unit) {
            val dialogLayout = View.inflate(context, R.layout.dialog_image, null)
            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_result)
                .setView(dialogLayout)
                .setPositiveButton(R.string.button_save) { dialog, _ ->
                    dialog.dismiss()
                    onSaveButtonClick()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
            dialogLayout.findViewById<ImageView>(R.id.imageView).setImageBitmap(image)
        }

        fun showAddNewStyleTypeSelectDialog(context: Context, onSelected: (type: MapStyleIndex.StyleType) -> Unit) {
            val itemList: ArrayList<CharSequence> = arrayListOf()
            itemList.add(context.getString(R.string.dialog_add_style_from_json))
            if (
                !context.defaultSharedPreferences
                    .getBoolean(context.getString(R.string.pref_mapbox_use_default_token), true)
            ) {
                itemList.add(context.getString(R.string.dialog_add_style_from_url))
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_add_style_from)
                .setItems(itemList.toTypedArray()) { _, which ->
                    val type = when (which) {
                        0 -> MapStyleIndex.StyleType.LOCAL
                        1 -> MapStyleIndex.StyleType.ONLINE
                        else -> null
                    } ?: return@setItems
                    onSelected(type)
                }
                .show()
        }

        fun showAddNewStyleFromJsonDialog(context: Context, onAddClick: (newStyleIndex: MapStyleIndex) -> Unit) {
            val layout = View.inflate(context, R.layout.dialog_add_style_json, null)
            val editTextName: EditText = layout.findViewById(R.id.editTextName)
            val editTextAuthor: EditText = layout.findViewById(R.id.editTextAuthor)

            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_add_style_from_url)
                .setView(layout)
                .setPositiveButton(R.string.button_save) { _, _ ->
                    context.defaultSharedPreferences.edit()
                        .putString(context.getString(R.string.pref_style_author_last), editTextAuthor.text.toString())
                        .apply()
                    onAddClick(MapStyleIndex(
                        editTextName.text.toString(),
                        editTextAuthor.text.toString(),
                        UUID.randomUUID().toString(),
                        MapStyleIndex.StyleType.LOCAL
                    ))
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()

            editTextAuthor.setText(context.defaultSharedPreferences.getString(
                context.getString(R.string.pref_style_author_last),
                context.getString(R.string.pref_style_author_last_default)
            ))
        }

        fun showAddNewStyleFromUrlDialog(context: Context, onSaveClick: (MapStyleIndex) -> Unit) {
            val layout = View.inflate(context, R.layout.dialog_add_style_url, null)
            val editTextUrl: EditText = layout.findViewById(R.id.editTextUrl)
            val editTextName: EditText = layout.findViewById(R.id.editTextName)
            val editTextAuthor: EditText = layout.findViewById(R.id.editTextAuthor)

            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_add_style_from_url)
                .setView(layout)
                .setPositiveButton(R.string.button_save) { _, _ ->
                    context.defaultSharedPreferences.edit()
                        .putString(context.getString(R.string.pref_style_author_last), editTextAuthor.text.toString())
                        .apply()
                    onSaveClick(MapStyleIndex(
                        editTextName.text.toString(), editTextAuthor.text.toString(), editTextUrl.text.toString()
                    ))
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()

            editTextAuthor.setText(context.defaultSharedPreferences.getString(
                context.getString(R.string.pref_style_author_last),
                context.getString(R.string.pref_style_author_last_default)
            ))
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.isEnabled = false
            editTextUrl.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    saveButton.isEnabled = false
                    return@setOnFocusChangeListener
                }
                saveButton.isEnabled = editTextUrl.text.toString().isNotBlank()
            }
        }

        fun showStyleInformationDialog(
            context: Context, style: MapStyleIndex,
            onEditClick: () -> Unit, onShouldLoadPreviewImage: (ImageView) -> Unit
        ) {
            val layout = View.inflate(context, R.layout.dialog_style_info, null)
            val textName: TextView = layout.findViewById(R.id.textName)
            val textAuthor: TextView = layout.findViewById(R.id.textAuthor)
            val imageType: ImageView = layout.findViewById(R.id.imageType)
            val imagePreview: ImageView = layout.findViewById(R.id.imagePreview)
            val switchInRandom: Switch = layout.findViewById(R.id.switchInRandom)

            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_style_information)
                .setView(layout)
                .setPositiveButton(R.string.button_edit) { _, _ -> onEditClick() }
                .setNegativeButton(R.string.button_dismiss) { _, _ ->
                    style.inRandom = switchInRandom.isChecked
                }
                .show()

            textName.text = style.name
            textAuthor.text = String.format(context.getString(R.string.style_author), style.author)
            imageType.setImageResource(
                when (style.type) {

                    MapStyleIndex.StyleType.MAPBOX, MapStyleIndex.StyleType.ONLINE, MapStyleIndex.StyleType.LUCKA -> {
                        R.drawable.ic_online
                    }

                    else -> {
                        R.drawable.ic_local
                    }
            })

            // Disable Edit button if it's default style
            when (style.type) {

                MapStyleIndex.StyleType.MAPBOX, MapStyleIndex.StyleType.LUCKA -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                }

                else -> {
                }

            }
            val imagePath = style.imagePath
            //if (imagePath == null) {
            if (imagePath.isEmpty()) {
                onShouldLoadPreviewImage(imagePreview)
            } else {
                imagePreview.setImageBitmap(DataKit.loadStylePreviewImage(context, imagePath))
            }

            switchInRandom.isChecked = style.inRandom

        }

        fun showEditStyleDialog(context: Context, style: MapStyleIndex, onSaveClick: () -> Unit) {
            val layout = View.inflate(context, R.layout.dialog_edit_style, null)
            val editTextName: EditText = layout.findViewById(R.id.editTextName)
            val editTextAuthor: EditText = layout.findViewById(R.id.editTextAuthor)
            val editTextUrl: EditText = layout.findViewById(R.id.editTextUrl)

            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.dialog_title_edit_style)
                .setView(layout)
                .setPositiveButton(R.string.button_save) { _, _ ->
                    context.defaultSharedPreferences.edit()
                        .putString(context.getString(R.string.pref_style_author_last), editTextAuthor.text.toString())
                        .apply()
                    style.name = editTextName.text.toString()
                    style.author = editTextAuthor.text.toString()
                    if (style.type == MapStyleIndex.StyleType.ONLINE) style.path = editTextUrl.text.toString()
                    onSaveClick()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()

            editTextName.setText(style.name)
            editTextAuthor.setText(style.author)

            // Handle the url
            when (style.type) {

                MapStyleIndex.StyleType.ONLINE -> {
                    editTextUrl.setText(style.path)
                    val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    saveButton.isEnabled = false
                    editTextUrl.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            saveButton.isEnabled = false
                            return@setOnFocusChangeListener
                        }
                        saveButton.isEnabled = editTextUrl.text.toString().isNotBlank()
                    }
                }

                else -> {
                    layout.findViewById<TextView>(R.id.textTitleUrl).visibility = View.GONE
                    editTextUrl.visibility = View.GONE
                }

            }

        }

    }
}