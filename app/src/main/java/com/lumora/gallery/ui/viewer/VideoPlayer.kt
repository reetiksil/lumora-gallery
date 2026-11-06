package com.lumora.gallery.ui.viewer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lumora.gallery.ui.theme.PureBlack
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    isMutedDefault: Boolean = true,
    isPlayingState: Boolean = true,
    isMutedState: Boolean = true,
    playbackSpeedState: Float = 1.0f,
    seekToFraction: Float? = null,
    onProgressUpdate: (currentMs: Long, totalMs: Long, isPlaying: Boolean) -> Unit = { _, _, _ -> },
    onToggleControls: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (isMutedDefault) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(isPlayingState) {
        if (isPlayingState) {
            if (!exoPlayer.isPlaying) exoPlayer.play()
        } else {
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
    }

    LaunchedEffect(isMutedState) {
        exoPlayer.volume = if (isMutedState) 0f else 1f
    }

    LaunchedEffect(playbackSpeedState) {
        exoPlayer.setPlaybackSpeed(playbackSpeedState)
    }

    LaunchedEffect(seekToFraction) {
        seekToFraction?.let { fraction ->
            val duration = exoPlayer.duration.coerceAtLeast(0L)
            if (duration > 0) {
                exoPlayer.seekTo((fraction * duration).toLong())
            }
        }
    }

    // Periodic time poll to feed parent scrubber
    LaunchedEffect(exoPlayer) {
        while (true) {
            val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            onProgressUpdate(cur, dur, exoPlayer.isPlaying)
            delay(250)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration, playing)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleControls
            )
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
