package app.echo.messages.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.MutableStateFlow

/** Routes: onboarding → home → chat / search / settings. Screens slide on a shared horizontal axis. */
@Composable
fun EchoRoot(openChat: MutableStateFlow<Pair<String, String>?>) {
    val context = LocalContext.current
    val vm: EchoViewModel = viewModel()
    val nav = rememberNavController()
    val start = remember { if (listenerEnabled(context)) "home" else "onboarding" }

    fun chatRoute(pkg: String, conversation: String) = "chat/${Uri.encode(pkg)}/${Uri.encode(conversation)}"

    // Tapping a "deleted message" alert lands directly in that chat.
    val pending by openChat.collectAsState()
    LaunchedEffect(pending) {
        val (pkg, conversation) = pending ?: return@LaunchedEffect
        openChat.value = null
        nav.navigate(chatRoute(pkg, conversation)) { launchSingleTop = true }
    }

    val duration = 280
    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(duration), initialOffset = { it / 4 }) + fadeIn(tween(duration)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(duration), targetOffset = { it / 4 }) + fadeOut(tween(duration / 2)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(duration), initialOffset = { it / 4 }) + fadeIn(tween(duration)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(duration), targetOffset = { it / 4 }) + fadeOut(tween(duration / 2)) },
    ) {
        composable("onboarding") {
            OnboardingScreen(onDone = { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } })
        }
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenChat = { pkg, c -> nav.navigate(chatRoute(pkg, c)) },
                onSearch = { nav.navigate("search") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable(
            "chat/{pkg}/{conversation}",
            arguments = listOf(navArgument("pkg") { type = NavType.StringType }, navArgument("conversation") { type = NavType.StringType }),
        ) { entry ->
            val pkg = entry.arguments?.getString("pkg").orEmpty()
            val conversation = entry.arguments?.getString("conversation").orEmpty()
            ChatScreen(vm, pkg, conversation, onBack = { nav.popBackStack() })
        }
        composable("search") {
            SearchScreen(vm, onOpenChat = { pkg, c -> nav.navigate(chatRoute(pkg, c)) }, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
