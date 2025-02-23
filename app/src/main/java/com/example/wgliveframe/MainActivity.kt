/*
 * Copyright (C) 2023 Pangeran Wiguan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Pangeran Wiguan
 * Email: pangeranwiguan@gmail.com
 * Since: 1.0.0
 */

package com.example.wgliveframe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult

/**
 * Main entry point for the WG LiveFrame app.
 *
 * @author Pangeran Wiguan
 * @email pangeranwiguan@gmail.com
 * @since 1.0.0
 */
class MainActivity : ComponentActivity() {

    // Stores the selected video URI
    private var videoUri: Uri? = null

    /**
     * Called when the activity is created.
     * Sets up the Jetpack Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WGTheme {
                MainScreen(
                    videoUri = videoUri,
                    onVideoSelected = { uri -> videoUri = uri }
                )
            }
        }
    }
}

/**
 * Wraps the app's UI with a Material Design 3 theme.
 *
 * @param content The main content of the app.
 */
@Composable
fun WGTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF6200EE)),
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

/**
 * The main screen of the app.
 *
 * Displays buttons for video selection, preview, and setting wallpapers.
 *
 * @param videoUri The URI of the selected video.
 * @param onVideoSelected Callback to update the video URI when a video is selected.
 */
@Composable
fun MainScreen(
    videoUri: Uri?,
    onVideoSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

    // Activity Result Launcher for selecting a video from the device
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onVideoSelected(uri) // Update the video URI when a video is selected
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title of the app
            Text(
                text = "WG LiveFrame",
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Button to select a video
            Button(onClick = {
                videoPickerLauncher.launch("video/*")
            }) {
                Text("Select Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview the selected video
            if (videoUri != null) {
                VideoPreview(videoUri)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button to set the video as a live wallpaper
            Button(
                onClick = {
                    if (videoUri != null) {
                        setVideoAsLiveWallpaper(context, videoUri)
                    }
                },
                enabled = videoUri != null
            ) {
                Text("Set as Home Wallpaper")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button to set the video as a lock screen wallpaper (placeholder)
            Button(
                onClick = { /* Implement lock screen functionality */ },
                enabled = videoUri != null
            ) {
                Text("Set as Lock Screen Wallpaper")
            }
        }
    }
}

/**
 * Displays a preview of the selected video using a VideoView.
 *
 * @param videoUri The URI of the video to preview.
 */
@Composable
fun VideoPreview(videoUri: Uri) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(videoUri)
                start()
            }
        },
        modifier = Modifier.size(200.dp)
    )
}

/**
 * Sets the selected video as a live wallpaper.
 *
 * @param context The application context.
 * @param videoUri The URI of the video to set as a live wallpaper.
 */
private fun setVideoAsLiveWallpaper(context: android.content.Context, videoUri: Uri) {
    val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            android.content.ComponentName(context, VideoLiveWallpaperService::class.java)
        )
        putExtra("videoUri", videoUri.toString())
    }
    context.startActivity(intent)
}

/**
 * Custom live wallpaper service to play the selected video.
 *
 * @author Pangeran Wiguan
 * @email pangeranwiguan@gmail.com
 * @since 1.0.0
 */
// Custom Live Wallpaper Service
class VideoLiveWallpaperService : WallpaperService() {

    private var serviceIntent: Intent? = null

    /**
     * Called when the service is created.
     * Passes the intent to the VideoEngine.
     */
    override fun onCreateEngine(): Engine {
        return VideoEngine(serviceIntent)
    }

    /**
     * Handles incoming intents.
     * Stores the intent for use in the VideoEngine.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceIntent = intent
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Inner class representing the live wallpaper engine.
     */
    inner class VideoEngine(private val serviceIntent: Intent?) : Engine() {
        private lateinit var mediaPlayer: MediaPlayer

        /**
         * Called when the engine is created.
         * Initializes the MediaPlayer to play the selected video.
         */
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Retrieve the video URI from the intent
            val videoUriString = serviceIntent?.getStringExtra("videoUri")
            if (!videoUriString.isNullOrEmpty()) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(videoUriString))
                    setSurface(surfaceHolder.surface)
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                println("Error: No video URI provided.")
            }
        }

        /**
         * Releases resources when the engine is destroyed.
         */
        override fun onDestroy() {
            super.onDestroy()
            mediaPlayer.release()
        }
    }
}