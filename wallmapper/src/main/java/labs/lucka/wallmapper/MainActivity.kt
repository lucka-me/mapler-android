package labs.lucka.wallmapper

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_style_manager.*

class MainActivity : AppCompatActivity() {

    private lateinit var mapKit: MapKit
    private lateinit var wallpaperManager: WallpaperManager

    private lateinit var recyclerViewAdapter: MapStyleManagerRecyclerViewAdapter
    private val recyclerViewAdapterListener: MapStyleManagerRecyclerViewAdapter.Listener =
        object : MapStyleManagerRecyclerViewAdapter.Listener {

            override fun onSelectedStyleIndexChanged(newStyleIndex: MapStyleIndex) {
                mapKit.setStyleIndex(newStyleIndex)
            }

            override fun onSwipeToDelete(target: MapStyleIndex, position: Int, onConfirmed: () -> MapStyleIndex) {

                if (target.type == MapStyleIndex.StyleType.MAPBOX || target.type == MapStyleIndex.StyleType.LUCKA) {
                    DialogKit.showSimpleAlert(
                        this@MainActivity, R.string.dialog_content_delete_default_style
                    )
                    recyclerViewAdapter.notifyItemChanged(position)
                    return
                }

                DialogKit.showDialog(
                    this@MainActivity,
                    R.string.dialog_title_delete_style,
                    String.format(
                        getString(R.string.dialog_content_delete_style), target.name, target.author
                    ),
                    positiveButtonListener = { _, _ ->

                        mapKit.setStyleIndex(onConfirmed())


                    },
                    negativeButtonTextId = R.string.button_cancel,
                    negativeButtonListener = { _, _ ->

                        recyclerViewAdapter.notifyItemChanged(position)

                    },
                    cancelable = false
                )

            }

            override fun onSwipeToInfo(target: MapStyleIndex, position: Int) {

                DialogKit.showStyleInformationDialog(
                    this@MainActivity, target,
                    {
                        DialogKit.showEditStyleDialog(this@MainActivity, target) {
                            recyclerViewAdapter.notifyItemChanged(position)
                        }
                    },
                    { imageView ->
                        mapKit.getPreviewImage(
                            target,
                            { image ->
                                DataKit.saveStylePreviewImage(this@MainActivity, target, image)
                                imageView.setImageBitmap(image)
                            },
                            { error -> DialogKit.showSimpleAlert(this@MainActivity, error) }
                        )
                    }
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wallpaperManager = WallpaperManager.getInstance(this)

        mapKit = MapKit(this)
        setContentView(R.layout.activity_main)

        deactivateButtons()

        fab_snapshot.setOnClickListener {
            deactivateButtons()
            mapKit.takeSnapshot(
                wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight,
                { image ->
                    DialogKit.showSaveImageDialog(this, image, { saveImage(image) }, { activateButtons() })
                },
                { error ->
                    DialogKit.showSimpleAlert(this, error)
                    activateButtons()
                }
            )
        }

        button_preferences.setOnClickListener {
            startActivityForResult(
                Intent(this, PreferenceActivity::class.java), DefaultValue.Request.SetPreference.code
            )
        }

        button_add_style.setOnClickListener {
            DialogKit.showAddNewStyleTypeSelectDialog(this) { type ->
                when (type) {

                    MapStyleIndex.StyleType.ONLINE -> {
                        DialogKit.showAddNewStyleFromUrlDialog(this) { newStyleIndex ->
                            recyclerViewAdapter.add(newStyleIndex)
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

        mapKit.onCreate(savedInstanceState, map_view_main) { activateButtons() }

        recyclerViewAdapter =
            MapStyleManagerRecyclerViewAdapter(this, recyclerViewAdapterListener)
        recycler_view_style_manager.layoutManager = LinearLayoutManager(this)
        recycler_view_style_manager.adapter = recyclerViewAdapter
        recyclerViewAdapter.attachItemTouchHelperTo(recycler_view_style_manager)

        val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_style_manager)
        bottomSheetBehavior.setBottomSheetCallback(
            object: BottomSheetBehavior.BottomSheetCallback() {

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) mapKit.deactivateMap() else mapKit.activateMap()
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) { }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        mapKit.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapKit.onResume()
        deactivateButtons()
        mapKit.setStyleIndex(recyclerViewAdapter.refreshSelectedStyleIndexFromPreferences()) { activateButtons() }
    }

    override fun onPause() {
        deactivateButtons()
        mapKit.onPause()
        recyclerViewAdapter.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapKit.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapKit.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        mapKit.onLowMemory()
        super.onLowMemory()
    }

    override fun onDestroy() {
        mapKit.onDestroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            DefaultValue.Request.RequestPermissionWriteExternalStorage.code -> {
                saveImage(mapKit.snapshot as Bitmap)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            DefaultValue.Request.SetPreference.code -> {
                if (data == null) return
                if (
                    data.getBooleanExtra(
                        getString(R.string.activity_result_should_reset_token), false
                    )
                ) {
                    deactivateButtons()
                    MapKit.setToken(this)
                    mapKit.setStyleIndex(recyclerViewAdapter.reloadStyleIndexList()) { activateButtons() }
                }
                if (
                    data.getBooleanExtra(getString(R.string.activity_result_should_reset_style), false)
                ) {
                    deactivateButtons()
                    mapKit.setStyleIndex(recyclerViewAdapter.refreshSelectedStyleIndexFromPreferences(), true) {
                        activateButtons()
                    }
                }
            }

            DefaultValue.Request.OpenJsonFile.code -> {
                if (resultCode == RESULT_OK && data != null) {
                    val json: String = DataKit.readFile(this, data.data)
                    DialogKit.showAddNewStyleFromJsonDialog(this) { newStyleIndex ->
                        recyclerViewAdapter.add(newStyleIndex)
                        DataKit.saveStyleJson(this, json, newStyleIndex)
                    }
                }
            }

        }


    }

    private fun deactivateButtons() {
        fab_snapshot.shrink()
        fab_snapshot.isEnabled = false
    }

    private fun activateButtons() {
        fab_snapshot.extend()
        fab_snapshot.isEnabled = true
    }

    private fun saveImage(image: Bitmap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                DialogKit.showDialog(
                    this,
                    R.string.dialog_title_request_permission,
                    R.string.request_permission_write_external_permission,
                    cancelable = false
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    DefaultValue.Request.RequestPermissionWriteExternalStorage.code
                )
            }
            return
        }

        deactivateButtons()
        DataKit.saveImage(
            this, image,
            { file ->
                Snackbar.make(fab_snapshot, R.string.snack_saved_text, Snackbar.LENGTH_LONG)
                    .setAnchorView(fab_snapshot)
                    .setAction(R.string.snack_saved_action_set) {
                        startActivity(
                            wallpaperManager
                                .getCropAndSetWallpaperIntent(DataKit.getImageContentUri(this, file))
                        )
                    }
                    .show()
                activateButtons()
            },
            {
                DialogKit.showSimpleAlert(this, it.message)
                activateButtons()
            }
        )

    }
}
