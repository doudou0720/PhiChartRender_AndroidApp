package com.example.phichartrender

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import android.content.Intent

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            
            // 为管理存档选项设置点击事件
            val manageArchivesPreference = findPreference<Preference>("manage_archives")
            manageArchivesPreference?.setOnPreferenceClickListener {
                val intent = Intent(activity, ArchiveManagerActivity::class.java)
                startActivity(intent)
                true
            }
        }
        
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 使用finish()替代已弃用的onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}