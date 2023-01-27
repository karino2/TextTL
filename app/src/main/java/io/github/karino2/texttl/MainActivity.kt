package io.github.karino2.texttl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.github.karino2.texttl.ui.theme.TextTLTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    companion object {
        const val REQUEST_OPEN_TREE_ID = 1

        const val  LAST_URI_KEY = "last_uri_path"
        fun lastUriStr(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)
        fun writeLastUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, path)
            .commit()

        fun resetLastUriStr(ctx: Context) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, null)
            .commit()

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("TextTL", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    val cells = mutableStateOf(emptyList<Hitokoto>())

    private var _url : Uri? = null

    private val rootDir: RootDir
        get() = _url?.let { RootDir(FastFile.fromTreeUri(this, it)) } ?: throw Exception("No url set")

    private fun writeLastUri(uri: Uri) = writeLastUriStr(this, uri.toString())
    private val lastUri: Uri?
        get() = lastUriStr(this)?.let { Uri.parse(it) }

    private val getRootDirUrl = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // if cancel, null coming.
        uri?.let {
            contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            writeLastUri(it)
            openRootDir(it)
        }
    }

    private val getEditResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if (result.resultCode == Activity.RESULT_OK) {
            val content = result.data?.getStringExtra("NEW_CONTENT") ?: ""
            val hitokoto = Hitokoto(content, Date())
            rootDir.saveHitokoto(hitokoto)
            cells.value = listOf(hitokoto) + cells.value
        }
    }

    fun reloadHitokotos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val hitokotos = rootDir.listHitokotoFiles().take(30).map { Hitokoto.fromFile(it) }.toList()
            withContext(Dispatchers.Main) {
                cells.value = hitokotos
            }
        }
    }
    private fun openRootDir(url: Uri) {
        _url = url
        reloadHitokotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextTLTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        TopAppBar(title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    showMessage(this@MainActivity, "Reload")
                                    reloadHitokotos()
                                }) {
                                    Icon(painter = painterResource(id = R.drawable.outline_refresh_24), contentDescription = "Reload")
                                }
                            }
                          },
                        )

                        Column(modifier= Modifier
                            .padding(13.dp, 5.dp)
                            .verticalScroll(rememberScrollState())
                            .weight(weight =1f, fill = false)) {
                            cells.value.forEach { CellView(it) }
                        }

                        Row(modifier = Modifier.padding(5.dp, 5.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End){
                            Button(onClick = {
                                Intent(this@MainActivity, EditActivity::class.java).also {
                                    getEditResult.launch(it)
                                }
                            }) {
                                Text("+", fontSize=23.sp)
                            }
                        }
                    }
                }
            }
        }

        try {
            lastUri?.let {
                openRootDir(it)

                if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
                    Intent(this@MainActivity, EditActivity::class.java).also { editIntent->
                        editIntent.putExtra(Intent.EXTRA_TEXT, receivedText(intent))
                        getEditResult.launch(editIntent)
                    }


                }
                return
            }
        } catch(_: Exception) {
            showMessage(this, "Can't open saved dir. Please reopen.")
        }
        getRootDirUrl.launch(null)
    }

    private fun receivedText(intent: Intent) : String {
        val body = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        return intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let {subject->
            "${subject} ${body}"
        } ?: body
    }
}

@Composable
fun CellView(cell: Hitokoto) {
    Card(modifier=Modifier.padding(0.dp, 2.dp), border= BorderStroke(2.dp, Color.Black)) {
        Column(modifier= Modifier
            .fillMaxWidth()
            .padding(5.dp, 0.dp)) {
            Text(cell.content, fontSize=20.sp)
            val dtf = DateFormat.getDateTimeInstance()
            Text(dtf.format(cell.date), fontSize=14.sp)
        }
    }
}

