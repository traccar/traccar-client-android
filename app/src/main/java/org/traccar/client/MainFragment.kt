/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.TwoStatePreference
import dev.doubledot.doki.ui.DokiActivity
import java.util.*
import kotlin.collections.HashSet

class MainFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
    private var requestingPermissions: Boolean = false

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (BuildConfig.HIDDEN_APP && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            removeLauncherIcon()
        }
        setHasOptionsMenu(true)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setPreferencesFromResource(R.xml.preferences, rootKey)
        initPreferences()

        findPreference<Preference>(KEY_DEVICE)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            newValue != null && newValue != ""
        }
        findPreference<Preference>(KEY_URL)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            newValue != null && validateServerURL(newValue.toString())
        }
        findPreference<Preference>(KEY_INTERVAL)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                newValue != null && (newValue as String).toInt() > 0
            } catch (e: NumberFormatException) {
                Log.w(TAG, e)
                false
            }
        }
        val numberValidationListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                newValue != null && (newValue as String).toInt() >= 0
            } catch (e: NumberFormatException) {
                Log.w(TAG, e)
                false
            }
        }
        findPreference<Preference>(KEY_DISTANCE)?.onPreferenceChangeListener = numberValidationListener
        findPreference<Preference>(KEY_ANGLE)?.onPreferenceChangeListener = numberValidationListener

        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val originalIntent = Intent(activity, AutostartReceiver::class.java)
        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        alarmIntent = PendingIntent.getBroadcast(activity, 0, originalIntent, flags)

        if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
            startTrackingService(checkPermission = true, initialPermission = false)
        }
    }

    class NumericEditTextPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {

        override fun onBindDialogView(view: View) {
            val editText = view.findViewById<EditText>(android.R.id.edit)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            super.onBindDialogView(view)
        }

        companion object {
            fun newInstance(key: String?): NumericEditTextPreferenceDialogFragment {
                val fragment = NumericEditTextPreferenceDialogFragment()
                val bundle = Bundle()
                bundle.putString(ARG_KEY, key)
                fragment.arguments = bundle
                return fragment
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (listOf(KEY_INTERVAL, KEY_DISTANCE, KEY_ANGLE).contains(preference.key)) {
            val f: EditTextPreferenceDialogFragmentCompat =
                NumericEditTextPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(requireFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun removeLauncherIcon() {
        val className = MainActivity::class.java.canonicalName!!.replace(".MainActivity", ".Launcher")
        val componentName = ComponentName(requireActivity().packageName, className)
        val packageManager = requireActivity().packageManager
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            val builder = AlertDialog.Builder(requireActivity())
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setMessage(getString(R.string.hidden_alert))
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (requestingPermissions) {
            requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setPreferencesEnabled(enabled: Boolean) {
        findPreference<Preference>(KEY_DEVICE)?.isEnabled = enabled
        findPreference<Preference>(KEY_URL)?.isEnabled = enabled
        findPreference<Preference>(KEY_INTERVAL)?.isEnabled = enabled
        findPreference<Preference>(KEY_DISTANCE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ANGLE)?.isEnabled = enabled
        findPreference<Preference>(KEY_ACCURACY)?.isEnabled = enabled
        findPreference<Preference>(KEY_BUFFER)?.isEnabled = enabled
        findPreference<Preference>(KEY_WAKELOCK)?.isEnabled = enabled
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == KEY_STATUS) {
            if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
                startTrackingService(checkPermission = true, initialPermission = false)
            } else {
                stopTrackingService()
            }
            (requireActivity().application as MainApplication).handleRatingFlow(requireActivity())
        } else if (key == KEY_DEVICE) {
            findPreference<Preference>(KEY_DEVICE)?.summary = sharedPreferences.getString(KEY_DEVICE, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.status) {
            startActivity(Intent(activity, StatusActivity::class.java))
            return true
        } else if (item.itemId == R.id.info) {
            DokiActivity.start(requireContext())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initPreferences() {
        PreferenceManager.setDefaultValues(requireActivity(), R.xml.preferences, false)
        if (!sharedPreferences.contains(KEY_DEVICE)) {
            val id = (Random().nextInt(900000) + 100000).toString()
            sharedPreferences.edit().putString(KEY_DEVICE, id).apply()
            findPreference<EditTextPreference>(KEY_DEVICE)?.text = id
        }
        findPreference<Preference>(KEY_DEVICE)?.summary = sharedPreferences.getString(KEY_DEVICE, null)
    }

    private fun showBackgroundLocationDialog(context: Context, onSuccess: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        val option = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.backgroundPermissionOptionLabel
        } else {
            context.getString(R.string.request_background_option)
        }
        builder.setMessage(context.getString(R.string.request_background, option))
        builder.setPositiveButton(android.R.string.ok) { _, _ -> onSuccess() }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
        var permission = initialPermission
        if (checkPermission) {
            val requiredPermissions: MutableSet<String> = HashSet()
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permission = requiredPermissions.isEmpty()
            if (!permission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
                }
                return
            }
        }
        if (permission) {
            setPreferencesEnabled(false)
            ContextCompat.startForegroundService(requireContext(), Intent(activity, TrackingService::class.java))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestingPermissions = true
                showBackgroundLocationDialog(requireContext()) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
                }
            } else {
                requestingPermissions = BatteryOptimizationHelper().requestException(requireContext())
            }
        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply()
            val preference = findPreference<TwoStatePreference>(KEY_STATUS)
            preference?.isChecked = false
        }
    }

    private fun stopTrackingService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.cancel(alarmIntent)
        }
        requireActivity().stopService(Intent(activity, TrackingService::class.java))
        setPreferencesEnabled(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            startTrackingService(false, granted)
        }
    }

    private fun validateServerURL(userUrl: String): Boolean {
        val port = Uri.parse(userUrl).port
        if (
            URLUtil.isValidUrl(userUrl) &&
            (port == -1 || port in 1..65535) &&
            (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))
        ) {
            return true
        }
        Toast.makeText(activity, R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show()
        return false
    }

    companion object {
        private val TAG = MainFragment::class.java.simpleName
        private const val ALARM_MANAGER_INTERVAL = 15000
        const val KEY_DEVICE = "id"
        const val KEY_URL = "url"
        const val KEY_INTERVAL = "interval"
        const val KEY_DISTANCE = "distance"
        const val KEY_ANGLE = "angle"
        const val KEY_ACCURACY = "accuracy"
        const val KEY_STATUS = "status"
        const val KEY_BUFFER = "buffer"
        const val KEY_WAKELOCK = "wakelock"
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3
    }

}
