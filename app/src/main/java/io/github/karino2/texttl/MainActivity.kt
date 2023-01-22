package io.github.karino2.texttl

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import io.github.karino2.texttl.ui.theme.TextTLTheme
import java.text.DateFormat
import java.util.*

// similar to DocumentFile, but store metadata at first query.
data class FastFile(val uri: Uri, val name: String, val lastModified: Long)

fun DocumentFile.toFastFile() = FastFile(this.uri, this.name ?: "", this.lastModified())

@SuppressLint("Range")
fun listFiles(resolver: ContentResolver, parent: Uri) : Sequence<FastFile> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
    val cursor = resolver.query(childrenUri, null,
        null, null, null, null) ?: return emptySequence()

    return sequence {
        cursor.use {cur ->
            while(cur.moveToNext()) {
                val docId = cur.getString(0)
                val uri = DocumentsContract.buildDocumentUriUsingTree(parent, docId)

                val disp = cur.getString(cur.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                val lm = cur.getLong(cur.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                yield(FastFile(uri, disp, lm))
            }
        }
    }
}

data class Cell(val content: String, val date: Date)

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

    val cells = mutableStateOf(listOf(Cell("test1", Date()), Cell("test2", Date())))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TextTLTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(modifier = Modifier.padding(5.dp, 5.dp)) {
                        ListCells(cells.value)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End){
                            Button(onClick = {}) {
                                Text("+", fontSize=23.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CellView(cell: Cell) {
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

@Composable
fun ListCells(cells: List<Cell>) {
    Column(modifier= Modifier
        .padding(0.dp, 10.dp)
        .verticalScroll(rememberScrollState())) {
        cells.forEach { CellView(it) }
    }
}
