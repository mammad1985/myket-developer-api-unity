package ir.myket.developerapi.service

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.os.Bundle
import ir.myket.developerapi.Core
import ir.myket.developerapi.service.ICore.Companion.MINIMUM_API_VERSION
import ir.myket.developerapi.service.ICore.Companion.MYKET_PACKAGE_NAME
import ir.myket.developerapi.thread.AbortableCountDownLatch
import ir.myket.developerapi.thread.BackgroundThread
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.TimeUnit


class ReceiverService(private val context: Context) : ICore {

    companion object {
        private const val ACTION_PING = "$MYKET_PACKAGE_NAME.developerPing"
        private const val ACTION_DEVELOPER_API_SUPPORT = "$MYKET_PACKAGE_NAME.developerSupport"
        private const val ACTION_IS_LOGIN = "$MYKET_PACKAGE_NAME.isUserLogin"
        private const val ACTION_GET_APP_UPDATE = "$MYKET_PACKAGE_NAME.getAppUpdate"
        private const val ACTION_GET_ACCOUNT_INFO = "$MYKET_PACKAGE_NAME.getAccountInfo"
        private const val ACTION_SAVE_DATA = "$MYKET_PACKAGE_NAME.saveData"
        private const val ACTION_LOAD_DATA = "$MYKET_PACKAGE_NAME.loadData"

        private const val KEY_RESULT_CODE = "RESPONSE_CODE"
        private const val KEY_SECURE = "SECURE"
        private const val KEY_PACKAGE_NAME = "PACKAGE_NAME"
        private const val KEY_PAYLOAD = "PAYLOAD"
    }

    private val backgroundThread = BackgroundThread()
    private val secureKey = UUID.randomUUID().toString()

    private var connectListener: WeakReference<OnBroadCastListener<Boolean>>? = null
    private var isApiSupportedListener: WeakReference<OnBroadCastListener<Bundle>>? = null
    private var loginListener: WeakReference<OnBroadCastListener<Bundle>>? = null
    private var updateListener: WeakReference<OnBroadCastListener<Bundle>>? = null
    private var accountInfoListener: WeakReference<OnBroadCastListener<Bundle>>? = null
    private var saveDataListener: WeakReference<OnBroadCastListener<Bundle>>? = null
    private var loadDataListener: WeakReference<OnBroadCastListener<Bundle>>? = null

    override fun connect(context: Context, callback: (Boolean) -> Unit) {
        createReceiver().also {
            ApiReceiver.addObserver(it)
        }

        with(backgroundThread) {
            execute {
                val connectLatch = AbortableCountDownLatch(1)

                context.sendBroadcast(getNewIntentForBroadcast(context.packageName).apply {
                    action = ACTION_PING
                })

                connectListener = WeakReference<OnBroadCastListener<Boolean>>(object :
                    OnBroadCastListener<Boolean> {
                    override fun onResult(result: Boolean?) {
                        connectLatch.countDown()
                        callback.invoke(true)
                    }
                })

                if (!connectLatch.await(5, TimeUnit.SECONDS)) {
                    connectListener = null
                    callback.invoke(false)
                }
            }
        }
    }

    private fun createReceiver(): ReceiverCommunicator = object : ReceiverCommunicator {
        override fun onNewBroadcastReceived(intent: Intent?) {
            if (intent?.action != null) {
                val secureResponse = intent.getStringExtra(KEY_SECURE)
                if (secureResponse == null || secureResponse != secureKey) {
                    // dispose
                    return
                }
                when (intent.action) {
                    ACTION_PING -> {
                        connectListener?.get()?.onResult(true)
                    }

                    ACTION_DEVELOPER_API_SUPPORT -> {
                        isApiSupportedListener?.get()?.onResult(intent.extras)
                    }

                    ACTION_IS_LOGIN -> {
                        loginListener?.get()?.onResult(intent.extras)
                    }

                    ACTION_GET_APP_UPDATE -> {
                        updateListener?.get()?.onResult(intent.extras)
                    }

                    ACTION_GET_ACCOUNT_INFO -> {
                        accountInfoListener?.get()?.onResult(intent.extras)
                    }

                    ACTION_SAVE_DATA -> {
                        saveDataListener?.get()?.onResult(intent.extras)
                    }

                    ACTION_LOAD_DATA -> {
                        loadDataListener?.get()?.onResult(intent.extras)
                    }
                }
            }
        }
    }

