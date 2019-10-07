package br.ufpe.cin.android.podcast

import android.app.IntentService
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class EpisodeDownloadService : IntentService("EpisodeDownloadService") {

    public override fun onHandleIntent(i: Intent?) {
        try {
            val root = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            root?.mkdirs()
            val output = File(root, i!!.data!!.lastPathSegment)
            if (output.exists()) {
                output.delete()
            }
            val url = URL(i.data!!.toString())
            val c = url.openConnection() as HttpURLConnection
            val fos = FileOutputStream(output.path)
            val out = BufferedOutputStream(fos)
            try {
                val `in` = c.inputStream
                val buffer = ByteArray(8192)
                var len = 0
                len = `in`.read(buffer)
                while (len >= 0) {
                    out.write(buffer, 0, len)
                    len = `in`.read(buffer)
                }
                out.flush()
            } finally {
                fos.fd.sync()
                out.close()
                c.disconnect()
            }

            val itemFeedDAO = ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO()
            val itemFeed = itemFeedDAO.getItemFeed(url.toString())
            itemFeed.filePath = output.path
            itemFeedDAO.updateItemFeeds(itemFeed)

            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainActivity.ITEM_UPDATED))


        } catch (e2: IOException) {
            Log.e(javaClass.name, "Exception durante download", e2)
        }

    }
}
