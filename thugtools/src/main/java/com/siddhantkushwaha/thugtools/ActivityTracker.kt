package com.siddhantkushwaha.thugtools

import android.app.Activity
import android.os.Bundle


class ActivityTracker {

    companion object {

        private var currentActivityClass: String? = null
        private var currentActivityExtras: Bundle? = null

        private val logger = LogUtil(this, true)

        fun setInfo(activity: Activity) {
            currentActivityClass = activity::class.java.toString()
            currentActivityExtras = activity.intent.extras

            logger.debug("Activity info set for $currentActivityClass")

            val action = activity.intent.action
            if (action != null) {
                logger.debug("Action : [$action]")
            }

            val data = activity.intent.dataString
            if (data != null) {
                logger.debug("Action : [$data]")
            }

            val keys = currentActivityExtras?.keySet()
            keys?.forEach { key ->
                logger.debug("$currentActivityClass, $key, ${currentActivityExtras?.get(key)}")
            }
        }

        fun resetInfo() {
            logger.debug("Activity info reset for $currentActivityClass")

            currentActivityClass = null
            currentActivityExtras = null
        }

        fun getActivityName(): String? {
            return currentActivityClass
        }

        fun getActivityExtras(): Bundle? {
            return currentActivityExtras
        }
    }
}