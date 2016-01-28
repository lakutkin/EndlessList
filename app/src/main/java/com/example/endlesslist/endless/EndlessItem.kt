package com.example.endlesslist.endless

data class EndlessItem(val type: EndlessItem.Type, val obj: Any? = null) {
    enum class Type {
        REAL, STUB
    }
}
