package dev.bluelemonade.ledger.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bluelemonade.ledger.comm.TagManager


@Composable
fun TagView() {
    val context = LocalContext.current
    var tags by remember { mutableStateOf(TagManager.getTags(context).filterNot { it == "태그없음" }) }
    var newTag by remember { mutableStateOf(TextFieldValue("")) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var editTag by remember { mutableStateOf(TextFieldValue("")) }

    val focusManager = LocalFocusManager.current
    BackHandler {
        if (editIndex != -1) {
            editIndex = -1
            focusManager.clearFocus()
        } else {
            (context as? android.app.Activity)?.finish()
        }
    }


    Column(modifier = Modifier
        .padding(16.dp)
        .navigationBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val context = LocalContext.current
            IconButton(onClick = {
                if (editIndex != -1) {
                    editIndex = -1
                } else {
                    (context as? android.app.Activity)?.finish()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Text("태그 관리", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTag,
                onValueChange = {
                    if (it.text.length <= 10) newTag = it
                },
                label = { Text("새 태그") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmed = newTag.text.trim()
                    if (trimmed.isNotEmpty() && trimmed !in tags) {
                        tags = tags + trimmed
                        TagManager.saveTags(context, tags)
                        newTag = TextFieldValue("")
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("추가")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(tags) { index, tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (editIndex == index) {
                        OutlinedTextField(
                            value = editTag,
                            onValueChange = { editTag = it },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val updated = editTag.text.trim()
                            if (updated.isNotEmpty() && updated !in tags) {
                                tags = tags.toMutableList().apply {
                                    set(index, updated)
                                }
                                TagManager.saveTags(context, tags)
                                editIndex = -1
                            }
                        }) {
                            Text("확인")
                        }
                    } else {
                        Text(
                            text = tag,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editIndex = index
                            editTag = TextFieldValue(tag)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            tags = tags.toMutableList().apply { removeAt(index) }
                            TagManager.saveTags(context, tags)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewTagManagerView() {
    MaterialTheme {
        TagView()
    }
}