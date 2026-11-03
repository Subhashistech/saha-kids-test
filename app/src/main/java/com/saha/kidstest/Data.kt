package com.saha.kidstest

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/** Sections, in the order they appear in the Olympiad papers. "E" is the Achievers section (2 marks). */
val SECTION_NAMES: Map<String, String> = linkedMapOf(
    "A" to "Logical Reasoning",
    "B" to "Mathematical Reasoning",
    "C" to "Everyday Mathematics",
    "D" to "Shapes and Patterns",
    "E" to "Achievers",
)

fun sectionName(code: String): String = SECTION_NAMES[code] ?: code

fun marksFor(section: String): Int = if (section == "E") 2 else 1

data class Question(
    val id: String,
    val section: String,
    val text: String,
    val options: List<String>,
    val answer: Int,
    val working: String = "",
)

/** One question as it was shown in a test, kept with the attempt so later edits don't change old results. */
data class AttemptItem(
    val qid: String,
    val section: String,
    val text: String,
    val options: List<String>,
    val correct: Int,
    val chosen: Int,
    val working: String,
)

data class Attempt(
    val id: String,
    val kid: String,
    val minutes: Int,
    val at: Long,
    val secondsUsed: Int,
    val items: List<AttemptItem>,
) {
    val score: Int get() = items.sumOf { if (it.chosen == it.correct) marksFor(it.section) else 0 }
    val maxScore: Int get() = items.sumOf { marksFor(it.section) }
    val correctCount: Int get() = items.count { it.chosen == it.correct }
    val percent: Int get() = if (maxScore == 0) 0 else (score * 100 + maxScore / 2) / maxScore
}

/** How many normal and Achievers questions each test length uses (60 min = the real exam: 25 + 10). */
data class TestPlan(val minutes: Int, val normal: Int, val achievers: Int, val label: String) {
    val total: Int get() = normal + achievers
}

val TEST_PLANS = listOf(
    TestPlan(15, 8, 2, "Quick practice"),
    TestPlan(30, 15, 5, "Half paper"),
    TestPlan(60, 25, 10, "Full exam, like the real Olympiad"),
)

data class TestQ(val q: Question, val options: List<String>, val correct: Int)

/**
 * Picks a random test. Normal questions are spread evenly over sections A–D and kept in section order,
 * Achievers come last, like the real paper. Questions this child saw in recent tests are used only when
 * there are not enough fresh ones. Answer options are shuffled every time.
 */
fun buildTest(all: List<Question>, plan: TestPlan, recentIds: Set<String>, rnd: Random = Random.Default): List<TestQ> {
    fun freshFirst(pool: List<Question>): MutableList<Question> {
        val fresh = pool.filter { it.id !in recentIds }.shuffled(rnd)
        val seen = pool.filter { it.id in recentIds }.shuffled(rnd)
        return (fresh + seen).toMutableList()
    }
    val normalSections = listOf("A", "B", "C", "D")
    val pools = normalSections.associateWith { s -> freshFirst(all.filter { it.section == s }) }
    val picked = mutableListOf<Question>()
    var i = 0
    while (picked.size < plan.normal && pools.values.any { it.isNotEmpty() }) {
        val pool = pools.getValue(normalSections[i % normalSections.size])
        if (pool.isNotEmpty()) picked.add(pool.removeAt(0))
        i++
    }
    val achievers = freshFirst(all.filter { it.section == "E" }).take(plan.achievers)
    // If the bank has too few Achievers questions, top up with more normal ones.
    val shortBy = plan.achievers - achievers.size
    if (shortBy > 0) {
        val rest = pools.values.flatten().toMutableList()
        repeat(shortBy) { if (rest.isNotEmpty()) picked.add(rest.removeAt(0)) }
    }
    val ordered = picked.sortedBy { normalSections.indexOf(it.section) } + achievers
    return ordered.map { q ->
        val order = q.options.indices.shuffled(rnd)
        TestQ(q, order.map { q.options[it] }, order.indexOf(q.answer))
    }
}

// ---------- Saving and loading (all on the tablet, in the app's private folder) ----------

class Store(private val ctx: Context) {
    private val questionsFile = File(ctx.filesDir, "questions.json")
    private val attemptsFile = File(ctx.filesDir, "attempts.json")
    private val kidsFile = File(ctx.filesDir, "kids.json")

    fun loadBuiltIn(): List<Question> {
        val text = ctx.assets.open("questions.json").bufferedReader().use { it.readText() }
        return questionsFromJson(JSONArray(text))
    }

    fun loadQuestions(): List<Question> =
        if (questionsFile.exists()) {
            runCatching { questionsFromJson(JSONArray(questionsFile.readText())) }.getOrElse { loadBuiltIn() }
        } else {
            loadBuiltIn()
        }

    fun saveQuestions(list: List<Question>) = writeSafely(questionsFile, questionsToJson(list).toString())

    fun loadKids(): List<String> = runCatching {
        if (!kidsFile.exists()) return emptyList()
        val arr = JSONArray(kidsFile.readText())
        List(arr.length()) { arr.getString(it) }
    }.getOrElse { emptyList() }

    fun saveKids(list: List<String>) = writeSafely(kidsFile, JSONArray(list).toString())

    fun loadAttempts(): List<Attempt> = runCatching {
        if (!attemptsFile.exists()) return emptyList()
        val arr = JSONArray(attemptsFile.readText())
        List(arr.length()) { attemptFromJson(arr.getJSONObject(it)) }
    }.getOrElse { emptyList() }

