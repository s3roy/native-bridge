package com.bridge

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

/**
 * OPTIONAL — only if you use Firebase Cloud Messaging and want push notifications
 * auto-captured for the web side. Register in your AndroidManifest.xml:
 *
 *   <service android:name="com.bridge.BridgeMessagingService"
 *            android:exported="false">
 *     <intent-filter>
 *       <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 *   </service>
 *
 * If you already have your own messaging service, just call
 * NativeBridge.recordNotification(...) from your onMessageReceived instead.
 */
open class BridgeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        NativeBridge.recordNotification(
            NotificationRecord(
                id = message.messageId ?: UUID.randomUUID().toString(),
                title = message.notification?.title,
                body = message.notification?.body,
                data = message.data,
                timestamp = System.currentTimeMillis(),
            )
        )
        super.onMessageReceived(message)
    }
}
