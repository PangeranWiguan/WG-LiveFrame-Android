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
package com.pangeranwiguan.wgliveframe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Main entry point for the WG LiveFrame app.
 *
 * @author Pangeran Wiguan
 * @email pangeranwiguan@gmail.com
 * @since 1.0.0
 */
class MainActivity : ComponentActivity() {

    // Use mutableStateOf to make videoUri observable
    private var videoUri by mutableStateOf<Uri?>(null)

    /**
     * Called when the activity is created.
     * Sets up the Jetpack Compose UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions using the Activity Result API
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                println("Permission granted.")
            } else {
                println("Permission denied.")
                Toast.makeText(this, "Storage permission is required to select videos.", Toast.LENGTH_SHORT).show()
            }
        }

        // Check and request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Requesting READ_MEDIA_VIDEO permission.")
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                println("READ_MEDIA_VIDEO permission already granted.")
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Requesting READ_EXTERNAL_STORAGE permission.")
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                println("READ_EXTERNAL_STORAGE permission already granted.")
            }
        }

        setContent {
            WGTheme {
                MainScreen(
                    videoUri = videoUri,
                    onVideoSelected = { uri ->
                        videoUri = uri
                        println("Updated videoUri in MainActivity: $videoUri") // Log the updated URI
                    }
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
            println("Selected video URI: $uri") // Log the URI for debugging
            onVideoSelected(uri) // Update the video URI
        } else {
            println("No video selected.") // Handle cases where no video is selected
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
                enabled = videoUri != null // Enable only if videoUri is not null
            ) {
                Text("Set as Home Wallpaper")
            }

            Spacer(modifier = Modifier.height(8.dp))

// Button to set the video as a lock screen wallpaper (placeholder)
            Button(
                onClick = { /* Implement lock screen functionality */ },
                enabled = videoUri != null // Enable only if videoUri is not null
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
    // Save the video URI in SharedPreferences
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("videoUri", videoUri.toString()).apply()

    // Launch the live wallpaper picker
    val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            android.content.ComponentName(context, VideoLiveWallpaperService::class.java)
        )
    }
    try {
        println("Launching live wallpaper intent with videoUri: ${videoUri.toString()}")
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to set live wallpaper.", Toast.LENGTH_SHORT).show()
    }
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
        return VideoEngine() // No arguments passed
    }

    /**
     * Handles incoming intents.
     * Stores the intent for use in the VideoEngine.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceIntent = intent // Store the intent for use in the VideoEngine
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Inner class representing the live wallpaper engine.
     */
    inner class VideoEngine : Engine(), SurfaceHolder.Callback {
        private var mediaPlayer: MediaPlayer? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Register the SurfaceHolder callback
            surfaceHolder.addCallback(this)

            // Retrieve the video URI from SharedPreferences
            val sharedPreferences = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val videoUriString = sharedPreferences.getString("videoUri", null)
            if (!videoUriString.isNullOrEmpty()) {
                try {
                    println("Initializing MediaPlayer with videoUri: $videoUriString")

                    // Initialize the MediaPlayer
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, Uri.parse(videoUriString))
                        isLooping = true

                        // Listener for when the MediaPlayer is prepared
                        setOnPreparedListener {
                            println("MediaPlayer prepared. Starting playback.")
                            start() // Start playback
                        }

                        // Listener for errors
                        setOnErrorListener { mp, what, extra ->
                            println("MediaPlayer error: what=$what, extra=$extra")
                            true
                        }

                        // Listener for when playback completes
                        setOnCompletionListener {
                            println("MediaPlayer completed playback.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Error initializing MediaPlayer: ${e.message}")
                }
            } else {
                println("Error: No video URI provided.")
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                mediaPlayer?.release()
                println("MediaPlayer released successfully.")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error releasing MediaPlayer: ${e.message}")
            }
        }

        // SurfaceHolder.Callback methods
        override fun surfaceCreated(holder: SurfaceHolder) {
            println("Surface created.")
            mediaPlayer?.setSurface(holder.surface)
            mediaPlayer?.prepareAsync() // Prepare the MediaPlayer asynchronously
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            println("Surface changed: $width x $height")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            println("Surface destroyed.")
            mediaPlayer?.setSurface(null) // Clear the Surface to avoid crashes
        }
    }
}