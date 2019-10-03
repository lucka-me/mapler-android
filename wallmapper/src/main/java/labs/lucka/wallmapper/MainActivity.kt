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

    private lateinit var recyclerViewAdapter: StyleRecyclerViewAdapter
    private val recyclerViewAdapterListener: StyleRecyclerViewAdapter.Listener =
        object : StyleRecyclerViewAdapter.Listener {

            override fun onSelectedStyleDataChanged(newStyleData: StyleData) {
                mapKit.setStyle(newStyleData)
            }

            override fun onSwipeToDelete(
                target: StyleData, position: Int, onConfirmed: () -> StyleData
            ) {

                if (target.type == StyleData.Type.MAPBOX || target.type == StyleData.Type.LUCKA) {
                    DialogKit.showSimpleAlert(
                        this@MainActivity, R.string.dialog_content_delete_default_style
                    )
                    recyclerViewAdapter.notifyItemChanged(position)
                    return
                }

                DialogKit.showDialog(
                    this@MainActivity,
                    R.string.dialog_title_delete_style,
                    getString(R.string.dialog_content_delete_style, target.name, target.author),
                    positiveButtonListener = { _, _ -> mapKit.setStyle(onConfirmed()) },
                    negativeButtonTextId = R.string.button_cancel,
                    negativeButtonListener = { _, _ ->
                        recyclerViewAdapter.notifyItemChanged(position)
                    },
                    cancelable = false
                )

            }

            override fun onSwipeToInfo(target: StyleData, position: Int) {

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
                                DataKit.saveStylePreviewImage(
                                    this@MainActivity, target, image
                                )
                                imageView.setImageBitmap(image)
                            },
                            { error ->
                                DialogKit.showSimpleAlert(this@MainActivity, error)
                            }
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
                    DialogKit.showSaveImageDialog(
                        this, image, { saveImage(image) }, { activateButtons() }
                    )
                },
                { error ->
                    DialogKit.showSimpleAlert(this, error)
                    activateButtons()
                }
            )
        }

        button_preferences.setOnClickListener {
            startActivity(Intent(this, PreferenceActivity::class.java))
        }

        button_add_style.setOnClickListener {

            DialogKit.showAddNewStyleFromUrlDialog(this) { newStyleIndex ->
                recyclerViewAdapter.add(newStyleIndex)

                // Snapshotter.fromStyleJson() still doesn't work properly, disable the feature temporarily
//            DialogKit.showAddNewStyleTypeSelectDialog(this) { type ->
//                when (type) {
//
//                    StyleData.Type.ONLINE -> {
//                        DialogKit.showAddNewStyleFromUrlDialog(this) { newStyleIndex ->
//                            recyclerViewAdapter.add(newStyleIndex)
//                        }
//                    }
//
//                    StyleData.Type.LOCAL -> {
//                        startActivityForResult(
//                            Intent(Intent.ACTION_GET_CONTENT)
//                                .addCategory(Intent.CATEGORY_OPENABLE)
//                                .setType(getString(R.string.mime_json)),
//                            DefaultValue.Request.OpenJsonFile.code
//                        )
//                    }
//
//                    else -> { }
//                }
            }
        }

        mapKit.onCreate(savedInstanceState, map_view_main) { activateButtons() }

        recyclerViewAdapter = StyleRecyclerViewAdapter(this, recyclerViewAdapterListener)
        recycler_view_style_manager.layoutManager = LinearLayoutManager(this)
        recycler_view_style_manager.adapter = recyclerViewAdapter
        recyclerViewAdapter.attachItemTouchHelperTo(recycler_view_style_manager)

        BottomSheetBehavior
            .from(bottom_sheet_style_manager)
            .bottomSheetCallback = object: BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING)
                    mapKit.deactivateMap()
                else
                    mapKit.activateMap()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) { }

        }
    }

    override fun onStart() {
        super.onStart()
        mapKit.onStart()
    }

    override fun onResume() {
        super.onResume()
        deactivateButtons()
        mapKit.onResume()
        mapKit.setStyle(recyclerViewAdapter.onResume()) {
            activateButtons()
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            DefaultValue.Request.RequestPermissionWriteExternalStorage.code -> {
                saveImage(mapKit.snapshot as Bitmap)
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        when (requestCode) {
//
//            DefaultValue.Request.OpenJsonFile.code -> {
//                if (resultCode == RESULT_OK && data != null) {
//                    val json: String = DataKit.readFile(this, data.data)
//                    DialogKit.showAddNewStyleFromJsonDialog(this) { newStyleIndex ->
//                        recyclerViewAdapter.add(newStyleIndex)
//                        DataKit.saveStyleJson(this, json, newStyleIndex)
//                    }
//                }
//            }
//
//        }
//
//    }

    private fun deactivateButtons() {
        fab_snapshot.hide()
    }

    private fun activateButtons() {
        fab_snapshot.show()
    }

    private fun saveImage(image: Bitmap) {
        if (ContextCompat
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
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
            { uri ->
                Snackbar.make(fab_snapshot, R.string.snack_saved_text, Snackbar.LENGTH_LONG)
                    .setAnchorView(fab_snapshot)
                    .setAction(R.string.snack_saved_action_set) {
                        startActivity(
                            wallpaperManager.getCropAndSetWallpaperIntent(
                                uri
                                //DataKit.getImageContentUri(this, file)
                            )
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
