package ir.myket.developerapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import android.widget.Toast
import ir.myket.developerapi.data.ApiResult
import ir.myket.developerapi.data.Update
import ir.myket.developerapi.service.ICore
import ir.myket.developerapi.service.IpcService
import ir.myket.developerapi.service.ReceiverService
import ir.myket.developerapi.thread.MainThread
import ir.myket.developerapi.thread.ThreadHelper
import ir.myket.developerapi.util.Security
import ir.myket.developerapi.util.decodeHex
import ir.myket.developerapi.util.getParcelableFromBundle


class Core(context: Context) {
    companion object {
        private const val TAG = "MyketDeveloperApi"
        private const val API_VERSION = 966
        private const val MYKET_PACKAGE_NAME = "ir.mservices.market"

        const val KEY_RESULT_CODE = "RESPONSE_CODE"
        const val KEY_UPDATE_AVAILABLE = "RESPONSE_APP_UPDATE_AVAILABLE"
        const val KEY_UPDATE_DESCRIPTION = "RESPONSE_APP_UPDATE_DESCRIPTION"
        const val KEY_VERSION_CODE = "RESPONSE_APP_VERSION_CODE"
        const val KEY_LOGIN_STATE = "RESPONSE_LOGIN"
        const val KEY_USER_ID = "RESPONSE_USER_ID"
        const val KEY_LOGIN_INTENT = "RESPONSE_LOGIN_INTENT"
        const val KEY_PAYLOAD = "RESPONSE_APP_STORAGE_PAYLOAD"
        const val KEY_MINIMUM_API_VERSION = "MINIMUM_API_VERSION"

        const val RESPONSE_RESULT_OK = 0
        const val RESPONSE_ERROR = -1
        const val RESPONSE_RESULT_DEVELOPER_API_UNAVAILABLE = 1
        const val RESPONSE_RESULT_DEVELOPER_ERROR = 2
        const val RESPONSE_RESULT_ERROR = 3
        const val RESPONSE_RESULT_USER_LOGIN = 4
        const val RESPONSE_RESULT_USER_NOT_LOGIN = 5
        const val RESPONSE_RESULT_USER_CANCEL = -2
        const val RESPONSE_RESULT_MYKET_UPDATE = -3
        const val RESPONSE_RESULT_ERROR_TIMEOUT = -4
        const val RESPONSE_RESULT_NEED_MYKET = -5
    }

    private val context = context.applicationContext
    private val packageName = context.packageName

    private var mSetupDone = false
    private var mAsyncInProgress = false
    private var mAsyncOperation = ""

    private lateinit var connection: ICore


