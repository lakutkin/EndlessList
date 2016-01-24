package com.example.my.endless

interface EndlessDataSource {
    val count: Int

    fun getItem(position: Int): EndlessItem

    fun getItemId(position: Int): Long

    fun setListener(function: () -> Unit)

}
