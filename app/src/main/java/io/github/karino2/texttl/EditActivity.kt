package io.github.karino2.texttl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import io.github.karino2.texttl.ui.theme.TextTLTheme
import kotlinx.coroutines.delay


class EditActivity : ComponentActivity() {
    private fun onSave(text: String) {
        Intent().apply {
            putExtra("NEW_CONTENT", text)
        }.also { setResult(RESULT_OK, it) }
        finish()
    }

    val requester = FocusRequester()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        setContent {
            TextTLTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column{
                        var text by remember { mutableStateOf(defaultText) }

                        TopAppBar(title={
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = { onSave(text) }) {
                                        Icon(painter = painterResource(id = R.drawable.outline_save), contentDescription = "Save")
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            })
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxSize().focusRequester(requester)
                        )
                        LaunchedEffect(Unit) {
                            // Need this delay for openning softkey.
                            delay(300)
                            requester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}
