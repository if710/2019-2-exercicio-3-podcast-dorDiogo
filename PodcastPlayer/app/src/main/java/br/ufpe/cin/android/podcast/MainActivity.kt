package br.ufpe.cin.android.podcast

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL

class MainActivity : AppCompatActivity() {

    internal var podcastPlayerService: PodcastPlayerService? = null
    internal var isBound = false

    private val sConn = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            podcastPlayerService = null
            isBound = false
        }

        override fun onServiceConnected(p0: ComponentName?, b: IBinder?) {
            val binder = b as PodcastPlayerService.MusicBinder
            podcastPlayerService = binder.service
            isBound = true
        }

    }

    private val onUpdateItemFeedEvent = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, i: Intent) {
            doAsync {
                val itemFeeds =
                    ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().getAllItemFeeds()
                uiThread {
                    xmlDataView.adapter = xmlDataViewAdapter(itemFeeds, podcastPlayerService)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, PodcastPlayerService::class.java))

        doAsync {
            DownloadRssFeed()

            val itemFeeds =
                ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().getAllItemFeeds()
            uiThread {
                LoadUi(itemFeeds)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isBound) {
            val bindIntent = Intent(this, PodcastPlayerService::class.java)
            isBound = bindService(bindIntent, sConn, Context.BIND_AUTO_CREATE)
            isBound = true
        }
    }

    override fun onStop() {
        if (isBound) {
            unbindService(sConn)
            isBound = false
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val f = IntentFilter(ITEM_UPDATED)
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(onUpdateItemFeedEvent, f)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(onUpdateItemFeedEvent)
    }

    fun DownloadRssFeed() {
        val xmlString: String
        try {
            xmlString = URL(getString(R.string.feed_xml_link)).readText()
        } catch (e: Exception) {
            return
        }
        val parsedXml = Parser.parse(xmlString)
        parsedXml.forEach {
            ItemFeedDB.getDatabase(applicationContext).ItemFeedDAO().insertItemFeeds(it)
        }
    }

    fun LoadUi(itemFeeds: List<ItemFeed>) {
        xmlDataView.layoutManager = LinearLayoutManager(this@MainActivity)
        xmlDataView.adapter = xmlDataViewAdapter(itemFeeds, podcastPlayerService)
        xmlDataView.addItemDecoration(
            DividerItemDecoration(
                this@MainActivity,
                LinearLayoutManager.VERTICAL
            )
        )
    }

    companion object {

        val ITEM_UPDATED = "br.ufpe.cin.android.podcast.action.ITEM_UPDATED"
    }
}
