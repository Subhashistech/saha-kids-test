package com.saha.kidstest

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class Screen { Home, Test, Result, Review, History, Questions, Edit }

class AppState(
    private val store: Store,
    val speak: (String) -> Unit,
    val stopSpeaking: () -> Unit,
) {
    var questions by mutableStateOf(store.loadQuestions())
        private set
    var kids by mutableStateOf(store.loadKids())
        private set
    var attempts by mutableStateOf(store.loadAttempts())
        private set
    var kid by mutableStateOf(kids.firstOrNull() ?: "")
    var screen by mutableStateOf(Screen.Home)

    // The test in progress
    var test by mutableStateOf<List<TestQ>>(emptyList())
        private set
    val answers = mutableStateListOf<Int>()
    var current by mutableIntStateOf(0)
    var plan by mutableStateOf(TEST_PLANS.first())
        private set
    var startedAt by mutableLongStateOf(0L)
        private set
    var endsAt by mutableLongStateOf(0L)
        private set

    // Results and review
    var reviewing by mutableStateOf<Attempt?>(null)
    var reviewBack by mutableStateOf(Screen.Result)

    // Question editor (null = adding a new question)
    var editing by mutableStateOf<Question?>(null)

    fun addKid(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || kids.any { it.equals(n, ignoreCase = true) }) return false
        kids = kids + n
        store.saveKids(kids)
        kid = n
        return true
    }

    fun startTest(p: TestPlan): Boolean {
        val recent = attempts.filter { it.kid == kid }
            .sortedByDescending { it.at }
            .take(3)
            .flatMap { a -> a.items.map { it.qid } }
            .toSet()
        val t = buildTest(questions, p, recent)
        if (t.isEmpty()) return false
        test = t
        answers.clear()
        repeat(t.size) { answers.add(-1) }
        current = 0
        plan = p
        startedAt = System.currentTimeMillis()
        endsAt = startedAt + p.minutes * 60_000L
        screen = Screen.Test
        return true
    }

    fun choose(option: Int) {
        if (current !in answers.indices) return
        answers[current] = if (answers[current] == option) -1 else option
    }

    fun submit() {
        if (screen != Screen.Test || test.isEmpty()) return
        stopSpeaking()
        val now = System.currentTimeMillis()
        val used = ((now - startedAt) / 1000).toInt().coerceIn(0, plan.minutes * 60)
        val items = test.mapIndexed { i, t ->
            AttemptItem(t.q.id, t.q.section, t.q.text, t.options, t.correct, answers.getOrElse(i) { -1 }, t.q.working)
        }
        val attempt = Attempt("a$now", kid, plan.minutes, now, used, items)
        attempts = attempts + attempt
        store.saveAttempts(attempts)
        test = emptyList()
        reviewing = attempt
        reviewBack = Screen.Result
        screen = Screen.Result
    }

    fun quitTest() {
        stopSpeaking()
        test = emptyList()
        screen = Screen.Home
    }

    fun openReview(a: Attempt, back: Screen) {
        reviewing = a
        reviewBack = back
        screen = Screen.Review
    }

    fun saveQuestion(q: Question) {
        questions = if (questions.any { it.id == q.id }) {
            questions.map { if (it.id == q.id) q else it }
        } else {
            questions + q
        }
        store.saveQuestions(questions)
    }

    fun deleteQuestion(id: String) {
        questions = questions.filterNot { it.id == id }
        store.saveQuestions(questions)
    }

    fun importQuestions(list: List<Question>) {
        questions = questions + list
        store.saveQuestions(questions)
    }

    /** Puts back the original questions. Questions you added yourself are kept. */
    fun restoreBuiltIn() {
        val own = questions.filter { it.id.startsWith("u") }
        questions = store.loadBuiltIn() + own
        store.saveQuestions(questions)
    }

    private var idCounter = 0
    fun newId(): String = "u" + System.currentTimeMillis().toString(36) + (idCounter++).toString(36)
}

@Composable
fun App(s: AppState) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (s.screen) {
            Screen.Home -> HomeScreen(s)
            Screen.Test -> TestScreen(s)
            Screen.Result -> ResultScreen(s)
            Screen.Review -> ReviewScreen(s)
            Screen.History -> HistoryScreen(s)
            Screen.Questions -> QuestionsScreen(s)
            Screen.Edit -> EditQuestionScreen(s)
        }
    }
}
