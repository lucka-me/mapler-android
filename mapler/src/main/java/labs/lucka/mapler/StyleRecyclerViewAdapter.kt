package labs.lucka.mapler

import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.jetbrains.anko.defaultSharedPreferences

class StyleRecyclerViewAdapter(
    private val context: Context,
    private val adapterListener: Listener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSelectedStyleDataChanged(newStyleData: StyleData)
        /**
         * Triggered when card is swiped to delete (left)
         *
         * @param target The style to delete
         * @param position The position of the style
         * @param onConfirmed Triggered when confirm to delete, remove the style and card, returns
         * the new selected style
         *
         * @author lucka-me
         * @since 0.1
         */
        fun onSwipeToDelete(target: StyleData, position: Int, onConfirmed: () -> StyleData)
        fun onSwipeToInfo(target: StyleData, position: Int)
    }

    private class ViewHolderStyleCard(
        itemView: View,
        private val onStyleSelected: (styleData: StyleData, position: Int) -> Unit
    ): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var styleData = StyleData("", "", "")
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_style)
        private val textName: TextView = itemView.findViewById(R.id.text_name)
        private val textAuthor: TextView = itemView.findViewById(R.id.text_author)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) { onStyleSelected(styleData, adapterPosition) }

        fun setFrom(context: Context, styleData: StyleData, isSelected: Boolean = false) {
            this.styleData = styleData
            textName.text = styleData.name
            textAuthor.text = context.getString(R.string.style_author, styleData.author)
            cardView.isChecked = isSelected
        }
    }

    private val styleDataList: ArrayList<StyleData> = arrayListOf()

    private var selectedStyleData: StyleData = StyleData("", "", "")
    private var selectedPosition: Int = -1

    private val onStyleSelected: (StyleData, Int) -> Unit = { styleData, position ->

        if (selectedStyleData.uid != styleData.uid) {
            selectedStyleData = styleData
            val oldPosition = selectedPosition
            selectedPosition = position
            adapterListener.onSelectedStyleDataChanged(styleData)
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }

    }

    private val itemTouchHelper =
        ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.ACTION_STATE_IDLE,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    if (viewHolder !is ViewHolderStyleCard) {
                        return
                    }
                    val styleData = viewHolder.styleData
                    val position = viewHolder.adapterPosition

                    when (direction) {

                        ItemTouchHelper.LEFT -> {
                            adapterListener.onSwipeToDelete(styleData, position) {

                                DataKit.deleteStyleFiles(context, styleData)
                                styleDataList.removeAt(position)
                                notifyItemRemoved(position)

                                if (selectedPosition >= position) {
                                    selectedPosition--
                                }
                                val newStyleData = styleDataList[selectedPosition]
                                selectedStyleData = newStyleData
                                notifyItemChanged(selectedPosition)

                                return@onSwipeToDelete selectedStyleData
                            }
                        }

                        ItemTouchHelper.RIGHT -> {
                            notifyItemChanged(position)
                            adapterListener.onSwipeToInfo(styleData, position)
                        }

                    }

                }

                override fun onChildDraw(
                    c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                    dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
                ) {
                    val icon = ContextCompat
                        .getDrawable(
                            context,
                            if (dX < 0) R.drawable.ic_delete_forever else R.drawable.ic_info
                        )
                        ?: return
                    val iconSize = icon.intrinsicWidth
                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top
                    val iconTop = itemView.top + (itemHeight - iconSize) / 2
                    val iconBottom = iconTop + iconSize
                    val iconMargin = (itemHeight - iconSize) / 2
                    if (dX < 0) {
                        // Remove
                        val iconLeft = itemView.right - iconMargin - iconSize
                        val iconRight = itemView.right - iconMargin
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    } else {
                        // Info
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + iconSize
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                    super.onChildDraw(
                        c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                    )
                }
            }
        )

    init {
        styleDataList.addAll(DataKit.loadStyleIndexList(context))
        val selectedStyleUid = context.defaultSharedPreferences
            .getString(context.getString(R.string.pref_selected_style_uid), styleDataList[0].uid)
        if (selectedStyleUid != null) {
            val styleData = findStyleDataBy(selectedStyleUid)
            val position = findPositionBy(selectedStyleUid)
            if ((styleData == null) || (position == null)) {
                selectedStyleData = styleDataList[0]
                selectedPosition = 0
                context.defaultSharedPreferences.edit {
                    putString(
                        context.getString(R.string.pref_selected_style_uid), selectedStyleData.uid
                    )
                }
            } else {
                selectedStyleData = styleData
                selectedPosition = position
            }
        } else {
            selectedStyleData = styleDataList[0]
            selectedPosition = 0
        }

    }

    override fun getItemCount(): Int = styleDataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.card_map_style, parent, false)
        return ViewHolderStyleCard(view, onStyleSelected)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderStyleCard) {
            val styleData = styleDataList[position]
            holder.setFrom(context, styleData, (selectedStyleData.uid == styleData.uid))
        }
    }

    fun onPause() {
        context.defaultSharedPreferences.edit {
            putString(
                context.getString(R.string.pref_selected_style_uid), selectedStyleData.uid
            )
        }
        DataKit.saveStyleIndexList(context, styleDataList)
    }

    fun onResume(): StyleData {
        val newSelectedUid = context.defaultSharedPreferences
            .getString(context.getString(R.string.pref_selected_style_uid), styleDataList[0].uid)
        if (newSelectedUid == null) {
            selectedStyleData = styleDataList[0]
            notifyItemChanged(selectedPosition)
            selectedPosition = 0
            notifyItemChanged(selectedPosition)
        } else if (selectedStyleData.uid != newSelectedUid) {
            val newSelectedStyleData = findStyleDataBy(newSelectedUid)
            val newSelectedPosition = findPositionBy(newSelectedUid)
            if ((newSelectedStyleData != null) && (newSelectedPosition != null)) {
                selectedStyleData = newSelectedStyleData
                notifyItemChanged(selectedPosition)
                selectedPosition = newSelectedPosition
                notifyItemChanged(selectedPosition)
            } else {
                selectedStyleData = styleDataList[0]
                notifyItemChanged(selectedPosition)
                selectedPosition = 0
            }
            notifyItemChanged(selectedPosition)
        }
        return selectedStyleData
    }

    private fun findPositionBy(uid: String): Int? {
        var count = 0
        for (styleData in styleDataList) {
            if (styleData.uid == uid) return count
            count += 1
        }
        return null
    }

    private fun findStyleDataBy(uid: String): StyleData? {
        for (styleData in styleDataList) {
            if (styleData.uid == uid) return styleData
        }
        return null
    }

    fun add(styleData: StyleData) {
        styleDataList.add(styleData)
        notifyItemInserted(itemCount - 1)
    }

    /**
     * Attach [itemTouchHelper] to the recycler view
     *
     * @author lucka-me
     * @since 0.1
     */
    fun attachItemTouchHelperTo(recyclerView: RecyclerView?) {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}