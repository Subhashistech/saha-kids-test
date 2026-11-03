@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.saha.kidstest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultScreen(s: AppState) {
    val a = s.reviewing ?: run { s.screen = Screen.Home; return }
    BackHandler { s.screen = Screen.Home }
    val skipped = a.items.count { it.chosen < 0 }
    val wrong = a.items.size - a.correctCount - skipped
    val pct = a.percent

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text(
            when {
                pct >= 80 -> "Excellent work, ${a.kid}!"
                pct >= 50 -> "Well done, ${a.kid}!"
                else -> "Good try, ${a.kid}! Let's look at the answers."
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${a.score}", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = scoreColor(pct))
            Text(" / ${a.maxScore} marks", fontSize = 28.sp, modifier = Modifier.padding(bottom = 14.dp))
            Spacer(Modifier.width(16.dp))
            Text("$pct%", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 14.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("${a.correctCount}", "correct", goodBg(), goodColor())
            StatChip("$wrong", "wrong", badBg(), badColor())
            StatChip("$skipped", "not answered", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            StatChip(formatClock(a.secondsUsed), "of ${a.minutes}:00 used", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(Modifier.widthIn(max = 720.dp)) {
            SectionTitle("By section")
            Spacer(Modifier.size(12.dp))
            SectionBreakdown(a.items, markWeakest = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { s.openReview(a, Screen.Result) }) {
                Text("See the answers", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
            }
            OutlinedButton(onClick = { s.screen = Screen.Home }) {
                Text("Home", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 15.sp)
        }
    }
}

private enum class ReviewFilter(val label: String) { All("All"), Wrong("Wrong"), Skipped("Not answered"), Right("Correct") }

@Composable
fun ReviewScreen(s: AppState) {
    val a = s.reviewing ?: run { s.screen = Screen.Home; return }
    BackHandler { s.screen = s.reviewBack }
    var filter by remember { mutableIntStateOf(0) }
    val f = ReviewFilter.entries[filter]
    val shown = a.items.withIndex().filter { (_, it) ->
        when (f) {
            ReviewFilter.All -> true
            ReviewFilter.Wrong -> it.chosen >= 0 && it.chosen != it.correct
            ReviewFilter.Skipped -> it.chosen < 0
            ReviewFilter.Right -> it.chosen == it.correct
        }
    }

    Scaffold(
        topBar = { BackTopBar("Answers · ${a.kid} · ${a.score}/${a.maxScore}", onBack = { s.screen = s.reviewBack }) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReviewFilter.entries.forEachIndexed { i, rf ->
                        FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(rf.label, fontSize = 17.sp) })
                    }
                }
            }
            if (shown.isEmpty()) {
                item { Text("No questions here.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp)) }
            }
            items(shown, key = { it.index }) { (i, item) -> ReviewCard(i + 1, item) }
        }
    }
}

@Composable
private fun ReviewCard(number: Int, item: AttemptItem) {
    val cs = MaterialTheme.colorScheme
    val right = item.chosen == item.correct
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Q$number · ${sectionName(item.section)} · ${marksFor(item.section)} mark${if (marksFor(item.section) > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.primary,
                    modifier = Modifier.weight(1f),
                )
                val (label, color) = when {
                    item.chosen < 0 -> "Not answered" to cs.onSurfaceVariant
                    right -> "Correct" to goodColor()
                    else -> "Wrong" to badColor()
                }
                Text(label, fontWeight = FontWeight.Bold, color = color)
            }
            Text(item.text, fontSize = 21.sp, lineHeight = 30.sp)
            item.options.forEachIndexed { i, opt ->
                val isCorrect = i == item.correct
                val isChosenWrong = i == item.chosen && !isCorrect
                val bg = when {
                    isCorrect -> goodBg()
                    isChosenWrong -> badBg()
                    else -> cs.surface
                }
                Row(
                    Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(34.dp).background(
                            when {
                                isCorrect -> goodColor()
                                isChosenWrong -> badColor()
                                else -> cs.surfaceVariant
                            },
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ('A' + i).toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect || isChosenWrong) cs.surface else cs.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(opt, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    when {
                        isCorrect && i == item.chosen -> Text("Your answer ✓", color = goodColor(), fontWeight = FontWeight.SemiBold)
                        isCorrect -> Text("Correct answer", color = goodColor(), fontWeight = FontWeight.SemiBold)
                        isChosenWrong -> Text("Your answer", color = badColor(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (item.working.isNotBlank()) {
                Surface(color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text("How to solve: ${item.working}", fontSize = 17.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
