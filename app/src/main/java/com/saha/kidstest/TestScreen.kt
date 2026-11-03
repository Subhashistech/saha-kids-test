@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.saha.kidstest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TestScreen(s: AppState) {
    if (s.test.isEmpty()) return

    // Keep the tablet awake while the clock is running.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.endsAt) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= s.endsAt) {
                s.submit()
                break
            }
            delay(250)
        }
    }

    var confirmQuit by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    BackHandler { confirmQuit = true }

    val totalSeconds = s.plan.minutes * 60
    val remaining = ((s.endsAt - now).coerceAtLeast(0L) / 1000L).toInt()
    val index = s.current.coerceIn(0, s.test.size - 1)
    val t = s.test[index]
    val answered = s.answers.count { it >= 0 }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Column(Modifier.fillMaxSize()) {
            // Top bar: quit, who and where, clock
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { confirmQuit = true }) { Icon(Icons.Filled.Close, contentDescription = "Stop test") }
                Column(Modifier.weight(1f)) {
                    Text("Question ${index + 1} of ${s.test.size}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("${s.kid} · ${s.plan.minutes}-minute test · $answered answered", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TimerPill(remaining)
            }
            LinearProgressIndicator(
                progress = { if (totalSeconds == 0) 0f else remaining.toFloat() / totalSeconds },
                modifier = Modifier.fillMaxWidth(),
                color = if (remaining <= 60) badColor() else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            if (wide) {
                Row(Modifier.weight(1f)) {
                    QuestionPane(s, t, index, Modifier.weight(1f).fillMaxHeight(), showPalette = false)
                    Column(
                        Modifier.width(300.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(20.dp),
                    ) { Palette(s) }
                }
            } else {
                QuestionPane(s, t, index, Modifier.weight(1f).fillMaxWidth(), showPalette = true)
            }

            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { s.current = index - 1 }, enabled = index > 0) {
                    Text("Previous", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                }
                Spacer(Modifier.weight(1f))
                if (index < s.test.size - 1) {
                    OutlinedButton(onClick = { s.current = index + 1 }) {
                        Text("Next", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                    }
                }
                Button(onClick = { confirmSubmit = true }) {
                    Text("Submit", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("Stop this test?") },
            text = { Text("The answers so far won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmQuit = false; s.quitTest() }) { Text("Stop test", color = badColor()) } },
            dismissButton = { Button(onClick = { confirmQuit = false }) { Text("Keep going") } },
        )
    }
    if (confirmSubmit) {
        val left = s.test.size - answered
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit the test?") },
            text = {
                Text(
                    if (left == 0) "All questions are answered. You'll see the score and the answers next."
                    else "$left question${if (left == 1) " is" else "s are"} not answered yet. You can still go back and answer them."
                )
            },
            confirmButton = { Button(onClick = { confirmSubmit = false; s.submit() }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Go back") } },
        )
    }
}

@Composable
private fun TimerPill(remaining: Int) {
    val low = remaining <= 60
    Surface(
        shape = RoundedCornerShape(50),
        color = if (low) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (low) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            formatClock(remaining),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun QuestionPane(s: AppState, t: TestQ, index: Int, modifier: Modifier, showPalette: Boolean) {
    val scroll = rememberScrollState()
    LaunchedEffect(index) { scroll.scrollTo(0) }
    Column(
        modifier.verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            sectionName(t.q.section) + if (t.q.section == "E") " · 2 marks" else " · 1 mark",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(t.q.text, fontSize = 26.sp, lineHeight = 38.sp, color = MaterialTheme.colorScheme.onBackground)
        TextButton(onClick = { s.speak(spokenQuestion(t.q.text, t.options)) }) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Read aloud", fontSize = 18.sp)
        }
        t.options.forEachIndexed { i, option ->
            OptionRow(letter = 'A' + i, text = option, selected = s.answers.getOrElse(index) { -1 } == i) { s.choose(i) }
        }
        Text(
            "Tap an answer again to clear it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showPalette) {
            Spacer(Modifier.size(8.dp))
            Palette(s)
        }
    }
}

/** One answer choice, drawn like an answer-sheet bubble. */
@Composable
private fun OptionRow(letter: Char, text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) cs.primaryContainer else cs.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) cs.primary else cs.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(if (selected) cs.primary else cs.surface, CircleShape)
                    .border(2.dp, cs.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (selected) cs.onPrimary else cs.primary)
            }
            Spacer(Modifier.width(18.dp))
            Text(text, fontSize = 24.sp, lineHeight = 32.sp, color = cs.onSurface)
        }
    }
}

/** Numbered circles to jump to any question. Filled = answered, ringed = the one on screen. */
@Composable
private fun Palette(s: AppState) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("All questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            s.test.indices.forEach { i ->
                val done = s.answers.getOrElse(i) { -1 } >= 0
                val here = i == s.current
                Box(
                    Modifier
                        .size(50.dp)
                        .background(if (done) cs.primary else cs.surfaceVariant, CircleShape)
                        .border(if (here) 3.dp else 0.dp, if (here) cs.secondary else cs.surfaceVariant, CircleShape)
                        .clickable { s.current = i },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = if (done) cs.onPrimary else cs.onSurfaceVariant)
                }
            }
        }
        Text(
            "Blue = answered. Questions ${s.plan.normal + 1}–${s.test.size} are Achievers (2 marks).",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}
