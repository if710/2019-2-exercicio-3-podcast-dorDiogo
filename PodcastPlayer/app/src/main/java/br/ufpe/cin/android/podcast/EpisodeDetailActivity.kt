package br.ufpe.cin.android.podcast

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_episode_detail.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.uiThread
import java.net.URL


class EpisodeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode_detail)

        doAsync {
            val episodeGuid = intent.getStringExtra("guid")
            val itemFeed = ItemFeedDB.getDatabase(this@EpisodeDetailActivity).ItemFeedDAO()
                .getItemFeed(episodeGuid)
            val bmp =
                BitmapFactory.decodeStream(URL(itemFeed.imageLink).openConnection().getInputStream())
            uiThread {
                ep_title.text = itemFeed.title
                ep_date.text = itemFeed.pubDate
                ep_image.imageBitmap = bmp
                ep_description.text = itemFeed.description
                ep_description.movementMethod = ScrollingMovementMethod()
            }
        }
    }
}
