package com.safeshield.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.safeshield.app.MainActivity
import com.safeshield.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * The VPN MVP (PRD Phase 2): establishes a TUN interface that captures only
 * DNS traffic (via a virtual DNS server address), classifies each query
 * against [DomainFilter], answers blocked queries with a synthetic
 * NXDOMAIN, and forwards everything else to a real upstream resolver.
 *
 * Deliberately out of scope here: anything beyond DNS-based filtering.
 * Non-DNS traffic is never routed into this VPN at all (see the `Builder`
 * routes below) — it continues over the device's normal network path
 * untouched, which is also why this service never sees, and therefore
 * never could log or store, actual browsing traffic.
 */
class SafeShieldVpnService : VpnService() {

    private val domainFilter = DomainFilter()
    private val packetProcessor = PacketProcessor(domainFilter)
    private val dnsResolver = DnsResolver(
        protectSocket = { socket -> protect(socket) },
        upstreamDnsServer = DnsResolver.DEFAULT_UPSTREAM
    )

    private var tunInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var packetLoopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    /** Called by Android if the user revokes VPN consent (e.g. from system Settings, or another VPN app takes over). */
    override fun onRevoke() {
        Log.i(TAG, "VPN permission revoked by the system")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startVpn() {
        if (tunInterface != null) return // already running

        VpnConnectionState.update(VpnState.CONNECTING)
        val establishedInterface = try {
            Builder()
                .setSession(getString(R.string.app_name))
                .addAddress(VPN_INTERFACE_ADDRESS, 32)
                .addDnsServer(VPN_INTERFACE_ADDRESS)
                .addRoute(VPN_INTERFACE_ADDRESS, 32) // only DNS traffic is captured; everything else bypasses this VPN
                .establish()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to establish VPN interface: ${e.javaClass.simpleName}")
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "VPN permission not granted: ${e.javaClass.simpleName}")
            null
        }

        if (establishedInterface == null) {
            VpnConnectionState.update(VpnState.ERROR)
            stopSelf()
            return
        }

        tunInterface = establishedInterface
        startForeground(NOTIFICATION_ID, buildNotification())
        VpnConnectionState.update(VpnState.CONNECTED)
        packetLoopJob = serviceScope.launch { runPacketLoop(establishedInterface) }
    }

    private fun stopVpn() {
        val job = packetLoopJob
        packetLoopJob = null
        if (job != null) {
            runBlocking { job.cancelAndJoin() }
        }
        tunInterface?.let { fd ->
            try {
                fd.close()
            } catch (e: IOException) {
                Log.w(TAG, "Error closing TUN interface: ${e.javaClass.simpleName}")
            }
        }
        tunInterface = null
        VpnConnectionState.update(VpnState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun runPacketLoop(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(MAX_PACKET_SIZE)

        try {
            while (currentCoroutineContext().isActive) {
                val length = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    Log.w(TAG, "TUN read failed, stopping VPN: ${e.javaClass.simpleName}")
                    break
                }
                if (length <= 0) continue

                when (val decision = packetProcessor.process(buffer, length)) {
                    is PacketProcessor.Decision.Block -> writeSafely(output, decision.packet)
                    is PacketProcessor.Decision.Forward -> {
                        val response = dnsResolver.forward(decision.dnsQuery)
                        if (response != null) {
                            writeSafely(output, decision.wrapReply(response))
                        }
                        // No response (timeout/error): drop it. The requesting
                        // app's own resolver will simply time out and retry,
                        // same as on any lossy network — we don't fabricate a
                        // reply for a failure that isn't ours to interpret.
                    }
                    PacketProcessor.Decision.Drop -> Unit
                }
            }
        } finally {
            withContextSafeStop()
        }
    }

    private fun writeSafely(output: FileOutputStream, packet: ByteArray) {
        try {
            output.write(packet)
        } catch (e: IOException) {
            Log.w(TAG, "TUN write failed: ${e.javaClass.simpleName}")
        }
    }

    /** If the packet loop exits on its own (TUN closed underneath us), make sure state/foreground status still get cleaned up. */
    private fun withContextSafeStop() {
        if (tunInterface != null) {
            VpnConnectionState.update(VpnState.ERROR)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.vpn_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "SafeShield/VpnService"
        private const val NOTIFICATION_CHANNEL_ID = "safeshield_protection"
        private const val NOTIFICATION_ID = 1
        private const val MAX_PACKET_SIZE = 32_767

        /** RFC 5737 TEST-NET-1 range — never a real routable address, so it can't collide with anything on a real network. */
        private const val VPN_INTERFACE_ADDRESS = "192.0.2.1"

        private const val ACTION_START = "com.safeshield.app.vpn.action.START"
        private const val ACTION_STOP = "com.safeshield.app.vpn.action.STOP"

        /** Returns a consent intent to launch via `startActivityForResult`, or null if consent is already granted. */
        fun prepareIntent(context: Context): Intent? = prepare(context)

        fun start(context: Context) {
            context.startService(Intent(context, SafeShieldVpnService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, SafeShieldVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
