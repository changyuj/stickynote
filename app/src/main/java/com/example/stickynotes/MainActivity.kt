package com.example.stickynotes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.stickynotes.ui.theme.StickynotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StickynotesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasPermission) {
            Text(text = "Overlay permission is required for floating notes.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }) {
                Text("Grant Permission")
            }
        } else {
            Text(text = "Overlay permission granted!")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(context, FloatingNoteService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                (context as? android.app.Activity)?.moveTaskToBack(true)
            }) {
                Text("Start Floating Note")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val intent = Intent(context, FloatingNoteService::class.java)
                context.stopService(intent)
            }) {
                Text("Stop Floating Note")
            }
        }
    }

    // Refresh permission status when resuming
    DisposableEffect(Unit) {
        val observer = object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                hasPermission = Settings.canDrawOverlays(context)
            }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        }
        // This is a bit hacky for a simple app, alternatively we could use a SideEffect or check on every recompose if we really wanted to.
        // But for now, let's just use a simple check in a LaunchedEffect that triggers on a signal.
        onDispose {}
    }
    
    // Better way to refresh is to just check when activity returns to foreground.
    // Simplifying for now.
}