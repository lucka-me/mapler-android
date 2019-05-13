package labs.lucka.wallmapper

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_map_style_manager.*
import org.jetbrains.anko.defaultSharedPreferences

class MapStyleManagerActivity: AppCompatActivity() {

    private var mapStyleIndexList: ArrayList<MapStyleIndex> = arrayListOf()
    private lateinit var recyclerViewAdapter: MapStyleManagerRecyclerViewAdapter
    private val recyclerViewAdapterListener: MapStyleManagerRecyclerViewAdapter.Listener =
        object : MapStyleManagerRecyclerViewAdapter.Listener {

            override fun onSelectedStyleIndexChanged(newStyleIndex: MapStyleIndex) {
                lastSelectedId = newStyleIndex.id
            }

            override fun onSwipeToDelete(target: MapStyleIndex, position: Int) {

                if (target.type == MapStyleIndex.StyleType.MAPBOX || target.type == MapStyleIndex.StyleType.LUCKA) {
                    DialogKit.showSimpleAlert(
                        this@MapStyleManagerActivity, R.string.dialog_content_delete_default_style
                    )
                    recyclerViewAdapter.notifyItemChanged(position)
                    return
                }

                DialogKit.showDialog(
                    this@MapStyleManagerActivity,
                    R.string.dialog_title_delete_style,
                    String.format(
                        getString(R.string.dialog_content_delete_style), target.name, target.author
                    ),
                    positiveButtonListener = { _, _ ->

                        DataKit.deleteStyleFiles(this@MapStyleManagerActivity, target)
                        mapStyleIndexList.removeAt(position)
                        recyclerViewAdapter.notifyItemRemoved(position)

                        if (lastSelectedId == target.id) {
                            var newSelectedPosition = position
                            lastSelectedId = if (position == mapStyleIndexList.size) {
                                newSelectedPosition--
                                mapStyleIndexList.last().id
                            } else {
                                mapStyleIndexList[position].id
                            }
                            recyclerViewAdapter.notifyItemRemoved(position)
                            recyclerViewAdapter.notifyNewSelectedPosition(newSelectedPosition)

                        }
                    },
                    negativeButtonTextId = R.string.button_cancel,
                    negativeButtonListener = { _, _ ->

                        recyclerViewAdapter.notifyItemChanged(position)

                    },
                    cancelable = false
                )

            }

            override fun onSwipeToInfo(target: MapStyleIndex, position: Int) {

                snapshotKit.refresh()
                DialogKit.showStyleInformationDialog(
                    this@MapStyleManagerActivity, target,
                    {
                        DialogKit.showEditStyleDialog(this@MapStyleManagerActivity, target) {
                            recyclerViewAdapter.notifyItemChanged(position)
                        }
                    },
                    { imageView ->
                        val size = Point()
                        windowManager.defaultDisplay.getSize(size)
                        val onSnapshotReady: (Bitmap) -> Unit = { image ->
                            DataKit.saveStylePreviewImage(this@MapStyleManagerActivity, target, image)
                            imageView.setImageBitmap(image)
                        }
                        snapshotKit.takeSnapshot(
                            size.x, size.y, target, DefaultValue.Map.CAMERA_POSITION, onSnapshotReady
                        ) { DialogKit.showSimpleAlert(this@MapStyleManagerActivity, it) }
                    }
                )
            }
        }
    private lateinit var snapshotKit: SnapshotKit
    private var resultIntent: Intent = Intent()
    private var initSelectedId: Int = 0
    private var lastSelectedId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_style_manager)

        snapshotKit = SnapshotKit(this)

        MapKit.checkAndUpdateStyleIndexList(this)
        mapStyleIndexList = DataKit.loadStyleIndexList(this)
        recyclerViewAdapter =
                MapStyleManagerRecyclerViewAdapter(this, mapStyleIndexList, recyclerViewAdapterListener)
        recyclerViewMapStyleList.layoutManager = LinearLayoutManager(this)
        recyclerViewMapStyleList.adapter = recyclerViewAdapter
        recyclerViewAdapter.attachItemTouchHelperTo(recyclerViewMapStyleList)

        lastSelectedId =
            defaultSharedPreferences.getInt(getString(R.string.pref_style_manager_selected_id), mapStyleIndexList[0].id)
    }

    override fun onResume() {
        super.onResume()
        resultIntent = Intent()
        recyclerViewAdapter.refreshSelectedPositionFromPreferences()
        initSelectedId = lastSelectedId
    }

    override fun onPause() {
        DataKit.saveStyleIndexList(this, mapStyleIndexList)
        snapshotKit.onPause()
        if (lastSelectedId != initSelectedId) {
            defaultSharedPreferences.edit {
                putInt(getString(R.string.pref_style_manager_selected_id), lastSelectedId)
            }
        }
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_style_manager, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {

            when (item.itemId) {

                android.R.id.home -> {
                    onBackPressed()
                    return true
                }

                R.id.menu_add_style -> {
                    DialogKit.showAddNewStyleTypeSelectDialog(this) { type ->
                        when (type) {
                            MapStyleIndex.StyleType.ONLINE -> {
                                DialogKit.showAddNewStyleFromUrlDialog(this) { newStyleIndex ->
                                    mapStyleIndexList.add(newStyleIndex)
                                    recyclerViewAdapter.notifyItemInserted(mapStyleIndexList.size - 1)
                                }

                            }

                            MapStyleIndex.StyleType.LOCAL -> {
                                startActivityForResult(
                                    Intent(Intent.ACTION_GET_CONTENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .setType(getString(R.string.mime_json)),
                                    DefaultValue.Request.OpenJsonFile.code
                                )
                            }

                            else -> { }
                        }
                    }
                }

                R.id.menu_preference -> {
                    startActivityForResult(
                        Intent(this, PreferenceMainActivity::class.java),
                        DefaultValue.Request.SetPreference.code
                    )
                }

                R.id.menu_about -> { startActivity(Intent(this, PreferenceAboutActivity::class.java)) }

            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            DefaultValue.Request.SetPreference.code -> {
                if (data != null) {
                    if (
                        data.getBooleanExtra(getString(R.string.activity_result_should_reset_token), false)
                    ) {
                        resultIntent.putExtra(getString(R.string.activity_result_should_reset_token), true)
                        MapKit.setToken(this)
                        setResult(Activity.RESULT_OK, resultIntent)
                        mapStyleIndexList.clear()
                        mapStyleIndexList.addAll(DataKit.loadStyleIndexList(this))
                        recyclerViewAdapter.refreshSelectedPositionFromPreferences()
                        recyclerViewAdapter.notifyDataSetChanged()
                        invalidateOptionsMenu()
                    }
                    if (
                        data.getBooleanExtra(getString(R.string.activity_result_should_reset_style), false)
                    ) {
                        resultIntent.putExtra(getString(R.string.activity_result_should_reset_style), true)
                        setResult(Activity.RESULT_OK, resultIntent)
                    }
                }
            }

            DefaultValue.Request.OpenJsonFile.code -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val json: String = DataKit.readFile(this, data.data)
                    DialogKit.showAddNewStyleFromJsonDialog(this) { newStyleIndex ->
                        mapStyleIndexList.add(newStyleIndex)
                        recyclerViewAdapter.notifyItemInserted(mapStyleIndexList.size - 1)
                        DataKit.saveStyleJson(this, json, newStyleIndex)
                    }
                }
            }
        }

    }
}