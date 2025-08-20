package com.example.grandmavideoplayer

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var hintText: TextView
    private var player: ExoPlayer? = null
    private var currentPosition = 0
    private var currentAttachedPlayerView: PlayerView? = null
    private val videoUris = mutableListOf<Uri>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadVideosAndStart()
        } else {
            hintText.text = "需要读取视频权限才能播放本地视频"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        hintText = findViewById(R.id.hintText)

        viewPager.adapter = VideoAdapter()
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                attachPlayerTo(position)
                playCurrent()
            }
        })

        // Tap to pause/play
        viewPager.getChildAt(0)?.setOnClickListener {
            val p = player ?: return@setOnClickListener
            p.playWhenReady = !p.isPlaying
        }

        ensurePermissionThenStart()
    }

    private fun ensurePermissionThenStart() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadVideosAndStart()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadVideosAndStart() {
        videoUris.clear()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " ASC"

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                videoUris.add(contentUri)
            }
        }

        if (videoUris.isEmpty()) {
            hintText.text = "未找到本地视频，请将视频拷贝到手机（如“Movies/”文件夹）后再打开应用。"
            return
        }

        (viewPager.adapter as VideoAdapter).submitCount(videoUris.size)

        // Init player and attach to first page
        player = ExoPlayer.Builder(this).build()
        viewPager.post {
            attachPlayerTo(0)
            playCurrent()
        }
    }

    private fun attachPlayerTo(position: Int) {
        val holder = (viewPager.getChildAt(0) as? RecyclerView)?.findViewHolderForAdapterPosition(position) as? VideoViewHolder
        val pv = holder?.playerView ?: return
        if (currentAttachedPlayerView === pv) return

        currentAttachedPlayerView?.player = null
        pv.player = player
        currentAttachedPlayerView = pv
    }

    private fun playCurrent() {
        val p = player ?: return
        if (videoUris.isEmpty()) return
        val uri = videoUris[currentPosition]
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.playWhenReady = true
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        currentAttachedPlayerView?.player = null
        player?.release()
        player = null
    }

    inner class VideoAdapter : RecyclerView.Adapter<VideoViewHolder>() {
        private var count = 0
        fun submitCount(c: Int) {
            count = c
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VideoViewHolder {
            val view = layoutInflater.inflate(R.layout.item_video, parent, false) as PlayerView
            return VideoViewHolder(view)
        }
        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            // nothing here; player attached on page selected
        }
        override fun getItemCount(): Int = count
    }

    inner class VideoViewHolder(val playerView: PlayerView) : RecyclerView.ViewHolder(playerView)
}
