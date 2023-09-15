package us.groundstate.sfsweepalert

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import us.groundstate.sfsweepalert.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}