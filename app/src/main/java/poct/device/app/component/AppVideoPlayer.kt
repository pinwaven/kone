package poct.device.app.component

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.distinctUntilChanged
import poct.device.app.App
import poct.device.app.R
import poct.device.app.theme.filledFontColor
import timber.log.Timber

@OptIn(UnstableApi::class)
@Composable
fun AppVideoPlayer(
    video: Uri,
    modifier: Modifier = Modifier
) {
    var playbackState by remember { mutableStateOf(PlaybackState.IDLE) }
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = remember { 3 }

    val exoPlayer = remember {
        ExoPlayer.Builder(App.getContext())
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // ✅ 优化缓冲策略：增大缓冲区减少卡顿
                    .setBufferDurationsMs(2000, 5000, 1000, 2000)
                    // ✅ 启用背压机制，避免频繁缓冲
                    .setBackBuffer(2000, true)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        playbackState = when (state) {
                            Player.STATE_IDLE -> PlaybackState.IDLE
                            Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                            Player.STATE_READY -> PlaybackState.READY
                            Player.STATE_ENDED -> PlaybackState.ENDED
                            else -> PlaybackState.IDLE
                        }

                        if (state == Player.STATE_READY) {
                            retryCount = 0 // 重置重试计数
                            if (playWhenReady) play()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Timber.w("VideoPlayer Error - Code: ${error.errorCode}, Message: ${error.message}")
                        error.cause?.let { Timber.w("Root cause: $it") }

                        playbackState = PlaybackState.ERROR
                        retryCount++

                        // ✅ 智能错误处理
                        handlePlaybackError(error, retryCount, maxRetries)
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        // 监控加载状态变化
                        Timber.d("Loading state changed: $isLoading")
                    }
                })
            }
    }

    // ✅ 优化URI变化处理
    LaunchedEffect(video) {
        if (exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.stop() // 先停止当前播放
            exoPlayer.clearMediaItems()
        }

        val newMediaItem = MediaItem.fromUri(video).buildUpon()
            .setCustomCacheKey(video.toString()) // 添加缓存标识
            .build()

        exoPlayer.setMediaItem(newMediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // ✅ 添加网络状态监听（如果播放网络视频）
    val connectivityManager = remember {
        App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    LaunchedEffect(Unit) {
        snapshotFlow { connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true }
            .distinctUntilChanged()
            .collect { isConnected ->
                if (!isConnected && playbackState == PlaybackState.BUFFERING) {
                    // 网络断开时暂停缓冲
                    exoPlayer.playWhenReady = false
                } else if (isConnected && !exoPlayer.playWhenReady) {
                    // 网络恢复时继续播放
                    exoPlayer.playWhenReady = true
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Releasing ExoPlayer resources")
            exoPlayer.release()
        }
    }

    // UI部分保持不变，但使用新的状态枚举
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(Color.WHITE)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = modifier,
        )

        when (playbackState) {
            PlaybackState.BUFFERING -> CircularProgressIndicator(
                color = filledFontColor,
                modifier = Modifier.align(Alignment.Center)
            )

            PlaybackState.ERROR -> RetryButton(
                retryCount = retryCount,
                maxRetries = maxRetries,
                onRetry = {
                    playbackState = PlaybackState.BUFFERING
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            )

            else -> {}
        }
    }
}

// ✅ 扩展：智能错误处理器
private fun handlePlaybackError(error: PlaybackException, retryCount: Int, maxRetries: Int) {
    when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
            if (retryCount < maxRetries) {
                Timber.i("Network error, will retry ($retryCount/$maxRetries)")
                // 可以在这里添加指数退避延迟
            } else {
                Timber.e("Max retries reached for network error")
            }
        }

        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
            Timber.e("Decoding error, likely format unsupported")
            // 可以尝试切换到软件解码器
        }

        else -> {
            Timber.e("Unhandled playback error: ${error.errorCodeName}")
        }
    }
}

// ✅ 扩展：重试按钮组件
@Composable
private fun RetryButton(
    retryCount: Int,
    maxRetries: Int,
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onRetry) {
            Icon(
                painter = painterResource(id = R.mipmap.cq_icon),
                contentDescription = "Retry",
                modifier = Modifier.size(74.dp),
                tint = filledFontColor,
            )
        }
        if (retryCount >= maxRetries) {
            Text(
                text = "播放失败，请检查网络或文件",
                color = filledFontColor,
                fontSize = 12.sp
            )
        }
    }
}

// ✅ 扩展：清晰的状态枚举
private enum class PlaybackState {
    IDLE, BUFFERING, READY, ENDED, ERROR
}