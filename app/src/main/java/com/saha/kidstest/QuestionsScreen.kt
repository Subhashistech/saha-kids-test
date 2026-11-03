@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.saha.kidstest

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuestionsScreen(s: AppState) {
    val ctx = LocalContext.current
    BackHandler { s.screen = Screen.Home }
    var search by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("") } // "" = all
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text == null) {
            Toast.makeText(ctx, "Couldn't open that file. Pick a .csv file.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        val (found, skipped) = questionsFromCsv(text) { s.newId() }
        s.importQuestions(found)
        val msg = when {
            found.isEmpty() -> "No questions found. Check the file has the columns shown in \"File format\"."
            skipped > 0 -> "Added ${found.size} questions. Skipped $skipped rows with missing parts."
            else -> "Added ${found.size} questions."
        }
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(questionsToCsv(s.questions)) } != null
        }.getOrDefault(false)
        Toast.makeText(ctx, if (ok) "Saved ${s.questions.size} questions." else "Couldn't save the file.", Toast.LENGTH_LONG).show()
    }

    val shown = s.questions.filter { q ->
        (section.isEmpty() || q.section == section) &&
            (search.isBlank() || q.text.contains(search.trim(), ignoreCase = true) || q.options.any { it.contains(search.trim(), ignoreCase = true) })
    }

    Scaffold(
        topBar = {
            BackTopBar("Questions (${s.questions.size})", onBack = { s.screen = Screen.Home }) {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Add questions from a CSV file") }, onClick = { menuOpen = false; importer.launch(arrayOf("*/*")) })
                    DropdownMenuItem(text = { Text("Save all questions to a CSV file") }, onClick = { menuOpen = false; exporter.launch("saha-questions.csv") })
                    DropdownMenuItem(text = { Text("File format") }, onClick = { menuOpen = false; showHelp = true })
                    DropdownMenuItem(text = { Text("Restore the original questions") }, onClick = { menuOpen = false; confirmRestore = true })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { s.editing = null; s.screen = Screen.Edit },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add question", fontSize = 18.sp) },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search questions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = section.isEmpty(), onClick = { section = "" }, label = { Text("All") })
                    SECTION_NAMES.forEach { (code, name) ->
                        val count = s.questions.count { it.section == code }
                        FilterChip(selected = section == code, onClick = { section = code }, label = { Text("$name ($count)") })
                    }
                }
            }
            if (shown.isEmpty()) {
                item { Text("No questions match.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 20.dp)) }
            }
            items(shown, key = { it.id }) { q ->
                Card(
                    onClick = { s.editing = q; s.screen = Screen.Edit },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(q.text, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "${sectionName(q.section)} · Answer: ${'A' + q.answer}. ${q.options.getOrElse(q.answer) { "" }}" +
                                if (q.id.startsWith("u")) " · added by you" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore the original questions?") },
            text = { Text("This puts back the 220 original questions as they came, undoing edits or deletions to them. Questions you added yourself stay.") },
            confirmButton = {
                Button(onClick = {
                    confirmRestore = false
                    s.restoreBuiltIn()
                    Toast.makeText(ctx, "Original questions restored.", Toast.LENGTH_SHORT).show()
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("CSV file format") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Make a spreadsheet with these 8 columns and save it as CSV:")
                    Text(CSV_HEADER, fontWeight = FontWeight.SemiBold)
                    Text("section: A, B, C, D or E (E = Achievers, 2 marks), or the section name.")
                    Text("answer: the letter of the correct option, A to D.")
                    Text("working: optional, shown in the answers after the test.")
                    Text("Example:")
                    Text("C,\"A pen costs ₹12. How much do 3 pens cost?\",₹15,₹36,₹24,₹30,B,\"12 + 12 + 12 = 36\"")
                    Text("Tip: use \"Save all questions to a CSV file\" to get a file in the right format to start from.")
                }
            },
            confirmButton = { Button(onClick = { showHelp = false }) { Text("OK") } },
        )
    }
}

@Composable
fun EditQuestionScreen(s: AppState) {
    val ctx = LocalContext.current
    val original = s.editing
    BackHandler { s.screen = Screen.Questions }

    var section by rememberSaveable { mutableStateOf(original?.section ?: "A") }
    var text by rememberSaveable { mutableStateOf(original?.text ?: "") }
    val options = remember { mutableStateListOf(*(original?.options ?: listOf("", "", "", "")).toTypedArray()) }
    var answer by rememberSaveable { mutableStateOf(original?.answer ?: 0) }
    var working by rememberSaveable { mutableStateOf(original?.working ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(if (original == null) "Add question" else "Edit question", onBack = { s.screen = Screen.Questions }) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.widthIn(max = 800.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Section", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SECTION_NAMES.forEach { (code, name) ->
                        FilterChip(
                            selected = section == code,
                            onClick = { section = code },
                            label = { Text(if (code == "E") "$name (2 marks)" else name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Question") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Options. Tap the circle next to the correct answer.", style = MaterialTheme.typography.titleMedium)
                options.indices.forEach { i ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = answer == i, onClick = { answer = i })
                        Text("${'A' + i}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
                        OutlinedTextField(
                            value = options[i],
                            onValueChange = { options[i] = it },
                            label = { Text(if (answer == i) "Option ${'A' + i} (correct)" else "Option ${'A' + i}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(
                    value = working,
                    onValueChange = { working = it },
                    label = { Text("How to solve (optional, shown after the test)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val opts = options.map { it.trim() }
                        val problem = when {
                            text.isBlank() -> "Type the question."
                            opts.any { it.isEmpty() } -> "Fill in all four options."
                            opts.toSet().size < 4 -> "The four options must all be different."
                            else -> null
                        }
                        if (problem != null) {
                            Toast.makeText(ctx, problem, Toast.LENGTH_LONG).show()
                        } else {
                            s.saveQuestion(Question(original?.id ?: s.newId(), section, text.trim(), opts, answer, working.trim()))
                            Toast.makeText(ctx, "Question saved.", Toast.LENGTH_SHORT).show()
                            s.screen = Screen.Questions
                        }
                    }) { Text("Save", fontSize = 18.sp, modifier = Modifier.padding(6.dp)) }
                    OutlinedButton(onClick = { s.screen = Screen.Questions }) { Text("Cancel", fontSize = 18.sp, modifier = Modifier.padding(6.dp)) }
                    if (original != null) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { confirmDelete = true }) { Text("Delete question", color = badColor(), fontSize = 18.sp) }
                    }
                }
            }
        }
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this question?") },
            text = { Text("It won't appear in new tests. Past results keep their copy of it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    s.deleteQuestion(original.id)
                    s.screen = Screen.Questions
                }) { Text("Delete", color = badColor()) }
            },
            dismissButton = { Button(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}
