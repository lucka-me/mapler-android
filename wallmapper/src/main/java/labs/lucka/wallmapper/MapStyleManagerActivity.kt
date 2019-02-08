package labs.lucka.wallmapper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_map_style_manager.*
import org.jetbrains.anko.defaultSharedPreferences

class MapStyleManagerActivity: AppCompatActivity() {

    private var mapStyleIndexList: ArrayList<MapStyleIndex> = arrayListOf()
    private lateinit var recyclerViewAdapter: MapStyleManagerRecyclerViewAdapter
    private val recyclerViewAdapterListener: MapStyleManagerRecyclerViewAdapter.Listener =
        object : MapStyleManagerRecyclerViewAdapter.Listener {

            override fun onSelectedIndexChanged(position: Int) {
                resultIntent.putExtra(getString(R.string.activity_result_should_reset_style), true)
                setResult(Activity.RESULT_OK, resultIntent)
            }

            override fun onSwipeToDelete(position: Int) {

                val target = mapStyleIndexList[position]
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

                        DataKit.deleteStyleJson(this@MapStyleManagerActivity, target.path)
                        mapStyleIndexList.removeAt(position)
                        recyclerViewAdapter.notifyItemRemoved(position)

                        var selectedIndex = defaultSharedPreferences.getInt(
                            getString(R.string.pref_style_manager_selected_index), 0
                        )
                        if (selectedIndex >= position) {
                            selectedIndex--
                            defaultSharedPreferences.edit()
                                .putInt(getString(R.string.pref_style_manager_selected_index), selectedIndex)
                                .apply()
                            recyclerViewAdapter.updateSelectedIndex()

                        }
                    },
                    negativeButtonTextId = R.string.button_cancel,
                    negativeButtonListener = { _, _ ->

                        recyclerViewAdapter.notifyItemChanged(position)

                    },
                    cancelable = false
                )

            }

            override fun onSwipeToInfo(position: Int) {

                val target = mapStyleIndexList[position]
                DialogKit.showStyleInformationDialog(this@MapStyleManagerActivity, target) {
                    recyclerViewAdapter.notifyItemChanged(position)
                }
            }
        }
    private lateinit var snapshotKit: SnapshotKit
    private var resultIntent: Intent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_style_manager)

        snapshotKit = SnapshotKit(this)

        MapKit.checkAndUpdateStyleList(this)
        mapStyleIndexList = DataKit.loadStyleIndexList(this)

        recyclerViewAdapter =
                MapStyleManagerRecyclerViewAdapter(this, mapStyleIndexList, recyclerViewAdapterListener)
        recyclerViewMapStyleList.layoutManager = LinearLayoutManager(this)
        recyclerViewMapStyleList.adapter = recyclerViewAdapter
        recyclerViewAdapter.attachItemTouchHelperTo(recyclerViewMapStyleList)
    }

    override fun onResume() {
        super.onResume()
        resultIntent = Intent()
    }

    override fun onPause() {
        DataKit.saveStyleIndexList(this, mapStyleIndexList)
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
                                    RequestCode.OpenJsonFile.code
                                )
                            }

                            else -> { }
                        }
                    }
                }

                R.id.menu_preference -> {
                    startActivityForResult(
                        Intent(this, PreferenceMainActivity::class.java), RequestCode.SetPreference.code
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

            RequestCode.SetPreference.code -> {
                if (data != null) {
                    if (
                        data.getBooleanExtra(getString(R.string.activity_result_should_reset_token), false)
                    ) {
                        resultIntent.putExtra(getString(R.string.activity_result_should_reset_token), true)
                        setResult(Activity.RESULT_OK, resultIntent)
                        MapKit.checkAndUpdateStyleList(this)
                        mapStyleIndexList.clear()
                        mapStyleIndexList.addAll(DataKit.loadStyleIndexList(this))
                        recyclerViewAdapter.updateSelectedIndex()
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

            RequestCode.OpenJsonFile.code -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val json: String = DataKit.readFile(this, data.data)
                    DialogKit.showAddNewStyleFromJsonDialog(this) { newStyleIndex ->
                        mapStyleIndexList.add(newStyleIndex)
                        recyclerViewAdapter.notifyItemInserted(mapStyleIndexList.size - 1)
                        DataKit.saveStyleJson(this, json, newStyleIndex.path)
                    }
                }
            }
        }

    }
}