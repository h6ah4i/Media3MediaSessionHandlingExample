package com.example.media3mediasessionhandlingexample

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.media3mediasessionhandlingexample.ui.theme.Media3MediaSessionHandlingExampleTheme


class MainActivity : ComponentActivity() {
    companion object {
        const val targetAppName = "Stellio"
        const val targetMediaAppPackage = "io.stellio.music"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Media3MediaSessionHandlingExampleTheme {
                ScreenContents(
                    requestNotificationAccess = { requestNotificationAccess() },
                    launchMediaApp = { launchMediaApp() },
                    createMediaController = { createMediaController() },
                )
            }
        }
    }

    private val listenerServiceComponent by lazy {
        ComponentName(this, NotificationListener::class.java)
    }

    private fun requestNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
            putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listenerServiceComponent.flattenToString())
        })
    }

    private fun launchMediaApp() {
        val intent = packageManager.getLaunchIntentForPackage(targetMediaAppPackage)
        startActivity(intent)
    }

    private fun createMediaController() {
        val sessionManager = this.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager

        val activeSessions = try {
            sessionManager.getActiveSessions(listenerServiceComponent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Notification access denied", Toast.LENGTH_LONG).show()
            return
        }

        val systemSession = activeSessions.find { it.packageName == targetMediaAppPackage }

        if (systemSession == null) {
            Toast.makeText(this, "$targetAppName is not running", Toast.LENGTH_LONG).show()
            return
        }

        val media3SessionFuture = SessionToken.createSessionToken(this, systemSession.sessionToken)
        media3SessionFuture.addListener({
            val media3Session = media3SessionFuture.get()!!
            val controllerFuture = MediaController.Builder(this@MainActivity, media3Session).buildAsync()

            controllerFuture.addListener({
                Toast.makeText(this, "MediaController created!", Toast.LENGTH_LONG).show()
            }, ContextCompat.getMainExecutor(this))

       }, ContextCompat.getMainExecutor(this))
    }
}

@Composable
fun ScreenContents(requestNotificationAccess: () -> Unit = {}, launchMediaApp: () -> Unit = {}, createMediaController: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Button(onClick = { requestNotificationAccess() }) {
                Text(text = "Step 1. Request Notification Access")
            }

            Button(onClick = { launchMediaApp() }) {
                Text(text = "Step 2. Launch ${MainActivity.targetAppName}")
            }

            Box(modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp)) {
                Text(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                        .padding(vertical = 8.dp, horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    text = "Step 3. Play a song in ${MainActivity.targetAppName} and set repeat mode to one of the \"Jump to a next list\""
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                text = "This app will crash after clicked the Step 4 button. You will see the following message on logcat.",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = "java.lang.IllegalArgumentException: Unrecognized PlaybackStateCompat.RepeatMode: ***"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { createMediaController() }) {
                Text(text = "Step 4. Attach to the AudioSession of Stellio")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Media3MediaSessionHandlingExampleTheme {
        ScreenContents()
    }
}