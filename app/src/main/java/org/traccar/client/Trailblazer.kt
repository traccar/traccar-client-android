package org.traccar.client

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.preference.Preference
import com.google.android.material.internal.ViewUtils.hideKeyboard
import org.traccar.client.MainFragment.Companion.KEY_STATUS
import org.traccar.client.Trailblazer.Server_Details.device_id
import org.traccar.client.Trailblazer.Server_Details.location_accuracy
import org.traccar.client.Trailblazer.Server_Details.server_url
import java.util.Locale


class Trailblazer : AppCompatActivity() {

    private lateinit var connectionStatus: TextView;
    private lateinit var sosButton: ImageButton
    private lateinit var deviceId: TextView;
    private lateinit var clockInImage: ImageView;
    private lateinit var clockInText: TextView;
    private lateinit var settingsButton: ImageButton;

    private lateinit var cardView: CardView
    private lateinit var deviceIdText: EditText
    private lateinit var serverUrlLabel: EditText
    private lateinit var locationAccuracyLabel: EditText

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
    private var requestingPermissions: Boolean = false

    private var onlineStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_trailblazer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(16, systemBars.top, 16, systemBars.bottom)
            insets
        }

        setupView()
        setupPreferences()
    }

    private fun setupView() {
        connectionStatus = findViewById<TextView>(R.id.connection_status)
        sosButton = findViewById<ImageButton>(R.id.sos)

        deviceId = findViewById<TextView>(R.id.device_id)

        clockInImage = findViewById<ImageView>(R.id.clock_in_image)
        clockInText = findViewById<TextView>(R.id.clock_in_text)
        settingsButton = findViewById<ImageButton>(R.id.settings_button)

        cardView = findViewById<CardView>(R.id.settings_view)
        deviceIdText = findViewById<EditText>(R.id.settings_device_id)
        serverUrlLabel = findViewById<EditText>(R.id.settings_server_url)
        locationAccuracyLabel = findViewById<EditText>(R.id.settings_location_accuracy)

        cardView.isVisible = false

        updateConnectionOffline()
    }

    public final fun clockInAndOut(view: View) {
        if (deviceId.text.toString().trim().isNotEmpty() && deviceId.text.toString().trim().isNotBlank()) {
            if (this.onlineStatus) {
                disconnectUser()
            } else {
                connectUser()
            }
        } else {
            showCardView()
        }
    }

    public final fun settingsClicked(view: View) {
        disconnectUser()
        showCardView()
    }

    private fun connectUser() {
        onlineStatus = true
        updateConnectionOnline()
        startTrackingService(checkPermission = true, initialPermission = false)
    }

    private fun disconnectUser() {
        onlineStatus = false
        updateConnectionOffline()
        stopTrackingService()
    }

    private fun showCardView() {
        deviceIdText.setText(device_id)
        serverUrlLabel.setText(server_url)
        locationAccuracyLabel.setText(location_accuracy)
        cardView.isVisible = true
    }

    public final fun cancelSettingsClicked(view: View) {
        hideKeyboard()
        cardView.isVisible = false
    }

    public final fun saveSettingsClicked(view: View) {
        hideKeyboard()
        if (deviceIdText.text.toString().trim().isNotEmpty() && deviceIdText.text.toString().trim().isNotBlank()) {
            sharedPreferences.edit().putString(Trailblazer.KEY_DEVICE, deviceIdText.text.toString()).apply()
            deviceId.text = sharedPreferences.getString(Trailblazer.KEY_DEVICE, "")
            device_id = deviceId.text.toString()
            cardView.isVisible = false
        } else {
            Toast.makeText(this, "Please enter the device id", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideKeyboard() {
        val imm=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(deviceIdText.windowToken, 0)
    }

    fun updateConnectionOnline() {
        connectionStatus.text = getString(R.string.status_connected)
        connectionStatus.setTextColor(ContextCompat.getColor(this,R.color.primary))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(), R.drawable.status_connected, null)

        clockInText.text = getString(R.string.clock_out)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.clock_out, null))
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun updateConnectionOffline() {
        connectionStatus.text = getString(R.string.status_disconnected)
        connectionStatus.setTextColor(ContextCompat.getColor(this,R.color.light_gray))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(), R.drawable.status_disconnected, null)

        clockInText.text = getString(R.string.clock_in)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.clock_in, null))
    }

    private fun setupPreferences() {
        sharedPreferences = getPreferences(MODE_PRIVATE)
        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val originalIntent = Intent(this, AutostartReceiver::class.java)
        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        alarmIntent = PendingIntent.getBroadcast(this, 0, originalIntent, flags)

        if (sharedPreferences.contains(Trailblazer.KEY_DEVICE)) {
            deviceId.text = sharedPreferences.getString(Trailblazer.KEY_DEVICE, "")
        } else {
            sharedPreferences.edit().putString(Trailblazer.KEY_DEVICE, "").apply()
            deviceId.setText("")
        }

        device_id = deviceId.text.toString()
        server_url = getString(R.string.settings_server_url_value)
        location_accuracy = getString(R.string.settings_location_accuracy_value)
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
            ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestingPermissions = true
                showBackgroundLocationDialog(this) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
                }
            } else {
                requestingPermissions = BatteryOptimizationHelper().requestException(this)
            }
        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply()
        }
    }

    private fun stopTrackingService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.cancel(alarmIntent)
        }
        stopService(Intent(this, TrackingService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    object Server_Details {
        lateinit var device_id: String
        lateinit var server_url: String
        lateinit var location_accuracy: String
    }

    companion object {
        private val TAG = Trailblazer::class.java.simpleName
        private const val ALARM_MANAGER_INTERVAL = 15000
        private const val RETRY_DELAY = 30 * 1000
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