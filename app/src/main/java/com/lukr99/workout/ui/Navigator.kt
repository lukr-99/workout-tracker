package com.lukr99.workout.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/** Holds the selected tab and the overlay back-stack as Compose state. */
class Navigator {
    var tab = mutableStateOf(Tab.HOME)
        private set
    val stack = mutableStateListOf<Route>()

    val top: Route? get() = stack.lastOrNull()

    fun switch(target: Tab) {
        tab.value = target
    }

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun popTo(route: Route) {
        val index = stack.indexOf(route)
        if (index >= 0) while (stack.size > index) stack.removeAt(stack.lastIndex)
    }
}
