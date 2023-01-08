package com.siddhantkushwaha.thugtools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object Util {
    private val logger = LogUtil(this, true)

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getDeviceModelInfo(): String {
        return "${Build.MANUFACTURER} - ${Build.MODEL}"
    }

    fun getHash(data: String, algorithm: String = "SHA-256"): String {
        return MessageDigest.getInstance(algorithm).digest(data.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    fun getHashLong(data: String, algorithm: String = "SHA-256"): Long {
        val bytes = MessageDigest.getInstance(algorithm).digest(data.toByteArray())
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        return buffer.long
    }

    fun getHashInt(data: String, algorithm: String = "SHA-256"): Int {
        val bytes = MessageDigest.getInstance(algorithm).digest(data.toByteArray())
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        return buffer.int
    }

    fun isProbablyAValidPhoneNumber(number: String): Boolean {
        val charsRegex = Regex("[a-zA-Z]")
        val normalizedNumber = normalizePhoneNumber(number)
        return if (normalizedNumber == null)
            false
        else !normalizedNumber.contains(charsRegex)
    }

    fun normalizePhoneNumber(number: String?): String? {
        val charsRegex = Regex("[a-zA-Z]")
        return if (number == null) {
            null
        } else if (number.contains(charsRegex)) {
            number.lowercase(Locale.getDefault())
                .replace(Regex("[^a-z0-9]"), "")
        } else {
            try {
                val phoneNumberUtil = PhoneNumberUtil.getInstance()
                val parsedPhone = phoneNumberUtil.parse(number, "IN")
                val numberType = phoneNumberUtil.getNumberType(parsedPhone)

                val addCountryCode = numberType == PhoneNumberUtil.PhoneNumberType.MOBILE
                        || numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE

                if (addCountryCode) {
                    phoneNumberUtil.format(parsedPhone, PhoneNumberUtil.PhoneNumberFormat.E164)
                } else {
                    parsedPhone.nationalNumber.toString()
                }
            } catch (exception: NumberParseException) {
                logger.err("Error parsing [$number].")
                null
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }
        }
    }

    private fun containsDigit(s: String): Boolean {
        var containsDigit = false
        if (s.isNotEmpty()) {
            for (c in s.toCharArray()) {
                if (Character.isDigit(c).also { containsDigit = it }) {
                    break
                }
            }
        }
        return containsDigit
    }

    fun cleanText(text: String): String {
        val textBuilder = StringBuilder()
        for (word in text.split(" ")) {

            // remove links and emails
            if (word.contains('/') && word.contains('.')) {
                continue
            } else if (word.contains(".com") || word.contains(".me")) {
                continue
            } else if (word.contains('@') && word.contains('.')) {
                continue
            }

            // remove all tokens with numbers
            else if (containsDigit(word)) {
                textBuilder.append(" #")
            }

            // otherwise clean and add
            else {
                var cleanedWord = word.lowercase(Locale.getDefault())
                cleanedWord = Regex("[^A-Za-z0-9 ]").replace(cleanedWord, " ")
                textBuilder.append(" $cleanedWord")
            }
        }

        val textBuilder2 = StringBuilder()
        for (word in textBuilder.split(" ")) {
            if (word.length > 1 || word == "#") {
                textBuilder2.append(" $word")
            }
        }

        return textBuilder2.toString().trim()
    }

    fun formatTimestamp(timestamp: Long, format: String): String {
        if (format.isBlank())
            return ""
        val timeZoneId = TimeZone.getDefault().toZoneId()
        val date = Instant.ofEpochMilli(timestamp).atZone(timeZoneId)
        return DateTimeFormatter.ofPattern(format.trim()).format(date)
    }

    fun getStringForTimestamp(timestamp: Long, showTime: Boolean = false): String {
        val dayMillis = CommonEnums.Intervals.Day.millis(1)

        val currTime = OffsetDateTime.now()
        val timeStampLastMidnight =
            currTime.toLocalDate().atTime(LocalTime.MIDNIGHT)
                .toEpochSecond(currTime.offset) * CommonEnums.Intervals.Second.millis(1)

        var prefix = ""
        var timeAlreadyAdded = false
        var format = when {
            timestamp >= timeStampLastMidnight -> {
                timeAlreadyAdded = true
                "hh:mm a"
            }
            timestamp >= (timeStampLastMidnight - dayMillis) -> {
                prefix = "Yesterday"
                ""
            }
            timestamp >= (timeStampLastMidnight - (6 * dayMillis)) -> {
                "EEE"
            }
            else -> {
                "dd/MM/yy"
            }
        }
        if (showTime && !timeAlreadyAdded) format += " hh:mm a"

        return "$prefix ${formatTimestamp(timestamp, format)}".trim()
    }

    fun checkPermissions(context: Context, permissions: Array<String>): Array<String> {
        return permissions.filter { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    fun copyToClipboard(context: Context, label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, content)
        clipboard.setPrimaryClip(clip)
    }

    fun showAlert(
        activity: Activity,
        title: String,
        message: String?,
        positiveBtnText: String,
        negativeBtnText: String,
        neutralBtnText: String?,
        positiveBtnCallback: () -> Unit,
        negativeBtnCallback: () -> Unit,
        neutralBtnCallback: () -> Unit
    ) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        if (message != null) {
            builder.setMessage(message)
        }
        builder.setPositiveButton(positiveBtnText) { _, _ -> positiveBtnCallback() }
        builder.setNegativeButton(negativeBtnText) { _, _ -> negativeBtnCallback() }
        if (neutralBtnText != null) {
            builder.setNeutralButton(neutralBtnText) { _, _ -> neutralBtnCallback() }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun highlightText(
        textView: TextView,
        text: String,
        substring: String,
        color: Int
    ) {
        var startPos = text.indexOf(substring, ignoreCase = true)
        if (startPos > -1) {

            var textToShow = text
            if (startPos > 30) {
                textToShow = "..." + text.substring(startIndex = startPos - 10)
                startPos = 13
            }

            val spannableString = SpannableString(textToShow)
            spannableString.setSpan(
                ForegroundColorSpan(color),
                startPos,
                startPos + substring.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = spannableString
        }
    }

    fun dbToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun isAndroidVersionAtLeast(sdkVersion: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdkVersion
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun base64Encode(text: String): String {
        return String(Base64.encode(text.toByteArray(), Base64.DEFAULT))
    }
}