    fun connect(listener: CoreListener<Boolean>) {
        connection = IpcService()
        if (isMarketInstalled()) {
            check(Security.verifyMyketClient(context)) {
                "Myket Client Verification Error!"
            }
        } else {
            listener.onError(
                ApiResult(
                    RESPONSE_RESULT_NEED_MYKET,
                    getTranslatedMessage(RESPONSE_RESULT_NEED_MYKET)
                )
            )
            return
        }


        if (!checkMyketVersion()) {
            listener.onError(
                ApiResult(
                    RESPONSE_RESULT_MYKET_UPDATE,
                    getTranslatedMessage(RESPONSE_RESULT_MYKET_UPDATE)
                )
            )
            return
        }

        check(!mSetupDone) { "Service already setup!" }

        connection.connect(context) { ipcConnected ->
            if (ipcConnected) {
                if (BuildConfig.DEBUG) {
                    Toast.makeText(context, "IPC Connected", Toast.LENGTH_SHORT).show()
                }
                mSetupDone = true
                isDeveloperApiSupported { listener.onResult(it) }
            } else {
                connection = ReceiverService(context)
                connection.connect(context) { receiverConnected ->
                    if (receiverConnected) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(context, "Receiver Connected", Toast.LENGTH_SHORT).show()
                        }
                        mSetupDone = true
                        isDeveloperApiSupported { listener.onResult(it) }
                    } else {
                        mSetupDone = false
                        listener.onError(ApiResult(0, "Could not connect to service"))
                    }
                }
            }
        }
    }

    private fun isDeveloperApiSupported(callback: (Boolean) -> Unit) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("isDeveloperApiSupported")
        connection.isDeveloperApiSupported {
            callback.invoke(it == RESPONSE_RESULT_OK)
        }
    }

    fun getAppUpdate(listener: CoreListener<Update>) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("getAppUpdate")

        flagStartAsync("getAppUpdate")
        connection.getAppUpdate(packageName) {
            ThreadHelper.switchToMainThread(MainThread()) {
                val responseCode = it.getInt(KEY_RESULT_CODE, RESPONSE_ERROR)
                if (responseCode == RESPONSE_RESULT_OK) {
                    val update = Update(
                        it.getBoolean(KEY_UPDATE_AVAILABLE),
                        it.getString(KEY_UPDATE_DESCRIPTION),
                        it.getInt(KEY_VERSION_CODE)
                    )
                    listener.onResult(update)
                } else {
                    listener.onError(
                        ApiResult(
                            responseCode,
                            getTranslatedMessage(responseCode)
                        )
                    )
                }
                flagEndAsync()
            }
        }
    }

    fun isUserLogin(callback: (Boolean) -> Unit) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("isUserLoginSync")

        connection.isUserLogin {
            callback.invoke(
                when (it) {
                    RESPONSE_RESULT_DEVELOPER_API_UNAVAILABLE -> false
                    RESPONSE_RESULT_USER_LOGIN -> true
                    RESPONSE_RESULT_USER_NOT_LOGIN -> false
                    else -> false
                }
            )
        }
    }

    fun getUserId(activity: Activity, listener: CoreListener<String>) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("getUserId")

        connection.getAccountInfo(packageName) { bundle ->
            val responseCode = bundle.getInt(KEY_RESULT_CODE, RESPONSE_ERROR)
            val loginState = bundle.getInt(KEY_LOGIN_STATE)
            if (responseCode == RESPONSE_RESULT_OK) {
                if (loginState == RESPONSE_RESULT_USER_LOGIN) {
                    val userId = bundle.getString(KEY_USER_ID)
                    if (!userId.isNullOrEmpty()) {
                        listener.onResult(userId)
                    } else {
                        listener.onError(
                            ApiResult(
                                RESPONSE_RESULT_ERROR,
                                getTranslatedMessage(RESPONSE_RESULT_ERROR)
                            )
                        )
                    }
                } else if (loginState == RESPONSE_RESULT_USER_NOT_LOGIN) {
                    val loginIntent =
                        bundle.getParcelableFromBundle(KEY_LOGIN_INTENT, Intent::class.java)
                    val intent = Intent(activity, ProxyActivity::class.java).apply {
                        putExtra(ProxyActivity.KEY_INTENT, loginIntent)
                        putExtra(ProxyActivity.KEY_RECEIVER, LoginResultReceiver(listener))
                    }
                    activity.startActivity(intent)
                } else {
                    listener.onError(
                        ApiResult(
                            RESPONSE_RESULT_ERROR,
                            getTranslatedMessage(RESPONSE_RESULT_ERROR)
                        )
                    )
                }
            }
        }
    }

    private inner class LoginResultReceiver(val listener: CoreListener<String>) :
        ResultReceiver(MainThread()) {
        override fun onReceiveResult(resultCode: Int, loginResult: Bundle?) {
            super.onReceiveResult(resultCode, loginResult)
            if (resultCode == Activity.RESULT_OK) {
                val intent = loginResult?.getParcelableFromBundle(
                    ProxyActivity.KEY_LOGIN_RESULT,
                    Intent::class.java
                )
                if (intent != null) {
                    val responseCode = intent.getIntExtra(KEY_RESULT_CODE, RESPONSE_ERROR)
                    val loginState =
                        intent.getIntExtra(
                            KEY_LOGIN_STATE,
                            RESPONSE_RESULT_USER_NOT_LOGIN
                        )
                    val userId = intent.getStringExtra(KEY_USER_ID)

                    if (responseCode == RESPONSE_RESULT_OK && loginState == RESPONSE_RESULT_USER_LOGIN && !userId.isNullOrEmpty()) {
                        listener.onResult(userId)
                    } else {
                        listener.onError(
                            ApiResult(
                                RESPONSE_RESULT_ERROR,
                                "Illegal State: Result Ok but could not get userId"
                            )
                        )
                    }
                } else {
                    listener.onError(
                        ApiResult(
                            RESPONSE_RESULT_ERROR,
                            "Illegal State: Activity resultCode"
                        )
                    )
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                listener.onError(
                    ApiResult(
                        RESPONSE_RESULT_USER_CANCEL,
                        getTranslatedMessage(RESPONSE_RESULT_USER_CANCEL)
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun saveData(activity: Activity, payload: ByteArray, listener: CoreListener<Boolean>) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("saveData")

        isUserLogin {
            if (!it) {
                getUserId(activity, object : CoreListener<String> {
                    override fun onResult(response: String) {
                        saveData(activity, payload, listener)
                        flagEndAsync()
                    }

                    override fun onError(error: ApiResult) {
                        listener.onError(
                            ApiResult(
                                error.response,
                                getTranslatedMessage(error.response)
                            )
                        )
                        Log.e(TAG, "Error=${error.message}")
                        flagEndAsync()
                    }
                })
            } else {
                flagStartAsync("saveData")
                connection.saveData(packageName, payload.toHexString()) {
                    ThreadHelper.switchToMainThread(MainThread()) {
                        val responseCode = it.getInt(KEY_RESULT_CODE, RESPONSE_ERROR)
                        if (responseCode == RESPONSE_RESULT_OK) {
                            listener.onResult(true)
                        } else {
                            listener.onError(
                                ApiResult(
                                    RESPONSE_RESULT_ERROR,
                                    "Response is not ok!"
                                )
                            )
                        }
                        flagEndAsync()
                    }
                }
            }
        }
    }

    fun loadData(activity: Activity, listener: CoreListener<ByteArray>) {
        check(Security.verifyMyketClient(context)) {
            "Myket Client Verification Error!"
        }
        checkSetupDone("loadData")

        isUserLogin {
            if (!it) {
                getUserId(activity, object : CoreListener<String> {
                    override fun onResult(response: String) {
                        loadData(activity, listener)
                        flagEndAsync()
                    }

                    override fun onError(error: ApiResult) {
                        listener.onError(
                            ApiResult(
                                error.response,
                                getTranslatedMessage(error.response)
                            )
                        )
                        Log.e(TAG, "Error=${error.message}")
                        flagEndAsync()
                    }
                })
            } else {
                flagStartAsync("loadData")
                connection.loadData(packageName) {
                    ThreadHelper.switchToMainThread(MainThread()) {
                        val responseCode = it.getInt(KEY_RESULT_CODE, RESPONSE_ERROR)
                        if (responseCode == RESPONSE_RESULT_OK) {
                            val payload = it.getString(KEY_PAYLOAD, "") as String
                            listener.onResult(payload.decodeHex().toByteArray())
                        } else {
                            listener.onError(
                                ApiResult(
                                    RESPONSE_RESULT_ERROR,
                                    "Response is not ok!"
                                )
                            )
                        }
                        flagEndAsync()
                    }
                }
            }
        }
    }

    private fun getTranslatedMessage(responseCode: Int): String {
        return when (responseCode) {
            RESPONSE_RESULT_OK -> "Response code is not Ok"
            RESPONSE_RESULT_DEVELOPER_API_UNAVAILABLE -> "Developer api version is too old!"
            RESPONSE_RESULT_DEVELOPER_ERROR -> "An input value is wrong!"
            RESPONSE_RESULT_ERROR -> "Backend/Network issue!"
            RESPONSE_RESULT_USER_LOGIN -> "User not login!"
            RESPONSE_RESULT_USER_CANCEL -> "User cancel the flow!"
            RESPONSE_RESULT_MYKET_UPDATE -> "Current Myket not support, please update"
            RESPONSE_RESULT_NEED_MYKET -> "Myket is not installed!"
            else -> "Unknown Error!"
        }
    }

    private fun flagStartAsync(operation: String) {
        check(!mAsyncInProgress) {
            "Can't start async operation ($operation) because another async operation($mAsyncOperation) is in progress."
        }
        mAsyncOperation = operation
        mAsyncInProgress = true
    }

    private fun flagEndAsync() {
        mAsyncOperation = ""
        mAsyncInProgress = false
    }

    private fun checkSetupDone(operation: String) {
        check(mSetupDone) { "helper is not set up. Can't perform operation: $operation" }
    }


    private fun checkMyketVersion(): Boolean {
        val packageInfo: PackageInfo? = getPackageInfo()
        val versionCode: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode
        } ?: 0
        return versionCode >= API_VERSION
    }

    private fun getPackageInfo(): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    MYKET_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of((PackageManager.MATCH_DISABLED_COMPONENTS).toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    MYKET_PACKAGE_NAME,
                    PackageManager.MATCH_DISABLED_COMPONENTS
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(MYKET_PACKAGE_NAME, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun isMarketInstalled() = getPackageInfo() != null

    interface CoreListener<T> {
        fun onResult(response: T)
        fun onError(error: ApiResult)
    }
}