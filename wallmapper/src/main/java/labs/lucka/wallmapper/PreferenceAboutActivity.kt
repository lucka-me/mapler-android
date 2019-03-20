package labs.lucka.wallmapper

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PreferenceAboutActivity : AppCompatActivity() {

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preference)
        if (savedInstanceState == null) {
            val preferenceFragment = PreferenceAboutFragment()
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