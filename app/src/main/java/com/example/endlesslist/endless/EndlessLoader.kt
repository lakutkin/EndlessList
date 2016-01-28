package com.example.endlesslist.endless

import android.content.Context
import android.util.Log

import com.example.endlesslist.SaneAsyncLoader

abstract class EndlessLoader(ctx: Context, var start: Int, var capacity: Int) : SaneAsyncLoader<WindowEndlessDataSource.Window>(ctx) {

    init{
        setUpdateThrottle(500)
    }

    override fun loadInBackground(): WindowEndlessDataSource.Window {
        Log.d("EndlessLoader", "Loading ${capacity} items from ${start}")
        val window = WindowEndlessDataSource.Window(start, capacity)
        window.data = advanceInBackground(window.start, window.capacity)
        Log.d("EndlessLoader", "data: ${window.data}")
        return window
    }
    abstract fun advanceInBackground(start: Int, length: Int): List<EndlessItem>
}
