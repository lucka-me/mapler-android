package labs.lucka.wallmapper

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mapKit: MapKit
    private lateinit var wallpaperManager: WallpaperManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wallpaperManager = WallpaperManager.getInstance(this)

        mapKit = MapKit(this)
        setContentView(R.layout.activity_main)

        fabSnapshot.hide()

        fabManageStyle.setOnClickListener {
            startActivityForResult(
                Intent(this, MapStyleManagerActivity::class.java), RequestCode.ManageMapStyle.code
            )
        }

        fabSnapshot.setOnClickListener {
            progressBarSnapshot.visibility = View.VISIBLE
            deactivateButtons()
            mapKit.takeSnapshot(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight, { image ->

                DialogKit.showSaveImageDialog(this, image) { saveImage(image) }

                progressBarSnapshot.visibility = View.INVISIBLE
                activateButtons()
            }, { error ->

                DialogKit.showSimpleAlert(this, error)

                progressBarSnapshot.visibility = View.INVISIBLE
                activateButtons()
            })
        }

        mapKit.onCreate(savedInstanceState, mainMapView) {
            fabSnapshot.show()
            activateButtons()
        }

    }

    override fun onStart() {
        super.onStart()
        mapKit.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapKit.onResume()
    }

    override fun onPause() {
        mapKit.onPause()
        progressBarSnapshot.visibility = View.INVISIBLE
        activateButtons()
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
            RequestCode.RequestPermissionWriteExternalStorage.code -> {
                saveImage(mapKit.snapshot as Bitmap)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.ManageMapStyle.code && data != null) {
            if (
                data.getBooleanExtra(
                    getString(R.string.activity_result_should_reset_token), false
                )
            ) {
                deactivateButtons()
                mapKit.setToken()
                mapKit.setStyle { activateButtons() }
            } else if (
                data.getBooleanExtra(getString(R.string.activity_result_should_reset_style), false)
            ) {
                deactivateButtons()
                mapKit.setStyle {
                    fabSnapshot.show()
                    activateButtons()
                }
            }
        }
    }

    private fun deactivateButtons() {
        fabSnapshot.isClickable = false
        //fabManageStyle.isClickable = false
    }

    private fun activateButtons() {
        fabSnapshot.isClickable = true
        fabManageStyle.isClickable = true
    }

    private fun saveImage(image: Bitmap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            deactivateButtons()
            progressBarSnapshot.visibility = View.VISIBLE
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
                        + File.separator + getString(R.string.path_save_folder)
            )
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory.absolutePath, UUID.randomUUID().toString() + ".png")
            try {
                val fos = FileOutputStream(file)
                image.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                activateButtons()
                progressBarSnapshot.visibility = View.INVISIBLE
                Snackbar.make(layoutFab, R.string.snack_saved_text, Snackbar.LENGTH_LONG)
                    .setAction(R.string.snack_saved_action_set) {
                        startActivity(
                            wallpaperManager.getCropAndSetWallpaperIntent(
                                SnapshotKit.getImageContentUri(this, file)
                            )
                        )
                    }
                    .show()


            } catch (error: Exception) {
                DialogKit.showSimpleAlert(this, error.message)
                progressBarSnapshot.visibility = View.INVISIBLE
                activateButtons()
            }
        } else {
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
                    RequestCode.RequestPermissionWriteExternalStorage.code
                )
            }
        }

    }
}
