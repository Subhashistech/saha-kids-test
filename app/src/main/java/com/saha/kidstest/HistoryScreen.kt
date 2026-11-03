@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.saha.kidstest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(s: AppState) {
    BackHandler { s.screen = Screen.Home }
    val mine = s.attempts.filter { it.kid == s.kid }.sortedByDescending { it.at }

    Scaffold(topBar = { BackTopBar("Score history", onBack = { s.screen = Screen.Home }) }) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                if (s.kids.isEmpty()) {
                    Text("Add a child on the home screen to start saving scores.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.kids.forEach { k ->
                            FilterChip(selected = k == s.kid, onClick = { s.kid = k }, label = { Text(k, fontSize = 18.sp, modifier = Modifier.padding(6.dp)) })
                        }
                    }
                }
            }
            if (s.kid.isNotEmpty() && mine.isEmpty()) {
                item {
                    Text(
                        "${s.kid} hasn't taken a test yet. Scores appear here after each test.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (mine.isNotEmpty()) {
                item {
                    val avg = mine.map { it.percent }.average().toInt()
                    val best = mine.maxOf { it.percent }
                    val lastFive = mine.take(5).reversed().joinToString("  →  ") { "${it.percent}%" }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Summary("${mine.size}", "tests taken")
                        Summary("$avg%", "average score")
                        Summary("$best%", "best score")
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("Last tests, oldest to newest: $lastFive", style = MaterialTheme.typography.bodyLarge)
                }
                item {
                    Column(Modifier.widthIn(max = 720.dp)) {
                        SectionTitle("Sections across all tests")
                        Spacer(Modifier.size(12.dp))
                        SectionBreakdown(mine.flatMap { it.items }, markWeakest = true)
                    }
                }
                item { SectionTitle("All tests") }
                items(mine, key = { it.id }) { a ->
                    Column(Modifier.widthIn(max = 720.dp)) { AttemptRow(a) { s.openReview(a, Screen.History) } }
                }
            }
        }
    }
}

@Composable
private fun Summary(value: String, label: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 15.sp)
        }
    }
}