    private fun getNewIntentForBroadcast(packageName: String): Intent {
        return Intent().apply {
            putExtras(Bundle().apply {
                putString(KEY_PACKAGE_NAME, packageName)
                putString(KEY_SECURE, secureKey)
                putInt(Core.KEY_MINIMUM_API_VERSION, MINIMUM_API_VERSION)
            })
            setPackage(MYKET_PACKAGE_NAME)
            flags = FLAG_INCLUDE_STOPPED_PACKAGES
        }
    }

    override fun isDeveloperApiSupported(callback: (Int) -> Unit) {
        with(backgroundThread) {
            execute {
                context.sendBroadcast(getNewIntentForBroadcast(context.packageName).apply {
                    action = ACTION_DEVELOPER_API_SUPPORT
                })
            }
        }
        isApiSupportedListener = WeakReference<OnBroadCastListener<Bundle>>(object :
            OnBroadCastListener<Bundle> {
            override fun onResult(result: Bundle?) {
                result?.let {
                    callback.invoke(it.getInt(KEY_RESULT_CODE))
                }
            }
        })
    }

    override fun isUserLogin(callback: (Int) -> Unit) {
        context.sendBroadcast(getNewIntentForBroadcast(context.packageName).apply {
            action = ACTION_IS_LOGIN
        })
        loginListener = WeakReference<OnBroadCastListener<Bundle>>(object :
            OnBroadCastListener<Bundle> {
            override fun onResult(result: Bundle?) {
                result?.let {
                    callback.invoke(it.getInt(KEY_RESULT_CODE))
                }
            }
        })
    }

    override fun getAppUpdate(packageName: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                val updateLatch = AbortableCountDownLatch(1)
                context.sendBroadcast(getNewIntentForBroadcast(packageName).apply {
                    action = ACTION_GET_APP_UPDATE
                })
                updateListener = WeakReference<OnBroadCastListener<Bundle>>(object :
                    OnBroadCastListener<Bundle> {
                    override fun onResult(result: Bundle?) {
                        result?.let {
                            updateLatch.countDown()
                            callback.invoke(it)
                        }
                    }
                })

                if (!updateLatch.await(30, TimeUnit.SECONDS)) {
                    updateListener = null
                    callback.invoke(Bundle().apply {
                        putInt(Core.KEY_RESULT_CODE, Core.RESPONSE_RESULT_ERROR_TIMEOUT)
                    })
                }
            }
        }
    }

    override fun getAccountInfo(packageName: String, callback: (Bundle) -> Unit) {
        context.sendBroadcast(getNewIntentForBroadcast(packageName).apply {
            action = ACTION_GET_ACCOUNT_INFO
        })
        accountInfoListener = WeakReference<OnBroadCastListener<Bundle>>(object :
            OnBroadCastListener<Bundle> {
            override fun onResult(result: Bundle?) {
                result?.let {
                    callback.invoke(it)
                }
            }
        })
    }

    override fun saveData(packageName: String, payload: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                val saveDataLatch = AbortableCountDownLatch(1)

                context.sendBroadcast(getNewIntentForBroadcast(packageName).apply {
                    action = ACTION_SAVE_DATA
                }.putExtra(KEY_PAYLOAD, payload))

                saveDataListener = WeakReference<OnBroadCastListener<Bundle>>(object :
                    OnBroadCastListener<Bundle> {
                    override fun onResult(result: Bundle?) {
                        result?.let {
                            saveDataLatch.countDown()
                            callback.invoke(it)
                        }
                    }
                })

                if (!saveDataLatch.await(30, TimeUnit.SECONDS)) {
                    saveDataListener = null
                    callback.invoke(Bundle().apply {
                        putInt(Core.KEY_RESULT_CODE, Core.RESPONSE_RESULT_ERROR_TIMEOUT)
                    })
                }
            }
        }
    }

    override fun loadData(packageName: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                val loadDataLatch = AbortableCountDownLatch(1)

                context.sendBroadcast(getNewIntentForBroadcast(packageName).apply {
                    action = ACTION_LOAD_DATA
                })
                loadDataListener = WeakReference<OnBroadCastListener<Bundle>>(object :
                    OnBroadCastListener<Bundle> {
                    override fun onResult(result: Bundle?) {
                        result?.let {
                            loadDataLatch.countDown()
                            callback.invoke(it)
                        }
                    }
                })

                if (!loadDataLatch.await(30, TimeUnit.SECONDS)) {
                    loadDataListener = null
                    callback.invoke(Bundle().apply {
                        putInt(Core.KEY_RESULT_CODE, Core.RESPONSE_RESULT_ERROR_TIMEOUT)
                    })
                }
            }
        }
    }

    private interface OnBroadCastListener<T> {
        fun onResult(result: T?)
    }
}