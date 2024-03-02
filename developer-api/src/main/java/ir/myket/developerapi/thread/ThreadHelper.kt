package ir.myket.developerapi.thread

import android.os.Looper

internal object ThreadHelper {

    fun switchToMainThread(mainThread: MainThread, task: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.invoke()
        } else {
            mainThread.post(Runnable(task))
        }
    }
}