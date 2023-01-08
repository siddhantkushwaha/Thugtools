package com.siddhantkushwaha.thugtools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CoroutineHelper {

    fun <T> throttleLatest(
        intervalMs: Long,
        coroutineScope: CoroutineScope,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        var throttleJob: Job? = null
        var latestParam: T
        return { param: T ->
            latestParam = param
            if (throttleJob?.isCompleted != false) {
                throttleJob = coroutineScope.launch {
                    delay(intervalMs)
                    latestParam.let(destinationFunction)
                }
            }
        }
    }
}