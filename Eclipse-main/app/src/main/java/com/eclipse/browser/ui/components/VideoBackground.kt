package com.eclipse.browser.ui.components

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.media.MediaPlayer
import java.io.File

@Composable
fun VideoBackground(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared = remember { false }

    DisposableEffect(videoPath) {
        try {
            val file = when {
                videoPath.startsWith("content://") -> {
                    // For content URIs, copy to temp file first
                    val uri = Uri.parse(videoPath)
                    val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                videoPath.startsWith("file://") -> {
                    File(videoPath.removePrefix("file://"))
                }
                else -> File(videoPath)
            }

            if (file.exists()) {
                mediaPlayer.setDataSource(file.absolutePath)
                mediaPlayer.isLooping = true
                mediaPlayer.setOnPreparedListener { mp ->
                    mp.start()
                    isPrepared = true
                }
                mediaPlayer.prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            mediaPlayer.setDisplay(holder)
                            if (isPrepared && !mediaPlayer.isPlaying) {
                                mediaPlayer.start()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
            }
        },
        modifier = modifier,
        update = { view ->
            view.visibility = View.VISIBLE
        }
    )
}
