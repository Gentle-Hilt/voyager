package cafe.adriel.voyager.navigator.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

public object TabHistory {
    public var tabList: List<String> by mutableStateOf(emptyList())
}
