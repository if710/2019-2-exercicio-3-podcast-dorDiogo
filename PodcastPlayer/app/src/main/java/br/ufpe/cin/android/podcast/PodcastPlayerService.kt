package br.ufpe.cin.android.podcast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.jetbrains.anko.doAsync
import java.io.File

class PodcastPlayerService : Service() {
    private val TAG = "PodcastPlayerService"
    private var mPlayer: MediaPlayer? = null
    private var currentMedia: ItemFeed? = null
    private val mBinder = MusicBinder()

    override fun onCreate() {
        super.onCreate()

        // configurar media player
        mPlayer = MediaPlayer()

        mPlayer?.setOnCompletionListener {
            erasePodcastFile()
        }

        createChannel()
        // cria notificacao na area de notificacoes para usuario voltar p/ Activity
        val notificationIntent =
            Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(
            applicationContext, "1"
        )
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true).setContentTitle("Podcast Service rodando")
            .setContentText("Clique para acessar o player!")
            .setContentIntent(pendingIntent).build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onDestroy() {
        savePodcastPlayblackPosition()
        mPlayer?.release()
        super.onDestroy()
    }

    fun togglePlayback(itemFeed: ItemFeed) {
        doAsync {
            if (currentMedia != itemFeed) {
                savePodcastPlayblackPosition()
                ChangePodcast(itemFeed)
            }
            if (!mPlayer!!.isPlaying) {
                mPlayer?.start()
            } else {
                mPlayer?.pause()
            }
            itemFeed.isPlaying = mPlayer!!.isPlaying
            ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().updateItemFeeds(itemFeed)
            LocalBroadcastManager.getInstance(this@PodcastPlayerService)
                .sendBroadcast(Intent(MainActivity.ITEM_UPDATED))
        }
    }

    fun ChangePodcast(itemFeed: ItemFeed) {
        currentMedia = itemFeed
        mPlayer?.reset()
        mPlayer?.setDataSource(itemFeed.filePath)
        mPlayer?.prepare()
        mPlayer?.seekTo(itemFeed.playbackPosition)
    }

    fun savePodcastPlayblackPosition() {
        currentMedia?.playbackPosition = mPlayer!!.currentPosition
        currentMedia?.isPlaying = false
        val itemFeed = currentMedia
        if (itemFeed != null) {
            ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().updateItemFeeds(itemFeed)
        }
    }

    fun erasePodcastFile() {
        doAsync {
            File(currentMedia!!.filePath!!).delete()
            currentMedia!!.filePath = null
            currentMedia!!.playbackPosition = 0
            currentMedia!!.isPlaying = false
            ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().updateItemFeeds(currentMedia!!)
            LocalBroadcastManager.getInstance(this@PodcastPlayerService)
                .sendBroadcast(Intent(MainActivity.ITEM_UPDATED))
        }
    }

    inner class MusicBinder : Binder() {
        internal val service: PodcastPlayerService
            get() = this@PodcastPlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val mChannel = NotificationChannel(
                "1",
                "Canal de Notificacoes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mChannel.description = "Podcast player"
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    companion object {
        private val NOTIFICATION_ID = 1
    }

}
