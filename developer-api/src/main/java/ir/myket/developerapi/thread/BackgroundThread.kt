package ir.myket.developerapi.thread

import android.os.Handler
import android.os.HandlerThread

class BackgroundThread : HandlerThread("ial") {

    init {
        start()
    }

    private val threadHandler = Handler(looper)

    fun execute(func: () -> Unit) {
        threadHandler.post {
            func.invoke()
        }
    }
}