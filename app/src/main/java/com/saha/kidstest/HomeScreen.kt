@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.saha.kidstest

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(s: AppState) {
    val ctx = LocalContext.current
    var showAddKid by remember { mutableStateOf(false) }
    var askForKid by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column {
            Text(
                "Saha Kids Test",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Class 1 Math Olympiad practice · ${s.questions.size} questions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle("Who is practising?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.kids.forEach { k ->
                FilterChip(
                    selected = k == s.kid,
                    onClick = { s.kid = k },
                    label = { Text(k, fontSize = 20.sp, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) },
                )
            }
            AssistChip(
                onClick = { showAddKid = true },
                label = { Text("+ Add child", fontSize = 20.sp, modifier = Modifier.padding(vertical = 10.dp)) },
            )
        }

        SectionTitle("Choose a test")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TEST_PLANS.forEach { p ->
                Card(
                    onClick = {
                        when {
                            s.kid.isEmpty() -> askForKid = true
                            !s.startTest(p) -> Toast.makeText(ctx, "There are no questions to use. Add some in Questions.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.widthIn(min = 230.dp, max = 320.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (p.minutes == 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (p.minutes == 60) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Row {
                            Text("${p.minutes}", fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            Text(" min", fontSize = 22.sp, modifier = Modifier.padding(top = 26.dp))
                        }
                        Text("${p.total} questions", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("${p.normal} normal + ${p.achievers} Achievers", fontSize = 15.sp)
                        Text(p.label, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { s.screen = Screen.History }) {
                Text("Score history", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
            }
            OutlinedButton(onClick = { s.screen = Screen.Questions }) {
                Text("Add or edit questions", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
            }
        }

        if (s.kid.isNotEmpty()) {
            val recent = s.attempts.filter { it.kid == s.kid }.sortedByDescending { it.at }.take(3)
            SectionTitle("${s.kid}'s recent tests")
            if (recent.isEmpty()) {
                Text(
                    "No tests yet. Pick a test above to start. Answers are shown at the end.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recent.forEach { a -> AttemptRow(a) { s.openReview(a, Screen.Home) } }
                }
            }
        }
    }

    if (showAddKid) {
        AddKidDialog(
            onAdd = { name ->
                if (s.addKid(name)) {
                    showAddKid = false
                } else {
                    Toast.makeText(ctx, "Type a name that isn't already in the list.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddKid = false },
        )
    }
    if (askForKid) {
        AlertDialog(
            onDismissRequest = { askForKid = false },
            title = { Text("Add a child first") },
            text = { Text("Type your child's name so their scores are saved.") },
            confirmButton = { Button(onClick = { askForKid = false; showAddKid = true }) { Text("Add child") } },
            dismissButton = { TextButton(onClick = { askForKid = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddKidDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add child") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Child's name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
        },
        confirmButton = { Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
