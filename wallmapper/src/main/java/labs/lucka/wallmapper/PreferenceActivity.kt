package labs.lucka.wallmapper

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.*
import org.jetbrains.anko.defaultSharedPreferences

@Keep
class PreferenceActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Keep
    class PreferenceMainFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        var onSharedPreferenceChanged: (String) -> Unit = { }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_main, rootKey)

            findPreference<Preference>(getString(R.string.pref_live_wallpaper_set))
                ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                        .putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(requireContext(), WallmapperLiveService::class.java)
                        )
                )
                true
            }

        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            activity?.title = getString(R.string.pref_main_screen_title)
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key != null) { onSharedPreferenceChanged(key) }
        }

    }

    @Keep
    class PreferenceLiveWallpaperFragment :
        PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        var onSharedPreferenceChanged: (String) -> Unit = { }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_live_wallpaper, rootKey)

            if (
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requireContext().defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_location_follow), false)
                }
            }

            // Set input type
            findPreference<EditTextPreference>(getString(R.string.pref_live_wallpaper_style_random_interval))?.apply {
                setOnBindEditTextListener { editText -> editText.inputType = InputType.TYPE_CLASS_NUMBER }
                summaryProvider = Preference.SummaryProvider { preference: EditTextPreference ->
                    String.format(getString(R.string.pref_live_wallpaper_style_random_interval_summary), preference.text)
                }
            }
            findPreference<EditTextPreference>(getString(R.string.pref_live_wallpaper_location_radius))?.apply {
                setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                summaryProvider = Preference.SummaryProvider { preference: EditTextPreference ->
                    String.format(getString(R.string.pref_live_wallpaper_location_radius_summary), preference.text)
                }
            }

            findPreference<SwitchPreferenceCompat>(getString(R.string.pref_live_wallpaper_location_follow))
                ?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (
                        ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        true
                    } else {
                        if (
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            DialogKit.showDialog(
                                requireContext(),
                                R.string.dialog_title_request_permission,
                                R.string.request_permission_fine_location,
                                cancelable = false
                            )
                        } else {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                DefaultValue.Request.RequestPermissionFineLocation.code
                            )
                        }
                        false
                    }
                } else {
                    true
                }
            }

            findPreference<Preference>(getString(R.string.pref_live_wallpaper_camera_reset))
                ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val sharedPreferences = requireContext().defaultSharedPreferences
                val zoom =
                    sharedPreferences.getFloat(
                        getString(R.string.pref_map_last_position_zoom), DefaultValue.Map.ZOOM.toFloat()
                    ).toInt()
                val tilt =
                    sharedPreferences.getFloat(
                        getString(R.string.pref_map_last_position_tilt), DefaultValue.Map.TILT.toFloat()
                    ).toInt()
                val bearing =
                    sharedPreferences.getFloat(
                        getString(R.string.pref_map_last_position_bearing), DefaultValue.Map.BEARING.toFloat()
                    ).toInt()
                sharedPreferences.edit {
                    putInt(getString(R.string.pref_live_wallpaper_camera_zoom), zoom)
                    putInt(getString(R.string.pref_live_wallpaper_camera_tilt), tilt)
                    putInt(getString(R.string.pref_live_wallpaper_camera_bearing), bearing)
                }
                // Refresh the preferences
                findPreference<SeekBarPreference>(getString(R.string.pref_live_wallpaper_camera_zoom))?.value = zoom
                findPreference<SeekBarPreference>(getString(R.string.pref_live_wallpaper_camera_tilt))?.value = tilt
                findPreference<SeekBarPreference>(getString(R.string.pref_live_wallpaper_camera_bearing))?.value = bearing
                true
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            activity?.title = getString(R.string.pref_live_wallpaper_screen_title)
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key != null) { onSharedPreferenceChanged(key) }
        }

    }

    @Keep
    class PreferenceAboutFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_about, rootKey)

            findPreference<Preference>(getString(R.string.pref_about_summary_version_key))?.summary =
                String.format(
                    getString(R.string.pref_about_summary_version_summary),
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )

        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            activity?.title = getString(R.string.pref_about_screen_title)
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }

    private val resultIntent: Intent = Intent()
    private var initialPrefMapboxUseDefaultToken: Boolean = true
    private var initialPrefDisplayLabels: Boolean = true
    private val onSharedPreferenceChanged: (String) -> Unit = { key: String ->
        when (key) {
            getString(R.string.pref_mapbox_use_default_token) -> {
                resultIntent.putExtra(
                    getString(R.string.activity_result_should_reset_token),
                    (defaultSharedPreferences.getBoolean(key, true) != initialPrefMapboxUseDefaultToken)
                )
                setResult(Activity.RESULT_OK, resultIntent)
            }

            getString(R.string.pref_display_label) -> {
                resultIntent.putExtra(
                    getString(R.string.activity_result_should_reset_style),
                    (defaultSharedPreferences.getBoolean(key, true) != initialPrefDisplayLabels)
                )
                setResult(Activity.RESULT_OK, resultIntent)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialPrefMapboxUseDefaultToken =
            defaultSharedPreferences.getBoolean(getString(R.string.pref_mapbox_use_default_token), true)
        initialPrefDisplayLabels =
            defaultSharedPreferences.getBoolean(getString(R.string.pref_display_label), true)

        setContentView(R.layout.activity_preference)

        supportFragmentManager
            .beginTransaction()
            .add(
                R.id.preference_frame,
                PreferenceMainFragment().apply {
                    onSharedPreferenceChanged = this@PreferenceActivity.onSharedPreferenceChanged
                }
            )
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            DefaultValue.Request.RequestPermissionFineLocation.code -> {
                defaultSharedPreferences.edit {
                    putBoolean(getString(R.string.pref_live_wallpaper_location_follow), true)
                }
                (supportFragmentManager.findFragmentById(R.id.preference_frame) as PreferenceLiveWallpaperFragment?)
                    ?.findPreference<SwitchPreferenceCompat>(getString(R.string.pref_live_wallpaper_location_follow))
                    ?.isChecked = true
            }
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
        if (caller == null || pref == null) return false

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in, android.R.animator.fade_out,
                android.R.animator.fade_in, android.R.animator.fade_out
            )
            .replace(
                R.id.preference_frame,
                supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment).apply {
                    arguments = pref.extras
                    setTargetFragment(caller, 0)
                }
            )
            .addToBackStack(null)
            .commit()
        return true
    }
}