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
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.jetbrains.anko.defaultSharedPreferences

class PreferenceMainActivity : AppCompatActivity() {

    class PreferenceMainFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        var onSharedPreferenceChanged: (String) -> Unit = { }
        lateinit var thisActivity: PreferenceMainActivity

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference_main, rootKey)

            val prefLiveRadius = findPreference<EditTextPreference>(getString(R.string.pref_live_wallpaper_radius))
            prefLiveRadius.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            prefLiveRadius.summaryProvider = Preference.SummaryProvider { preference: EditTextPreference ->
                String.format(getString(R.string.pref_live_wallpaper_radius_summary), preference.text)
            }

            findPreference<Preference>(getString(R.string.pref_live_wallpaper_set)).onPreferenceClickListener =
                    object : Preference.OnPreferenceClickListener {
                        override fun onPreferenceClick(preference: Preference?): Boolean {
                            if (preference == null) return false
                            if (
                                ContextCompat.checkSelfPermission(
                                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                                )
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                startActivity(
                                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                        .putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(requireContext(), WallmapperLiveService::class.java)
                                        )
                                )
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
                            }

                            return true
                        }
                    }
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
        if (savedInstanceState == null) {
            val preferenceFragment = PreferenceMainFragment()
            preferenceFragment.onSharedPreferenceChanged = onSharedPreferenceChanged
            preferenceFragment.thisActivity = this
            supportFragmentManager
                .beginTransaction()
                .add(R.id.preferenceFrame, preferenceFragment)
                .commit()
        }
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
}