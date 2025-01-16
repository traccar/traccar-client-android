package org.traccar.client.trailblazer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.traccar.client.Position
import org.traccar.client.PositionProviderFactory
import org.traccar.client.R
import org.traccar.client.trailblazer.service.PositionProvider.PositionListener
import org.traccar.client.trailblazer.model.ProtocolFormatter.formatRequest
import org.traccar.client.trailblazer.network.RequestManager.RequestHandler
import org.traccar.client.trailblazer.network.RequestManager.sendRequestAsync
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.device_id
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.location_accuracy
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.server_url
import org.traccar.client.trailblazer.service.AutostartReceiver
import org.traccar.client.trailblazer.service.PositionProvider
import org.traccar.client.trailblazer.service.TrackingService
import org.traccar.client.trailblazer.util.BatteryOptimizationHelper


class Trailblazer : AppCompatActivity(), PositionListener {

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
    private lateinit var positionProvider: PositionProvider // = PositionProviderFactory.create(this, this)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var remoteConfig: FirebaseRemoteConfig

    private var onlineStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        //FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_trailblazer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(16, systemBars.top, 16, systemBars.bottom)
            insets
        }
       // remoteConfig = FirebaseRemoteConfig.getInstance()
       // remoteConfig.setDefaultsAsync(mapOf("logs_activated" to false))

        setupView()
        setupPreferences()
        setupLogsListener()
        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {

        // Create an instance of BatteryOptimizationHelper
        val batteryOptimizationHelper = BatteryOptimizationHelper()

        // Check if battery optimization exception is needed and request it
        if (batteryOptimizationHelper.requestException(this)) {
            Log.i(TAG, "Battery optimization exception request was triggered.")
        } else {
            Log.i(TAG, "Battery optimization exception already granted or not needed.")
        }

    }

    object Server_Details {
        lateinit var device_id: String
        lateinit var server_url: String
        lateinit var location_accuracy: String
    }

    companion object {
        public val TAG = "TRAIL_CONNECTION"
        private const val ALARM_MANAGER_INTERVAL = 15000
        private const val RETRY_DELAY = 5 * 1000
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

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted for background location (required for Android 10+)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
        } else {
            // Permissions granted, proceed with location updates
            updateConnectionOnline()
            positionProvider.startUpdates()
            startTrackingService(checkPermission = true, initialPermission = false)
            Log.i(TAG, "User connected successfully.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted, proceed with starting location updates
                    connectUser()
                } else {
                    // Permission denied, show message to the user
                    Toast.makeText(this, "Location permission is required to access GPS", Toast.LENGTH_LONG).show()
                }
            }

            PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Background location permission granted
                    updateConnectionOnline()
                    positionProvider.startUpdates()
                    startTrackingService(checkPermission = true, initialPermission = false)
                    Log.i(TAG, "User connected successfully.")
                } else {
                    // Background location permission denied, show message to the user
                    Toast.makeText(this, "Background location permission is required for tracking location in the background.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun setupLogsListener() {
        findViewById<ImageButton>(R.id.btn_Logs).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.linContent, LogsFragment())
                .addToBackStack(null)
                .commit()
        }

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

        if (sharedPreferences.contains(KEY_DEVICE)) {
            deviceId.text = sharedPreferences.getString(KEY_DEVICE, "")
        } else {
            sharedPreferences.edit().putString(KEY_DEVICE, "").apply()
            deviceId.setText("")
        }

        device_id = deviceId.text.toString()
        server_url = getString(R.string.settings_server_url_value)
        location_accuracy = getString(R.string.settings_location_accuracy_value)
        positionProvider = PositionProviderFactory.create(this, this)
    }

    public final fun clockInAndOut(view: View) {
        Log.d(TAG, "clockInAndOut invoked")
        if (deviceId.text.toString().trim().isNotEmpty() && deviceId.text.toString().trim().isNotBlank()) {
            if (this.onlineStatus) {
                Log.i(TAG, "User is online. Proceeding to disconnect.")
                disconnectUser()
            } else {
                Log.i(TAG, "User is offline. Proceeding to connect.")
                connectUser()
            }
        } else {
            Log.w(TAG, "Device ID is empty or blank. Showing card view.")
            showCardView()
        }
    }

    public final fun settingsClicked(view: View) {
        Log.d(TAG, "settingsClicked invoked")
        disconnectUser()
        Log.i(TAG, "User disconnected successfully. Showing card view.")
        showCardView()
    }

    private fun connectUser() {
        try {
            Log.d(TAG, "Connecting user...")
            onlineStatus = true
            checkLocationPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect user: ${e.message}", e)
        }
    }

    fun updateConnectionOnline() {
        connectionStatus.text = getString(R.string.status_connected)
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(),
            R.drawable.status_connected, null)

        clockInText.text = getString(R.string.clock_out)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
            R.drawable.clock_out, null))
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun updateConnectionOffline() {
        connectionStatus.text = getString(R.string.status_disconnected)
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.light_gray))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(),
            R.drawable.status_disconnected, null)

        clockInText.text = getString(R.string.clock_in)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
            R.drawable.clock_in, null))
    }

    private fun disconnectUser() {
        try {
            Log.d(TAG, "Disconnecting user...")
            onlineStatus = false
            updateConnectionOffline()
            positionProvider.stopUpdates()
            stopTrackingService()
            Log.i(TAG, "User disconnected successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect user: ${e.message}", e)
        }
    }

    private fun showCardView() {
        Log.d(TAG, "Showing card view")
        deviceIdText.setText(device_id)
        serverUrlLabel.setText(server_url)
        locationAccuracyLabel.setText(location_accuracy)
        cardView.isVisible = true
    }

    public final fun cancelSettingsClicked(view: View) {
        Log.d(TAG, "cancelSettingsClicked invoked")
        hideKeyboard()
        cardView.isVisible = false
        Log.i(TAG, "Settings view canceled.")
    }

    public final fun saveSettingsClicked(view: View) {
        Log.d(TAG, "saveSettingsClicked invoked")
        hideKeyboard()
        if (deviceIdText.text.toString().trim().isNotEmpty() && deviceIdText.text.toString().trim().isNotBlank()) {
            try {
                sharedPreferences.edit().putString(KEY_DEVICE, deviceIdText.text.toString()).apply()
                deviceId.text = sharedPreferences.getString(KEY_DEVICE, "")
                device_id = deviceId.text.toString()
                cardView.isVisible = false
                Log.i(TAG, "Settings saved successfully. Device ID updated to: $device_id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Device ID is empty or blank. Prompting user.")
            Toast.makeText(this, "Please enter the device id", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideKeyboard() {
        try {
            Log.d(TAG, "Hiding keyboard")
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(deviceIdText.windowToken, 0)
            Log.i(TAG, "Keyboard hidden successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide keyboard: ${e.message}", e)
        }
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
        Log.d(TAG, "Starting tracking service with checkPermission=$checkPermission, initialPermission=$initialPermission")
        // Existing logic with appropriate logs for permission checks and exceptions
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
        Log.i(TAG, "Tracking service started successfully.")


    }

    private fun stopTrackingService() {
        try {
            Log.d(TAG, "Stopping tracking service...")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.cancel(alarmIntent)
            }
            stopService(Intent(this, TrackingService::class.java))
            Log.i(TAG, "Tracking service stopped successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tracking service: ${e.message}", e)
        }
    }

    override fun onPositionUpdate(position: Position) {
        Log.d(TAG, "Position update received: $position")
        handler.postDelayed({
            if (onlineStatus) {
                Log.i(TAG, "Sending position update...")
                send(position)
            }
        }, RETRY_DELAY.toLong())
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position error encountered: ${error.message}", error)
    }

    private fun send(position: Position) {
        try {
            position.deviceId = Server_Details.device_id.replace("\\s".toRegex(), "").uppercase()
            val serverUrl: String = Server_Details.server_url
            val request = formatRequest(serverUrl, position)

            sendRequestAsync(request, object : RequestHandler {
                override fun onComplete(success: Boolean) {
                    if (success) {
                        Log.i(TAG, "Position data sent successfully.")
                    } else {
                        Log.w(TAG, "Failed to send position data.")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send position data: ${e.message}", e)
        }
    }

}