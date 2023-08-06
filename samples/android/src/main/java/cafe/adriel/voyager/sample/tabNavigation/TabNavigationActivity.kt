package cafe.adriel.voyager.sample.tabNavigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.tab.AndroidTabDisposable
import cafe.adriel.voyager.navigator.tab.AndroidTabNavigator
import cafe.adriel.voyager.navigator.tab.CurrentAndroidTab
import cafe.adriel.voyager.navigator.tab.LocalAndroidTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.sample.tabNavigation.tabs.FavoritesTab
import cafe.adriel.voyager.sample.tabNavigation.tabs.HomeTab
import cafe.adriel.voyager.sample.tabNavigation.tabs.ProfileTab

class TabNavigationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Content()
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun Content() {
        val tabTitlesToTabs = mutableMapOf(
            // When i modified TabOptions and added tab directly the backstack was not working correctly for some reason
            HomeTab.options.title to HomeTab,
            ProfileTab.options.title to ProfileTab,
            FavoritesTab.options.title to FavoritesTab
        )

        AndroidTabNavigator(
            noBackStackGoToThePreviousTab = true,
            noBackStackGoToTheInitialTab = false,
            titlesToTabs = tabTitlesToTabs,
            initialTab = HomeTab,
            tabDisposable = {
                AndroidTabDisposable(
                    tabs = listOf(HomeTab, FavoritesTab, ProfileTab),
                    tabNavigator = it,
                )
            },
        ) { tabNavigator ->
            val history by tabNavigator.tabHistory.collectAsState()
            Log.d("tag", "tabHistory: $history")

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = tabNavigator.current.options.title) }
                    )
                },
                content = {
                    CurrentAndroidTab()
                },
                bottomBar = {
                    BottomNavigation {
                        TabNavigationItem(HomeTab)
                        TabNavigationItem(FavoritesTab)
                        TabNavigationItem(ProfileTab)
                    }
                }
            )
        }
    }

    @Composable
    private fun RowScope.TabNavigationItem(tab: Tab) {
        val tabNavigator = LocalAndroidTabNavigator.current
        val tabTitle = tabNavigator.current.options.title
        BottomNavigationItem(
            selected = tabNavigator.current.key == tab.key,
            onClick = {
                tabNavigator.updateTabHistory(tabTitle)
                tabNavigator.current = tab
            },
            icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) }
        )
    }
}
