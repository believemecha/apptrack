package com.example.apptrack.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.apptrack.call.CallInfo
import com.example.apptrack.ui.contacts.ContactsScreen
import com.example.apptrack.ui.dialer.DialPadScreen
import com.example.apptrack.ui.recents.RecentsScreen
import com.example.apptrack.ui.screens.CallHistoryScreen
import com.example.apptrack.ui.screens.ContactProfileScreen
import com.example.apptrack.ui.search.SearchScreen

@Composable
fun HomeNavHost(
    callHistory: List<CallInfo>,
    currentCall: CallInfo?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMakeCall: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    isBlocked: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTabScreen = currentRoute in listOf(
        PhoneAppRoutes.Recents,
        PhoneAppRoutes.Contacts,
        PhoneAppRoutes.Search
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTabScreen) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (isTabScreen) {
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    DialerFAB(
                        onClick = {
                            navController.navigate(PhoneAppRoutes.DialPad) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = PhoneAppRoutes.Recents,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(PhoneAppRoutes.Recents) {
                RecentsScreen(
                    callHistory = callHistory,
                    currentCall = currentCall,
                    onSearchClick = {
                        navController.navigate(PhoneAppRoutes.Search) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCallDetails = { number ->
                        navController.navigate(PhoneAppRoutes.callDetails(number))
                    },
                    onOpenProfile = { number ->
                        navController.navigate(PhoneAppRoutes.contactDetails(number))
                    },
                    onMakeCall = onMakeCall,
                    onBlockNumber = onBlockNumber,
                    onUnblockNumber = onUnblockNumber,
                    isBlocked = isBlocked,
                    onAnswerCall = onAnswerCall,
                    onRejectCall = onRejectCall
                )
            }
            composable(PhoneAppRoutes.Contacts) {
                ContactsScreen(
                    onContactClick = { number ->
                        navController.navigate(PhoneAppRoutes.contactDetails(number))
                    },
                    onCallClick = onMakeCall
                )
            }
            composable(PhoneAppRoutes.Search) {
                SearchScreen(
                    callHistory = callHistory,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onBack = {
                        navController.navigate(PhoneAppRoutes.Recents) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onContactClick = { number ->
                        navController.navigate(PhoneAppRoutes.contactDetails(number))
                    },
                    onCallDetailsClick = { number ->
                        navController.navigate(PhoneAppRoutes.callDetails(number))
                    },
                    onMakeCall = onMakeCall
                )
            }
            composable(PhoneAppRoutes.DialPad) {
                DialPadScreen(
                    callHistory = callHistory,
                    onCall = { number ->
                        onMakeCall(number)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onContactClick = { number ->
                        navController.navigate(PhoneAppRoutes.contactDetails(number))
                    }
                )
            }
            composable(
                route = PhoneAppRoutes.CallDetails,
                arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("phoneNumber") ?: return@composable
                CallHistoryScreen(
                    phoneNumber = number,
                    callHistory = callHistory,
                    onBack = { navController.popBackStack() },
                    onMakeCall = onMakeCall
                )
            }
            composable(
                route = PhoneAppRoutes.ContactDetails,
                arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
            ) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("phoneNumber") ?: return@composable
                ContactProfileScreen(
                    phoneNumber = number,
                    callHistory = callHistory,
                    onBack = { navController.popBackStack() },
                    onMakeCall = onMakeCall
                )
            }
        }
    }
}
