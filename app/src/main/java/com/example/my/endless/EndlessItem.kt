package com.example.my.endless

data class EndlessItem(val type: EndlessItem.Type, val obj: Any? = null) {
    enum class Type {
        REAL, STUB
    }
}
