package lol.omnius.android.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra("stream_url") ?: run {
            finish()
            return
        }
        val title = intent.getStringExtra("title") ?: ""

        playerView = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 5000
            controllerAutoShow = true
        }
        setContentView(playerView)

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView?.player = exo
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                player?.let {
                    it.playWhenReady = !it.playWhenReady
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                player?.let { it.seekTo(it.currentPosition - 10_000) }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                player?.let { it.seekTo(it.currentPosition + 10_000) }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
