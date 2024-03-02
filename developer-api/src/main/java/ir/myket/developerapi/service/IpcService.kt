package ir.myket.developerapi.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import com.myket.api.IMyketDeveloperApi
import ir.myket.developerapi.service.ICore.Companion.MINIMUM_API_VERSION
import ir.myket.developerapi.service.ICore.Companion.MYKET_PACKAGE_NAME
import ir.myket.developerapi.thread.BackgroundThread

class IpcService : ICore {
    companion object {
        private const val MYKET_BIND_SERVICE = "ir.mservices.market.MyketDeveloperApiService.BIND"
    }

    private val backgroundThread = BackgroundThread()
    private var mService: IMyketDeveloperApi? = null

    override fun connect(context: Context, callback: (Boolean) -> Unit) {
        val serviceIntent = Intent(MYKET_BIND_SERVICE).apply {
            setPackage(MYKET_PACKAGE_NAME)
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService = IMyketDeveloperApi.Stub.asInterface(service)
                callback.invoke(true)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mService = null
            }
        }

        if (isServiceAvailable(context, serviceIntent)) {
            context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        } else {
            callback.invoke(false)
        }
    }

    override fun isDeveloperApiSupported(callback: (Int) -> Unit) {
        mService?.let {
            callback.invoke(it.isDeveloperApiSupported(MINIMUM_API_VERSION))
        }
    }

    override fun getAppUpdate(packageName: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                mService?.let {
                    callback.invoke(it.getAppUpdateState(MINIMUM_API_VERSION, packageName))
                }
            }
        }
    }

    override fun isUserLogin(callback: (Int) -> Unit) {
        mService?.let {
            callback.invoke(it.isUserLogin(MINIMUM_API_VERSION))
        }
    }

    override fun getAccountInfo(packageName: String, callback: (Bundle) -> Unit) {
        mService?.let {
            callback.invoke(it.getAccountInfo(MINIMUM_API_VERSION, packageName))
        }
    }

    override fun saveData(packageName: String, payload: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                mService?.let {
                    callback.invoke(it.saveData(MINIMUM_API_VERSION, packageName, payload))
                }
            }
        }
    }

    override fun loadData(packageName: String, callback: (Bundle) -> Unit) {
        with(backgroundThread) {
            execute {
                mService?.let {
                    callback.invoke(it.loadData(MINIMUM_API_VERSION, packageName))
                }
            }
        }
    }

    private fun isServiceAvailable(context: Context, serviceIntent: Intent): Boolean {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentServices(
                serviceIntent,
                PackageManager.ResolveInfoFlags.of((PackageManager.MATCH_DISABLED_COMPONENTS).toLong())
            ).isNotEmpty()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(
                serviceIntent,
                PackageManager.MATCH_DISABLED_COMPONENTS
            ).isNotEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(serviceIntent, 0).isNotEmpty()
        }
    }
}