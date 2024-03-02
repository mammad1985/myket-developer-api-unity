package ir.myket.developerapi.thread

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AbortableCountDownLatch(count: Int) : CountDownLatch(count) {
    private var aborted = false

    fun abort() {
        if (count == 0L) return
        aborted = true
        while (count > 0) countDown()
    }

    override fun await(timeout: Long, unit: TimeUnit): Boolean {
        val res = super.await(timeout, unit)
        if (aborted) return false

        return res
    }
}