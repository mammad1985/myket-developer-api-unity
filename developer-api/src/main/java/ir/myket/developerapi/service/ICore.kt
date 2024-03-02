package ir.myket.developerapi.service

import android.content.Context
import android.os.Bundle

interface ICore {
    companion object {
        const val MYKET_PACKAGE_NAME = "ir.mservices.market"
        const val MINIMUM_API_VERSION = 3
    }

    fun isDeveloperApiSupported(callback: (Int) -> Unit)
    fun connect(context: Context, callback: (Boolean) -> Unit)
    fun isUserLogin(callback: (Int) -> Unit)
    fun getAppUpdate(packageName: String, callback: (Bundle) -> Unit)
    fun getAccountInfo(packageName: String, callback: (Bundle) -> Unit)
    fun saveData(packageName: String, payload: String, callback: (Bundle) -> Unit)
    fun loadData(packageName: String, callback: (Bundle) -> Unit)
}