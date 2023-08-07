package cafe.adriel.voyager.navigator.tab

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.compositionUniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

public typealias TabAndroidNavigatorContent = @Composable (tabNavigator: AndroidTabNavigator) -> Unit

public val LocalAndroidTabNavigator: ProvidableCompositionLocal<AndroidTabNavigator> =
    staticCompositionLocalOf { error("TabNavigator not initialized") }

@Composable
public fun AndroidTabNavigator(
    noBackStackGoToThePreviousTab: Boolean = false,
    noBackStackGoToTheInitialTab: Boolean = false,
    titlesToTabs: MutableMap<String, Tab> = mutableMapOf(),
    initialTab: Tab,
    disposeNestedNavigators: Boolean = false,
    tabDisposable: (@Composable (AndroidTabNavigator) -> Unit)? = null,
    key: String = compositionUniqueId(),
    content: TabAndroidNavigatorContent = { CurrentTab() },
) {

    Navigator(
        screen = initialTab,
        disposeBehavior = NavigatorDisposeBehavior(
            disposeNestedNavigators = disposeNestedNavigators,
            disposeSteps = false
        ),
        onBackPressed = null,
        key = key,
    ) { navigator ->
        val scope = rememberCoroutineScope()
        val tabNavigator = remember(navigator) {
            AndroidTabNavigator(navigator, scope)
        }

        val tabHistory by tabNavigator.tabHistory.collectAsStateWithLifecycle()

        tabDisposable?.invoke(tabNavigator)

        CompositionLocalProvider(LocalAndroidTabNavigator provides tabNavigator) {
            if (noBackStackGoToTheInitialTab) {
                BackHandler(
                    enabled = tabNavigator.current != initialTab,
                    onBack = { tabNavigator.current = initialTab }
                )
            }
            if (noBackStackGoToThePreviousTab) {
                BackHandler(
                    enabled = tabHistory.isNotEmpty(),
                    onBack = {
                        scope.launch {
                            tabNavigator.popTab()
                            val lastTabTitle = tabHistory.last()
                            val tab = tabNavigator.getTabByTitle(lastTabTitle, titlesToTabs, initialTab)
                            tabNavigator.current = tab
                        }
                    }
                )
            }
            content(tabNavigator)
        }
    }
}

@Composable
public fun AndroidTabDisposable(
    tabNavigator: AndroidTabNavigator,
    tabs: List<Tab>
) {
    DisposableEffectIgnoringConfiguration(Unit) {
        onDispose {
            tabs.forEach { tab ->
                tabNavigator.navigator.dispose(tab)
            }
        }
    }
}

private val tabReselectHandlers = mutableListOf<suspend () -> Boolean>()

public class AndroidTabNavigator internal constructor(
    public val navigator: Navigator,
    private val coroutineScope: CoroutineScope,
    public val stateHolder: SaveableStateHolder = navigator.stateHolder
) {

    public var current: Tab
        get() = navigator.lastItem as Tab
        set(tab) {
            if (navigator.lastItem == tab) {
                coroutineScope.launch {
                    tabReselectHandlers.reversed().firstOrNull { it.invoke() }
                }
            } else {
                navigator.replaceAll(tab)
            }
        }

    public val tabHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    public fun updateTabHistory(title: String) {
        tabHistory.value = tabHistory.value + title

        TabHistory.tabList = TabHistory.tabList + title
    }

    public fun popTab() {
        tabHistory.value = tabHistory.value.dropLast(1)

        TabHistory.tabList = TabHistory.tabList.dropLast(1)
        tabHistory.value = TabHistory.tabList
    }

    public fun syncTabHistory() {
        tabHistory.value = TabHistory.tabList
    }

    public fun getTabByTitle(
        title: String,
        tabTitlesToTabs: Map<String, Tab>,
        initialTab: Tab
    ): Tab {
        return tabTitlesToTabs[title] ?: initialTab
    }

    @Composable
    public fun saveableState(
        key: String,
        tab: Tab = current,
        content: @Composable () -> Unit
    ) {
        syncTabHistory()
        navigator.saveableState(key, tab, content = content)
    }
}

@Composable
public fun TabReselectHandler(handler: suspend () -> Boolean) {
    DisposableEffect(Unit) {
        tabReselectHandlers.add(handler)
        onDispose { tabReselectHandlers.remove(handler) }
    }
}
