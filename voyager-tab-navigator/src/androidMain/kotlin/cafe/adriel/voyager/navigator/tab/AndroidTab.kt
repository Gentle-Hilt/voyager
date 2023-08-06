package cafe.adriel.voyager.navigator.tab

import androidx.compose.runtime.Composable

@Composable
public fun CurrentAndroidTab() {
    val tabNavigator = LocalAndroidTabNavigator.current
    val currentTab = tabNavigator.current

    tabNavigator.saveableState("currentTab") {
        currentTab.Content()
    }
}
