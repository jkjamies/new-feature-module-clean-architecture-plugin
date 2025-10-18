package ${PACKAGE}

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ${FLOW_NAME} {
    private val _state = MutableStateFlow(Unit)
    val state: StateFlow<Unit> = _state
}
