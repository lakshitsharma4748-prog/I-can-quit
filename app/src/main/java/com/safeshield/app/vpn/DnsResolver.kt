package com.safeshield.app.vpn

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Forwards an allowed DNS query to a real upstream resolver and returns the
 * raw response bytes. The socket is created fresh per query rather than
 * pooled — the Phase 2 MVP favors a simple, obviously-correct
 * implementation over the added complexity of connection reuse; revisit if
 * per-query socket setup shows up as a real latency problem.
 *
 * [protectSocket] must call [android.net.VpnService.protect] on the socket
 * so its traffic bypasses the VPN's own routes instead of looping back into
 * itself — that wiring lives in [SafeShieldVpnService] since only a
 * VpnService instance can call `protect()`.
 */
class DnsResolver(
    private val protectSocket: (DatagramSocket) -> Unit,
    private val upstreamDnsServer: InetAddress,
    private val timeoutMillis: Int = 5_000
) {

    /** Returns the raw upstream response, or null on any failure (timeout, IO error, closed socket). */
    fun forward(query: ByteArray, upstreamPort: Int = STANDARD_DNS_PORT): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket().apply {
                protectSocket(this)
                soTimeout = timeoutMillis
            }
            socket.send(DatagramPacket(query, query.size, upstreamDnsServer, upstreamPort))

            val buffer = ByteArray(MAX_DNS_MESSAGE_SIZE)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            responsePacket.data.copyOfRange(0, responsePacket.length)
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Upstream DNS query timed out")
            null
        } catch (e: IOException) {
            Log.w(TAG, "Upstream DNS query failed: ${e.javaClass.simpleName}")
            null
        } finally {
            socket?.close()
        }
    }

    companion object {
        private const val TAG = "SafeShield/DnsResolver"
        private const val STANDARD_DNS_PORT = 53
        private const val MAX_DNS_MESSAGE_SIZE = 512 // plain UDP DNS (no EDNS0); sufficient for the MVP's simple A/AAAA lookups

        /** Public DNS resolver used as the default upstream — no user browsing data is logged or stored locally either way. */
        val DEFAULT_UPSTREAM: InetAddress by lazy { InetAddress.getByName("8.8.8.8") }
    }
}
