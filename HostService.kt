package com.juligram.chess

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import fi.iki.elonen.NanoHTTPD
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class HostService : Service() {
    private var server: RoomServer? = null
    private var pages: PageServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "juligram_host"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Hosting", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Juligram Chess")
            .setContentText("Hosting a game")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notification)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "juligram:host").apply { acquire() }

        if (server == null) server = RoomServer(8080).also { it.start() }
        if (pages == null) pages = PageServer(this).also { it.start() }
        return START_STICKY
    }

    override fun onDestroy() {
        try { server?.stop() } catch (e: Exception) {}
        server = null
        pages?.stop()
        pages = null
        wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class RoomServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {}
    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {}
    override fun onMessage(conn: WebSocket, message: String) {
        for (other in connections) {
            if (other != conn) other.send(message)
        }
    }
    override fun onError(conn: WebSocket?, ex: Exception) {}
    override fun onStart() { connectionLostTimeout = 30 }
}

class PageServer(private val context: Context) : NanoHTTPD(8081) {
    override fun serve(session: IHTTPSession): Response {
        return try {
            val html = context.assets.open("index.html").bufferedReader().readText()
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
        }
    }
}
