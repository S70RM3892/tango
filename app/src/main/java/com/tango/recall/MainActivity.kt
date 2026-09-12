package com.tango.recall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tango.recall.ui.AppViewModel
import com.tango.recall.ui.screens.BrowseScreen
import com.tango.recall.ui.screens.DeckScreen
import com.tango.recall.ui.screens.GraphScreen
import com.tango.recall.ui.screens.HomeScreen
import com.tango.recall.ui.screens.NoteScreen
import com.tango.recall.ui.screens.ReviewScreen
import com.tango.recall.ui.screens.SettingsScreen
import com.tango.recall.ui.screens.StatsScreen
import com.tango.recall.ui.screens.UniversityScreen
import com.tango.recall.ui.screens.WordShelfScreen
import com.tango.recall.ui.theme.TangoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TangoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TangoApp()
                }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val REVIEW = "review/{deckId}"
    const val DECK = "deck/{deckId}"
    const val BROWSE = "browse/{deckId}"
    const val NOTE = "note/{noteId}/{deckId}"
    const val EXAM_REVIEW = "review/exam"
    const val NOTE_REVIEW = "review/note/{noteId}"
    const val SESSION = "review/session"
    const val WORDS = "words"
    const val GRAPH = "graph/{deckId}"
    const val UNIVERSITIES = "universities"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun review(deckId: Long?) = "review/${deckId ?: -1L}"
    fun deck(deckId: Long) = "deck/$deckId"
    fun browse(deckId: Long?) = "browse/${deckId ?: -1L}"
    fun note(noteId: Long, deckId: Long) = "note/$noteId/$deckId"
    fun graph(deckId: Long?) = "graph/${deckId ?: -1L}"
    fun reviewNote(noteId: Long) = "review/note/$noteId"
}

@Composable
fun TangoApp() {
    val vm: AppViewModel = viewModel()
    val nav = rememberNavController()

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                LaunchedEffect(Unit) { vm.refresh() }
                HomeScreen(vm, nav)
            }
            composable(
                Routes.REVIEW,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { entry ->
                val raw = entry.arguments?.getLong("deckId") ?: -1L
                val deckId = raw.takeIf { it >= 0 }
                ReviewScreen(vm, nav, deckId)
            }
            composable(
                Routes.DECK,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { entry ->
                DeckScreen(vm, nav, entry.arguments?.getLong("deckId") ?: 0L)
            }
            composable(
                Routes.BROWSE,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { entry ->
                val raw = entry.arguments?.getLong("deckId") ?: -1L
                BrowseScreen(vm, nav, raw.takeIf { it >= 0 })
            }
            composable(
                Routes.NOTE,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.LongType },
                    navArgument("deckId") { type = NavType.LongType },
                ),
            ) { entry ->
                NoteScreen(
                    vm, nav,
                    noteId = entry.arguments?.getLong("noteId") ?: 0L,
                    deckId = entry.arguments?.getLong("deckId") ?: 0L,
                )
            }
            composable(
                Routes.GRAPH,
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { entry ->
                val raw = entry.arguments?.getLong("deckId") ?: -1L
                GraphScreen(vm, nav, raw.takeIf { it >= 0 })
            }
            composable(Routes.EXAM_REVIEW) { ReviewScreen(vm, nav, deckId = null, exam = true) }
            composable(
                Routes.NOTE_REVIEW,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
            ) { entry ->
                ReviewScreen(vm, nav, deckId = null, noteId = entry.arguments?.getLong("noteId"))
            }
            composable(Routes.SESSION) { ReviewScreen(vm, nav, deckId = null, resume = true) }
            composable(Routes.WORDS) { WordShelfScreen(vm, nav) }
            composable(Routes.UNIVERSITIES) { UniversityScreen(vm, nav) }
            composable(Routes.STATS) { StatsScreen(vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
        }
    }
}
