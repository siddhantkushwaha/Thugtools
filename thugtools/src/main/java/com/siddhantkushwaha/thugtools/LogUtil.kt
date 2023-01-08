package com.siddhantkushwaha.thugtools


import android.util.Log


class LogUtil(
    cls: Any,
    private val isDebugBuild: Boolean
) {

    private val tag = "AppLogging - ${cls::class.simpleName.toString()}"

    fun debug(message: Any, newTag: String? = null) {
        val curTag = newTag ?: tag
        if (isDebugBuild) Log.d(curTag, "$message")
    }

    fun warn(message: Any, newTag: String? = null) {
        val curTag = newTag ?: tag
        if (isDebugBuild) Log.w(curTag, "$message")
    }

    fun err(message: Any, newTag: String? = null) {
        val curTag = newTag ?: tag
        if (isDebugBuild) Log.e(curTag, "$message")
    }
}