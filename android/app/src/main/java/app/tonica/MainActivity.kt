package app.tonica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.tonica.ui.AppRoot
import app.tonica.ui.TonicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TonicaApp
        setContent {
            TonicaTheme {
                AppRoot(app)
            }
        }
    }
}
