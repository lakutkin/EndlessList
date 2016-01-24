package com.example.my.endless

interface EndlessDataSource {
    val totalCount: Int

    fun getItem(position: Int): EndlessItem

    fun getItemId(position: Int): Long

    fun setListener(function: () -> Unit)

    fun dropCache()

}
