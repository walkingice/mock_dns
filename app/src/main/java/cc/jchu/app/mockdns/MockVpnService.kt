@file:Suppress("unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "")

package cc.jchu.app.mockdns

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MockVpnService : VpnService() {

    private var binder: MyBinder? = null
    private var fd: ParcelFileDescriptor? = null
    private val ioCoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d(TAG, "service on create")
        binder = MyBinder(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d(TAG, "service on destroy")
        binder = null
        releaseResource()
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action ?: return START_STICKY
        val dnsServerIp = intent.getStringExtra(EXTRA_DNS_SERVER_IP) ?: DEFAULT_DNS_SERVER_IP

        when (action) {
            ACTION_START_TASK -> startTask(dnsServerIp)
            ACTION_FINISH_TASK -> finishTaskAndStopService()
            else -> Unit
        }
        return START_STICKY
    }

    private fun startTask(dnsServerIp: String) {
        moveStartedServiceToForeground()
        ioCoroutineScope.launch {
            buildTunnel(dnsServerIp)
        }
    }

    private fun finishTaskAndStopService() {
        stopForeground(true)
        stopSelf()
        releaseResource()
    }

    private fun releaseResource() {
        fd?.close()
        fd = null
    }

    private fun moveStartedServiceToForeground() {
        val builder = createNotificationBuilder(applicationContext)
            .setContentTitle("Mock VPN service")
            .setContentInfo("change network dns setting")
            .setContentIntent(buildPendingIntentForStopService())
        // add a button to Notification
        builder.addAction(
            android.R.drawable.ic_menu_delete,
            "Stop",
            buildPendingIntentForStopService()
        )

        startForeground(NOTIFICATION_ID, builder.build())
    }

    // TODO: better implementation
    // Not fully understand this implementation. Need more investigation
    private suspend fun buildTunnel(dnsServerIp: String) = withContext(Dispatchers.IO) {
        val builder: VpnService.Builder = Builder()
        try {
            // FIXME: 不知為何，改成 192.168.142.100 就不會動了
            val virtualInterfaceIp = "192.168.2.2"

            builder.setSession("MockVpnServiceSession")
                .addAddress(virtualInterfaceIp, 24)
                .addDnsServer(dnsServerIp)
            fd = builder.establish()
            //val tunnel = DatagramChannel.open()
            //tunnel.connect(InetSocketAddress("127.0.0.1", 8087))
            //protect(tunnel.socket())
        } catch (e: Exception) {
            finishTaskAndStopService()
        }
    }

    private fun buildPendingIntentForStopService(): PendingIntent {
        val intent = buildMessageIntentForService(this, false)
        return PendingIntent.getService(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationBuilder(context: Context): NotificationCompat.Builder {
        val bitmap = BitmapFactory.decodeResource(
            context.resources,
            android.R.drawable.ic_menu_directions
        )
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setLargeIcon(bitmap)
                .setColor(ContextCompat.getColor(context, android.R.color.black))
                .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(false)
        }
        return builder
    }

    class MyBinder(private val service: MockVpnService) : Binder() {
        fun getMyService(): MockVpnService {
            return service
        }
    }

    companion object {

        private const val TAG = "MockVpn"
        private const val ACTION_START_TASK = "_action_start_task"
        private const val ACTION_FINISH_TASK = "_action_finish_task"
        private const val EXTRA_DNS_SERVER_IP = "_extra_dns_server_ip_"
        private const val NOTIFICATION_ID: Int = 0x4242
        private const val NOTIFICATION_CHANNEL_ID: String = "notification_channel_of_mock_server"

        private const val DEFAULT_DNS_SERVER_IP = "192.168.142.102"

        private fun buildLaunchIntent(applicationContext: Context): Intent {
            return Intent(applicationContext, MockVpnService::class.java)
        }

        fun buildMessageIntentForService(
            applicationContext: Context,
            isStartService: Boolean
        ): Intent {
            val intent = buildLaunchIntent(applicationContext)
            if (isStartService) {
                intent.action = ACTION_START_TASK
            } else {
                intent.action = ACTION_FINISH_TASK
            }
            return intent
        }

        fun createNotificationChannel(context: Context) {
            createNotificationChannel(
                context,
                NOTIFICATION_CHANNEL_ID,
                "Channel Name",
                NotificationManagerCompat.IMPORTANCE_LOW
            )
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createNotificationChannel(
            context: Context,
            channelId: String,
            channelName: String,
            importance: Int
        ) {
            val mgr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, importance)
            mgr.createNotificationChannel(channel)
        }

        fun startMockService(context: Context, dnsServerIp: String) {
            val serviceIntent = buildMessageIntentForService(context, true)
            serviceIntent.putExtra(EXTRA_DNS_SERVER_IP, dnsServerIp)
            context.startService(serviceIntent)
        }

        fun stopMockService(context: Context) {
            val serviceIntent = buildMessageIntentForService(context, false)
            context.startService(serviceIntent)
        }
    }
}
