@file:OptIn(ExperimentalMaterial3Api::class)

package com.saha.kidstest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() },
    )
}

private val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
fun formatDate(ms: Long): String = dateFormat.format(Date(ms))

@Composable
fun scoreColor(percent: Int): Color = when {
    percent >= 75 -> goodColor()
    percent >= 50 -> MaterialTheme.colorScheme.secondary
    else -> badColor()
}

@Composable
fun AttemptRow(a: Attempt, showKid: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    (if (showKid) "${a.kid} · " else "") + "${a.minutes}-minute test · ${a.items.size} questions",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    formatDate(a.at) + " · time used " + formatClock(a.secondsUsed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${a.score} / ${a.maxScore}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor(a.percent))
                Text("${a.percent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Correct / total for each section, as bars. Marks the weakest section when asked. */
@Composable
fun SectionBreakdown(items: List<AttemptItem>, markWeakest: Boolean = false) {
    val rows = SECTION_NAMES.keys.mapNotNull { code ->
        val inSection = items.filter { it.section == code }
        if (inSection.isEmpty()) null else Triple(code, inSection.count { it.chosen == it.correct }, inSection.size)
    }
    val weakest = if (markWeakest && rows.size > 1) rows.minByOrNull { it.second.toFloat() / it.third }?.first else null
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { (code, right, total) ->
            val fraction = right.toFloat() / total
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sectionName(code), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (code == weakest) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                            Text(
                                "Practise more",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("$right of $total correct", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = scoreColor((fraction * 100).toInt()),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