    fun saveAttempts(list: List<Attempt>) {
        val arr = JSONArray()
        list.forEach { arr.put(attemptToJson(it)) }
        writeSafely(attemptsFile, arr.toString())
    }

    private fun writeSafely(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}

private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

fun questionsFromJson(arr: JSONArray): List<Question> = List(arr.length()) { i ->
    val o = arr.getJSONObject(i)
    Question(
        id = o.getString("id"),
        section = o.getString("s"),
        text = o.getString("q"),
        options = o.getJSONArray("o").strings(),
        answer = o.getInt("a"),
        working = o.optString("w", ""),
    )
}

fun questionsToJson(list: List<Question>): JSONArray {
    val arr = JSONArray()
    list.forEach { q ->
        arr.put(JSONObject().apply {
            put("id", q.id); put("s", q.section); put("q", q.text)
            put("o", JSONArray(q.options)); put("a", q.answer)
            if (q.working.isNotBlank()) put("w", q.working)
        })
    }
    return arr
}

private fun attemptToJson(a: Attempt): JSONObject = JSONObject().apply {
    put("id", a.id); put("kid", a.kid); put("min", a.minutes); put("at", a.at); put("used", a.secondsUsed)
    val items = JSONArray()
    a.items.forEach { it ->
        items.put(JSONObject().apply {
            put("qid", it.qid); put("s", it.section); put("q", it.text); put("o", JSONArray(it.options))
            put("c", it.correct); put("ch", it.chosen); put("w", it.working)
        })
    }
    put("items", items)
}

private fun attemptFromJson(o: JSONObject): Attempt {
    val items = o.getJSONArray("items")
    return Attempt(
        id = o.getString("id"),
        kid = o.getString("kid"),
        minutes = o.getInt("min"),
        at = o.getLong("at"),
        secondsUsed = o.getInt("used"),
        items = List(items.length()) { i ->
            val it = items.getJSONObject(i)
            AttemptItem(
                qid = it.getString("qid"), section = it.getString("s"), text = it.getString("q"),
                options = it.getJSONArray("o").strings(), correct = it.getInt("c"), chosen = it.getInt("ch"),
                working = it.optString("w", ""),
            )
        },
    )
}

// ---------- CSV import / export ----------

const val CSV_HEADER = "section,question,option_a,option_b,option_c,option_d,answer,working"

fun parseCsv(input: String): List<List<String>> {
    val text = input.removePrefix("﻿")
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    cell.append('"'); i++
                } else {
                    inQuotes = false
                }
            } else {
                cell.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(cell.toString()); cell.setLength(0) }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.setLength(0); rows.add(row); row = mutableListOf() }
                else -> cell.append(c)
            }
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row.add(cell.toString()); rows.add(row)
    }
    return rows
}

fun sectionFromText(value: String): String? {
    val t = value.trim()
    if (t.isEmpty()) return null
    val upper = t.uppercase()
    if (upper.length == 1 && SECTION_NAMES.containsKey(upper)) return upper
    return SECTION_NAMES.entries.firstOrNull { (_, name) ->
        name.equals(t, ignoreCase = true) || (t.length >= 3 && name.lowercase().startsWith(t.lowercase()))
    }?.key
}

/** Returns the questions read from the file and how many rows were skipped because something was missing. */
fun questionsFromCsv(text: String, newId: () -> String): Pair<List<Question>, Int> {
    val rows = parseCsv(text).filter { r -> r.any { it.isNotBlank() } }
    val out = mutableListOf<Question>()
    var skipped = 0
    rows.forEachIndexed { index, r ->
        if (index == 0 && r.firstOrNull()?.trim()?.lowercase() == "section") return@forEachIndexed
        if (r.size < 7) { skipped++; return@forEachIndexed }
        val section = sectionFromText(r[0])
        val answer = "ABCD".indexOf(r[6].trim().uppercase().take(1))
        val question = r[1].trim()
        val options = r.subList(2, 6).map { it.trim() }
        if (section == null || answer < 0 || question.isEmpty() || options.any { it.isEmpty() }) {
            skipped++; return@forEachIndexed
        }
        out.add(Question(newId(), section, question, options, answer, r.getOrNull(7)?.trim() ?: ""))
    }
    return out to skipped
}

private fun csvCell(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""

fun questionsToCsv(list: List<Question>): String = buildString {
    append(CSV_HEADER).append("\n")
    list.forEach { q ->
        val cells = listOf(q.section, q.text) + q.options + listOf("ABCD"[q.answer].toString(), q.working)
        append(cells.joinToString(",") { csvCell(it) }).append("\n")
    }
}

// ---------- Read aloud ----------

/** Turns symbols into words so the tablet's voice reads them properly. */
fun speakable(s: String): String = s
    .replace(Regex("₹\\s?(\\d+)"), "\$1 rupees")
    .replace("₹", " rupees ")
    .replace("★", " star ")
    .replace("●", " circle ")
    .replace("▲", " triangle ")
    .replace("■", " square ")
    .replace("−", " minus ")
    .replace("×", " times ")
    .replace("÷", " divided by ")
    .replace("½", " half ")
    .replace(Regex("_{2,}"), " blank ")
    .replace(Regex("\\s+"), " ")
    .trim()

fun spokenQuestion(text: String, options: List<String>): String = buildString {
    append(speakable(text)).append(". ")
    options.forEachIndexed { i, o -> append("Option ${'A' + i}: ").append(speakable(o)).append(". ") }
}

fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
