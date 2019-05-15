package labs.lucka.wallmapper

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

class MapStyleManagerRecyclerViewAdapter(
    private val context: Context,
    private val adapterListener: Listener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSelectedStyleIndexChanged(newStyleIndex: MapStyleIndex)
        fun onSwipeToDelete(target: MapStyleIndex, position: Int, onConfirmed: () -> MapStyleIndex)
        fun onSwipeToInfo(target: MapStyleIndex, position: Int)
    }

    private class ViewHolderStyleCard(
        itemView: View,
        private val onStyleSelected: (position: Int) -> Unit
    ): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val cardView: MaterialCardView = itemView.findViewById(R.id.card_style)
        val textName: TextView = itemView.findViewById(R.id.text_name)
        val textAuthor: TextView = itemView.findViewById(R.id.text_author)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) { onStyleSelected(adapterPosition) }

        fun setFrom(context: Context, styleIndex: MapStyleIndex, isSelected: Boolean = false) {
            textName.text = styleIndex.name
            textAuthor.text = String.format(context.getString(R.string.style_author), styleIndex.author)
            cardView.isChecked = isSelected
        }
    }

    private val mapStyleIndexList: ArrayList<MapStyleIndex> = arrayListOf()

    private var selectedPosition: Int = 0

    private val onStyleSelected: (Int) -> Unit = { position: Int ->

        if (selectedPosition != position) {
            val oldPosition = selectedPosition
            selectedPosition = position
            adapterListener.onSelectedStyleIndexChanged(mapStyleIndexList[position])
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
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
                        adapterListener.onSwipeToDelete(mapStyleIndexList[position], position) {
                            val target = mapStyleIndexList[position]

                            DataKit.deleteStyleFiles(context, target)
                            mapStyleIndexList.removeAt(position)
                            notifyItemRemoved(position)

                            if (selectedPosition >= position) {
                                selectedPosition--
                                notifyItemChanged(selectedPosition)
                            }

                            return@onSwipeToDelete mapStyleIndexList[selectedPosition]
                        }
                    }

                    ItemTouchHelper.RIGHT -> {
                        notifyItemChanged(position)
                        adapterListener.onSwipeToInfo(mapStyleIndexList[position], position)
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
        MapKit.checkAndUpdateStyleIndexList(context)
        reloadStyleIndexList()
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
            holder.setFrom(context, mapStyleIndexList[position], (selectedPosition == position))
        }
    }

    fun onPause() {
        context.defaultSharedPreferences.edit {
            putInt(context.getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[selectedPosition].id)
        }
        DataKit.saveStyleIndexList(context, mapStyleIndexList)
    }

    fun refreshSelectedStyleIndexFromPreferences(): MapStyleIndex {
        val selectedId = context.defaultSharedPreferences.getInt(
            context.getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[0].id
        )
        var newSelectedPosition = findPositionFrom(selectedId)
        if (newSelectedPosition == selectedPosition) return mapStyleIndexList[selectedPosition]
        if (newSelectedPosition < 0) {
            newSelectedPosition = 0
        }
        notifyItemChanged(selectedPosition)
        notifyItemChanged(newSelectedPosition)
        selectedPosition = newSelectedPosition
        adapterListener.onSelectedStyleIndexChanged(mapStyleIndexList[selectedPosition])
        return mapStyleIndexList[selectedPosition]
    }

    private fun findPositionFrom(id: Int): Int {
        for (i in 0 until mapStyleIndexList.size) if (mapStyleIndexList[i].id == id) return i
        return -1
    }

    fun reloadStyleIndexList(): MapStyleIndex {
        mapStyleIndexList.clear()
        mapStyleIndexList.addAll(DataKit.loadStyleIndexList(context))
        notifyDataSetChanged()
        return refreshSelectedStyleIndexFromPreferences()
    }

    fun add(mapStyleIndex: MapStyleIndex) {
        mapStyleIndexList.add(mapStyleIndex)
        notifyItemInserted(mapStyleIndexList.size - 1)
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