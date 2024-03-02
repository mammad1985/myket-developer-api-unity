package ir.myket.developerapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import ir.myket.developerapi.util.getParcelableFromIntent


class ProxyActivity : Activity() {
    companion object {
        const val KEY_RECEIVER = "myket_login_receiver"
        const val KEY_INTENT = "myket_login_intent"
        const val KEY_LOGIN_RESULT = "login_result"

        const val REQUEST_CODE = 100
    }

    private var resultReceiver: ResultReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultReceiver = intent.getParcelableFromIntent(KEY_RECEIVER, ResultReceiver::class.java)
        val intent = intent.getParcelableFromIntent(KEY_INTENT, Intent::class.java)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            resultReceiver?.send(resultCode, getReceiverResult(data))
            finish()
        }
    }

    private fun getReceiverResult(data: Intent?): Bundle {
        return Bundle().apply {
            putParcelable(KEY_LOGIN_RESULT, data)
        }
    }
}