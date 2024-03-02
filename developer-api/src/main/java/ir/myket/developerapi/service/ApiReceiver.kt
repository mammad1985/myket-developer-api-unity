package ir.myket.developerapi.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


interface ReceiverCommunicator {
    fun onNewBroadcastReceived(intent: Intent?)
}

class ApiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sendIntent = Intent().apply {
            action = intent.action
            intent.extras?.let {
                putExtras(it)
            }
        }
        notifyObservers(sendIntent)
    }

    companion object {
        private val observerLock = Any()
        private val observers: MutableList<ReceiverCommunicator> = ArrayList()
        fun addObserver(communicator: ReceiverCommunicator) {
            synchronized(observerLock) {
                observers.add(
                    communicator
                )
            }
        }

        fun removeObserver(communicator: ReceiverCommunicator) {
            synchronized(observerLock) {
                observers.remove(
                    communicator
                )
            }
        }

        private fun notifyObservers(intent: Intent) {
            synchronized(observerLock) {
                for (observer in observers) {
                    observer.onNewBroadcastReceived(intent)
                }
            }
        }
    }
}