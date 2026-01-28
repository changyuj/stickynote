package com.example.stickynotes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.stickynotes.data.Note
import com.example.stickynotes.data.NoteDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FloatingNoteService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val CHANNEL_ID = "FloatingNoteServiceChannel"

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())
        showFloatingNote()
    }

    private fun showFloatingNote() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        composeView = ComposeView(this).apply {
            // Needed for Compose in Service
            val lifecycleOwner = MyLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val db = NoteDatabase.getDatabase(this@FloatingNoteService)
                val noteDao = db.noteDao()
                val focusManager = LocalFocusManager.current

                var noteText by remember { mutableStateOf("Tap to edit") }
                var offsetX by remember { mutableStateOf(params.x.toFloat()) }
                var offsetY by remember { mutableStateOf(params.y.toFloat()) }
                
                var isInteracting by remember { mutableStateOf(false) }
                var isFocused by remember { mutableStateOf(false) }
                
                // Clear focus when requested
                DisposableEffect(this@apply) {
                    val listener = android.view.View.OnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_OUTSIDE) {
                            focusManager.clearFocus()
                            true
                        } else {
                            false
                        }
                    }
                    this@apply.setOnTouchListener(listener)
                    onDispose { this@apply.setOnTouchListener(null) }
                }

                val alpha by animateFloatAsState(
                    targetValue = if (isInteracting || isFocused) 1.0f else 0.5f,
                    label = "alpha"
                )

                LaunchedEffect(Unit) {
                    val savedNote = noteDao.getAllNotes().firstOrNull()?.firstOrNull()
                    if (savedNote != null) {
                        noteText = savedNote.text
                        offsetX = savedNote.x.toFloat()
                        offsetY = savedNote.y.toFloat()
                        params.x = savedNote.x
                        params.y = savedNote.y
                        windowManager.updateViewLayout(this@apply, params)
                    }
                }

                Surface(
                    modifier = Modifier
                        .width(200.dp)
                        .wrapContentHeight()
                        .graphicsLayer(alpha = alpha)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { 
                                    isInteracting = true
                                    focusManager.clearFocus()
                                },
                                onDragEnd = { isInteracting = false },
                                onDragCancel = { isInteracting = false }
                            ) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                params.x = offsetX.toInt()
                                params.y = offsetY.toInt()
                                windowManager.updateViewLayout(this@apply, params)
                                
                                lifecycleScope.launch {
                                    noteDao.insertNote(Note(id = 1, text = noteText, x = params.x, y = params.y))
                                }
                            }
                        },
                    color = Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sticky Note", style = MaterialTheme.typography.titleSmall)
                            IconButton(
                                onClick = { stopSelf() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray
                                )
                            }
                        }
                        TextField(
                            value = noteText,
                            onValueChange = { 
                                noteText = it
                                lifecycleScope.launch {
                                    noteDao.insertNote(Note(id = 1, text = noteText, x = params.x, y = params.y))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { 
                                    isFocused = it.isFocused 
                                },
                            maxLines = if (isFocused || isInteracting) Int.MAX_VALUE else 2,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        // To allow typing, we need to remove FLAG_NOT_FOCUSABLE. 
        // But then it will block all other apps from receiving key events.
        // A common trick is to set it as focusable only when touched.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                      WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        
        windowManager.addView(composeView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Floating Note Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Note Active")
                .setContentText("Your sticky note is floating.")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Floating Note Active")
                .setContentText("Your sticky note is floating.")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let {
            windowManager.removeView(it)
        }
    }

    // Helper class to provide Lifecycle for ComposeView in a Service
    internal class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }

        fun performRestore(savedState: android.os.Bundle?) {
            savedStateRegistryController.performRestore(savedState)
        }
    }
}
