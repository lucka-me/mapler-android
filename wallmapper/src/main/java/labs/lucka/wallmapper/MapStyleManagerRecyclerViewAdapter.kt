package labs.lucka.wallmapper

import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.defaultSharedPreferences

class MapStyleManagerRecyclerViewAdapter(
    private val context: Context,
    private val mapStyleIndexList: ArrayList<MapStyleIndex>,
    private val adapterListener: Listener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSelectedIndexChanged(position: Int)
        fun onSwipeToDelete(position: Int)
        fun onSwipeToInfo(position: Int)
    }

    private class ViewHolderStyleCard(
        itemView: View,
        private val onStyleSelected: (position: Int) -> Unit
    ): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val textName: TextView = itemView.findViewById(R.id.textName)
        val textAuthor: TextView = itemView.findViewById(R.id.textAuthor)
        val radioSelected: RadioButton = itemView.findViewById(R.id.radioSelected)

        init {
            itemView.setOnClickListener(this)
            radioSelected.setOnClickListener { onStyleSelected(adapterPosition) }
        }

        override fun onClick(v: View?) { onStyleSelected(adapterPosition) }

        fun setFrom(context: Context, styleIndex: MapStyleIndex, isSelected: Boolean = false) {
            textName.text = styleIndex.name
            textAuthor.text = String.format(context.getString(R.string.style_author), styleIndex.author)
            radioSelected.isChecked = isSelected
        }
    }

    private var selectedIndex: Int = 0

    private val onStyleSelected: (Int) -> Unit = { position: Int ->

        if (selectedIndex != position) {
            val oldIndex = selectedIndex
            selectedIndex = position
            adapterListener.onSelectedIndexChanged(selectedIndex)
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedIndex)
        }

    }

    private val itemTouchHelper: ItemTouchHelper = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                when (direction) {

                    ItemTouchHelper.LEFT -> {
                        adapterListener.onSwipeToDelete(position)
                    }

                    ItemTouchHelper.RIGHT -> {
                        notifyItemChanged(position)
                        adapterListener.onSwipeToInfo(position)
                    }

                }

            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val icon = ContextCompat.getDrawable(
                    context, if (dX < 0) R.drawable.ic_delete_forever else R.drawable.ic_info
                ) ?: return
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
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
    )

    init {
        updateSelectedIndex()
    }

    override fun getItemCount(): Int {
        return mapStyleIndexList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_map_style, parent, false)
        return ViewHolderStyleCard(view, onStyleSelected)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderStyleCard) {
            holder.setFrom(context, mapStyleIndexList[position], (selectedIndex == position))
        }
    }

    fun updateSelectedIndex() {
        var newSelectedIndex = context.defaultSharedPreferences.getInt(
            context.getString(R.string.pref_style_manager_selected_index), 0
        )
        if (newSelectedIndex == selectedIndex) return
        if (newSelectedIndex >= mapStyleIndexList.size) {
            newSelectedIndex = 0
            context.defaultSharedPreferences.edit()
                .putInt(context.getString(R.string.pref_style_manager_selected_index), newSelectedIndex)
                .apply()
        }
        notifyItemChanged(selectedIndex)
        notifyItemChanged(newSelectedIndex)
        selectedIndex = newSelectedIndex
        adapterListener.onSelectedIndexChanged(selectedIndex)
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