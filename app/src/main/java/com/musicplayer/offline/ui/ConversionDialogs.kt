package com.musicplayer.offline.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicplayer.offline.conversion.AudioConversionManager
import com.musicplayer.offline.conversion.ConversionState
import com.musicplayer.offline.music.Song

data class ConversionRequest(val song: Song, val shareAfter: Boolean)

@Composable
fun ConversionDialogs(
    request: ConversionRequest?,
    state: ConversionState,
    onDismissRequest: () -> Unit,
    onConfirm: (ConversionRequest) -> Unit,
    onCancel: () -> Unit,
    onDismissState: () -> Unit,
    onCompleted: (Uri) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    if (request != null && state is ConversionState.Idle) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(if (request.shareAfter) "Converter e compartilhar" else "Converter para MP3") },
            text = {
                Text(
                    if (request.shareAfter) "Esta música está em WAV. Deseja converter para MP3 em 128 kbps antes de compartilhar?"
                    else "Converter para MP3 em 128 kbps? O arquivo WAV original será preservado."
                )
            },
            confirmButton = {
                TextButton({ onConfirm(request) }) {
                    Text(if (request.shareAfter) "Converter e compartilhar" else "Converter")
                }
            },
            dismissButton = { TextButton(onDismissRequest) { Text("Cancelar") } }
        )
    }

    when (state) {
        is ConversionState.Running -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Convertendo para MP3") },
            text = {
                Column {
                    Text("Convertendo... ${state.progress}%")
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        color = PrimaryBlue
                    )
                }
            },
            confirmButton = { TextButton(onCancel) { Text("Cancelar") } }
        )
        is ConversionState.Success -> {
            LaunchedEffect(state.uri) {
                onCompleted(state.uri)
                if (state.shareAfter) shareAudioUri(context, state.uri, state.displayName, "audio/mpeg")
            }
            AlertDialog(
                onDismissRequest = onDismissState,
                title = { Text("Conversão concluída") },
                text = { Text("${state.displayName} foi salvo em ${AudioConversionManager.OUTPUT_FOLDER}") },
                confirmButton = {
                    Row {
                        TextButton({ shareAudioUri(context, state.uri, state.displayName, "audio/mpeg") }) { Text("Compartilhar") }
                        TextButton({ openConvertedFolder(context) }) { Text("Abrir pasta") }
                    }
                },
                dismissButton = { TextButton(onDismissState) { Text("Fechar") } }
            )
        }
        is ConversionState.Failure -> AlertDialog(
            onDismissRequest = onDismissState,
            title = { Text("Não foi possível converter") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onDismissState) { Text("Fechar") } }
        )
        is ConversionState.Cancelled -> AlertDialog(
            onDismissRequest = onDismissState,
            title = { Text("Conversão cancelada") },
            text = { Text("Nenhum MP3 incompleto foi mantido.") },
            confirmButton = { TextButton(onDismissState) { Text("Fechar") } }
        )
        ConversionState.Idle -> Unit
    }
}

fun shareAudioUri(context: Context, uri: Uri, title: String, mimeType: String) {
    runCatching {
        require(uri.scheme == "content") { "URI de áudio não compartilhável" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar música"))
    }.onFailure {
        Toast.makeText(context, "Não foi possível compartilhar esta música.", Toast.LENGTH_SHORT).show()
    }
}

private fun openConvertedFolder(context: Context) {
    val folderUri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents",
        "primary:Music/JUKE/Convertidos"
    )
    val viewIntent = Intent(Intent.ACTION_VIEW, folderUri).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(viewIntent) }.onFailure {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
            })
        }.onFailure {
            Toast.makeText(context, "Abra Music/JUKE/Convertidos no gerenciador de arquivos.", Toast.LENGTH_LONG).show()
        }
    }
}
