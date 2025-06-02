package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpException
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/positions")
    suspend fun sendPosition(@Body position: Position)

    @POST("api/form_submissions")
    suspend fun sendFormData(@Body submission: FormSubmission)

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(
    @SerializedName("data") val data: UserData?,
    @SerializedName("message") val message: String,
    @SerializedName("requiresPasswordChange") val requiresPasswordChange: Boolean? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: Any? = null,
    @SerializedName("stack") val stack: String? = null
)

data class UserData(
    @SerializedName("id") val id: Long,
    @SerializedName("phone") val phone: String?,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("password") val password: String?
)

class LoginActivity : AppCompatActivity() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-server.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.phone)
        val passwordInput = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = apiService.login(LoginRequest(username, password))
                        if (response.data != null) {
                            val userData = response.data
                            val user = User(
                                id = userData.id,
                                phone = userData.phone,
                                firstName = userData.firstName,
                                lastName = userData.lastName,
                                password = userData.password
                            )
                            // Save to database
                            val db = DatabaseHelper(this@LoginActivity)
                            db.insertUserAsync(user, object : DatabaseHelper.DatabaseHandler<Unit?> {
                                override fun onComplete(success: Boolean, result: Unit?) {
                                    if (success) {
                                        // Save device ID to SharedPreferences
                                        PreferenceManager.getDefaultSharedPreferences(this@LoginActivity).edit()
                                            .putString(MainFragment.KEY_DEVICE, user.id.toString())
                                            .apply()
                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                        finish()
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this@LoginActivity, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            })
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Login failed: ${response.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: HttpException) {
//                        val errorBody = e.response()?.errorBody()?.string()
//                        val errorResponse = errorBody?.let { Gson().fromJson(it, ErrorResponse::class.java) }
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Incorrect Credentials", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}