package ir.myket.developerapi.sample

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import ir.myket.developerapi.Core
import ir.myket.developerapi.data.ApiResult
import ir.myket.developerapi.data.Update
import ir.myket.developerapi.sample.databinding.ActivityMainBinding
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var core: Core
    private lateinit var mProgressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mProgressDialog = ProgressDialog(this)
        setupButtons()
        setButtonsEnable(false)

        core = Core(this)

        connectService()
    }

    private fun connectService() {
        Log.d(TAG, "Starting to connect service...")
        showProgress(true)
        setButtonsEnable(false)

        core.connect(object : Core.CoreListener<Boolean> {
            override fun onResult(response: Boolean) {
                Log.d(TAG, "Service connected successfully.")
                showProgress(false)
                setButtonsEnable(true)
            }

            override fun onError(result: ApiResult) {
                Log.e(TAG, "Could not connect to service! Message=${result.message}")
                showProgress(false)
                setButtonsEnable(false)
                if (result.response == Core.RESPONSE_RESULT_NEED_MYKET) {
                    alertNeedMyket(result.message)
                } else if (result.response == Core.RESPONSE_RESULT_MYKET_UPDATE) {
                    alertUpdateMyket(result.message)
                } else {
                    alert(result.message)
                }
            }
        })
    }

    private fun checkUpdate() {
        setButtonsEnable(false)
        showProgress(true)

        core.getAppUpdate(object : Core.CoreListener<Update> {
            override fun onResult(update: Update) {
                setButtonsEnable(true)
                showProgress(false)
                if (update.isUpdateAvailable) {
                    alert("An update with versionCode=${update.versionCode} Available!\nWhats New: ${update.description}")
                } else {
                    alert("App Already Updated :)")
                }
            }

            override fun onError(error: ApiResult) {
                setButtonsEnable(true)
                showProgress(false)
                Log.e(TAG, "Error=${error.message}")
                alert("Error=${error.message}")
            }
        })
    }

    private fun isUserLogin() {
        setButtonsEnable(false)
        core.isUserLogin {
            if (it) {
                alert("User Login")
            } else {
                alert("User Not Login")
            }
            setButtonsEnable(true)
        }
    }

    private fun getUserId() {
        setButtonsEnable(false)

        core.getUserId(this, object : Core.CoreListener<String> {
            override fun onResult(userId: String) {
                alert(userId)
                setButtonsEnable(true)
            }

            override fun onError(error: ApiResult) {
                Log.e(TAG, "Error=${error.message}")
                alert("Error=${error.message}")
                setButtonsEnable(true)
            }
        })
    }

    private fun saveData() {
        setButtonsEnable(false)
        showProgress(true)
        val data = "YOUR SIMPLE PAYLOAD".toByteArray()

        core.saveData(this, data, object : Core.CoreListener<Boolean> {
            override fun onResult(save: Boolean) {
                alert("Data Saved")
                setButtonsEnable(true)
                showProgress(false)
            }

            override fun onError(error: ApiResult) {
                Log.e(TAG, "Error=${error.message}")
                alert("Error=${error.message}")
                setButtonsEnable(true)
                showProgress(false)
            }
        })
    }

    private fun loadData() {
        setButtonsEnable(false)
        showProgress(true)

        core.loadData(this, object : Core.CoreListener<ByteArray> {
            override fun onResult(payload: ByteArray) {
                alert(payload.toString(Charset.defaultCharset()))
                setButtonsEnable(true)
                showProgress(false)
            }

            override fun onError(error: ApiResult) {
                Log.e(TAG, "Error=${error.message}")
                alert("Error=${error.message}")
                setButtonsEnable(true)
                showProgress(false)
            }
        })
    }

    private fun alertUpdateMyket(message: String) {
        AlertDialog.Builder(this).apply {
            setMessage(message)
            setCancelable(false)
            setPositiveButton("Update") { _, _ ->
                startActivity(Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("myket://details?id=ir.mservices.market")
                })
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun alertNeedMyket(message: String) {
        AlertDialog.Builder(this).apply {
            setMessage(message)
            setCancelable(false)
            setPositiveButton("Install") { _, _ ->
                startActivity(Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("https://myket.ir/")
                })
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun alert(message: String) {
        AlertDialog.Builder(this).apply {
            setMessage(message)
            setCancelable(false)
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun showProgress(show: Boolean) {
        mProgressDialog.setTitle("Please Wait...")
        mProgressDialog.setCancelable(false)
        when (show) {
            true -> mProgressDialog.show()
            false -> mProgressDialog.dismiss()
        }
    }

    private fun setupButtons() {
        binding.btnGetUpdate.setOnClickListener {
            checkUpdate()
        }
        binding.btnIsLogin.setOnClickListener {
            isUserLogin()
        }
        binding.btnGetUserId.setOnClickListener {
            getUserId()
        }
        binding.btnSavePayload.setOnClickListener {
            saveData()
        }
        binding.btnLoadPayload.setOnClickListener {
            loadData()
        }
    }

    private fun setButtonsEnable(isEnable: Boolean) {
        binding.btnGetUpdate.isEnabled = isEnable
        binding.btnIsLogin.isEnabled = isEnable
        binding.btnGetUserId.isEnabled = isEnable
        binding.btnSavePayload.isEnabled = isEnable
        binding.btnLoadPayload.isEnabled = isEnable
    }
}