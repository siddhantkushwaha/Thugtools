package com.siddhantkushwaha.thugtools


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.siddhantkushwaha.thugtools.Util.checkPermissions
import java.io.InputStream


object TelephonyUtil {

    private val logger = LogUtil(this, true)

    data class SMSMessage(
        val id: Int,
        val threadId: Int,
        val user2: String,
        val timestamp: Long,
        val body: String,
        val type: Int,
        val subId: Int,
        val isRead: Boolean
    )

    data class ContactInfo(
        val id: Long,
        val number: String,
        val name: String,
        val label: String
    )

    data class SubscriptionInfo(
        val subId: Int,
        val number: String?,
        val carrierDisplayName: String,
        val carrierName: String,
        val slotIndex: Int
    )

    @SuppressLint("MissingPermission")
    fun getSubscriptions(context: Context): HashMap<Int, SubscriptionInfo>? {
        var subscriptions: HashMap<Int, SubscriptionInfo>? = null
        if (checkPermissions(context, arrayOf(Manifest.permission.READ_PHONE_STATE)).isEmpty()) {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            subscriptions = HashMap()
            subscriptionManager.activeSubscriptionInfoList?.forEach {
                val phoneNumber = it.number
                subscriptions[it.subscriptionId] =
                    SubscriptionInfo(
                        subId = it.subscriptionId,
                        number = Util.normalizePhoneNumber(phoneNumber),
                        carrierDisplayName = it.displayName?.toString() ?: "Unknown",
                        carrierName = it.carrierName?.toString() ?: "Unknown carrier",
                        slotIndex = it.simSlotIndex
                    )
            }
        }
        return subscriptions
    }

    fun getDefaultSMSSubscriptionId(): Int {
        var subId = SubscriptionManager.getDefaultSmsSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            subId = -1
        return subId
    }

    @SuppressLint("Range")
    fun getAllSms(
        context: Context,
        fromTimeMillis: Long = -1
    ): ArrayList<SMSMessage>? {

        var messages: ArrayList<SMSMessage>? = null
        if (checkPermissions(context, arrayOf(Manifest.permission.READ_SMS)).isEmpty()) {
            messages = ArrayList()
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                "${Telephony.Sms.DATE} >= $fromTimeMillis", // get messages after from time
                null,
                "${Telephony.Sms.DATE} DESC" // latest messages at the top
            )

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        val smsId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))

                        val threadId =
                            cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))

                        val user2: String =
                            cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                                ?: continue

                        val body: String =
                            cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))

                        // Epoch time in milliseconds
                        val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))

                        /*
                            1 - Received
                            2 - Sent
                        */
                        val type: Int =
                            cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                        val isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ))

                        val subId =
                            if (cursor.columnNames.find { it == Telephony.Sms.SUBSCRIPTION_ID } != null) {
                                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
                            } else {
                                cursor.getInt(cursor.getColumnIndex("sim_id"))
                            }

                        val message = SMSMessage(
                            threadId = threadId,
                            id = smsId,
                            user2 = user2,
                            timestamp = date,
                            body = body,
                            type = type,
                            subId = subId,
                            isRead = isRead == 1
                        )

                        // in the decreasing order of timestamps
                        messages.add(message)
                    } catch (exp: Exception) {
                        exp.printStackTrace()
                    }
                }
                cursor.close()
            }
        }
        return messages
    }

    fun saveSms(context: Context, smsMessage: SMSMessage): Int {
        val values = ContentValues()

        if (smsMessage.threadId > 0)
            values.put(Telephony.Sms.THREAD_ID, smsMessage.threadId)

        values.put(Telephony.Sms.ADDRESS, smsMessage.user2)
        values.put(Telephony.Sms.DATE, smsMessage.timestamp)
        values.put(Telephony.Sms.BODY, smsMessage.body)
        values.put(Telephony.Sms.TYPE, smsMessage.type)
        values.put(Telephony.Sms.SUBSCRIPTION_ID, smsMessage.subId)
        values.put(Telephony.Sms.READ, smsMessage.isRead)

        val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            ?: return 0

        val smsId = uri.toString().split("/").last().toInt()

        logger.debug("Message added at URI: $uri $smsId")

        return smsId
    }

    fun markMessageAsSendFailed(context: Context, smsId: Int): Int {
        val uri = Uri.parse("${Telephony.Sms.CONTENT_URI}/$smsId")

        val values = ContentValues()
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED)

        val numUpdated = context.contentResolver.update(
            uri,
            values,
            null,
            null
        )

        logger.debug("Message marked as failed at URI?: $uri $smsId $numUpdated")

        return numUpdated
    }

    fun markMessageAsSendSuccess(context: Context, smsId: Int): Int {
        val uri = Uri.parse("${Telephony.Sms.CONTENT_URI}/$smsId")

        val values = ContentValues()
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)

        val numUpdated = context.contentResolver.update(
            uri,
            values,
            null,
            null
        )

        logger.debug("Message marked as sent at URI?: $uri $smsId $numUpdated")

        return numUpdated
    }

    @SuppressLint("Range")
    fun getAllContacts(context: Context): HashMap<String, ContactInfo>? {
        var contactsList: HashMap<String, ContactInfo>? = null
        if (checkPermissions(context, arrayOf(Manifest.permission.READ_CONTACTS)).isEmpty()) {
            contactsList = HashMap()
            val contentResolver: ContentResolver = context.contentResolver
            val uri: Uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        val contactId =
                            cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID))
                        val phoneNumber: String =
                            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        val name: String =
                            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                        val type =
                            cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
                        val label =
                            when (type) {
                                ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> {
                                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL))
                                }
                                else -> {
                                    ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                        context.resources,
                                        type,
                                        "Unknown"
                                    ).toString()
                                }
                            }

                        logger.debug("Contact : $phoneNumber, $label")

                        if (Util.isProbablyAValidPhoneNumber(phoneNumber)) {
                            val phoneNumberNormalized = Util.normalizePhoneNumber(phoneNumber)
                                ?: throw Exception("Normalized number cannot be null for a valid number.")
                            contactsList[phoneNumberNormalized] =
                                ContactInfo(contactId, phoneNumber, name, label)
                        }
                    } catch (exp: Exception) {
                        exp.printStackTrace()
                    }
                }
                cursor.close()
            }
        }
        return contactsList
    }

    fun openContactPhoto(
        context: Context,
        contactId: Long,
        preferHighRes: Boolean
    ): InputStream? {
        val contactUri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        return ContactsContract.Contacts.openContactPhotoInputStream(
            context.contentResolver,
            contactUri,
            preferHighRes
        )
    }

    fun deleteSMS(context: Context, smsId: Int): Boolean {
        val uri = Uri.parse("${Telephony.Sms.CONTENT_URI}/$smsId")
        val numDeleted = context.contentResolver.delete(
            uri,
            null,
            null
        )
        return numDeleted > 0
    }

    fun markSmsRead(context: Context, smsId: Int): Boolean {
        val uri = Uri.parse("${Telephony.Sms.CONTENT_URI}/$smsId")
        val values = ContentValues()
        values.put(Telephony.Sms.READ, true)
        val numUpdated = context.contentResolver.update(uri, values, null, null)
        return numUpdated > 0
    }

    fun isDefaultSmsApp(context: Context): Boolean {
        return context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
    }

    fun call(activity: Activity, number: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$number")
        activity.startActivity(intent)
    }
}