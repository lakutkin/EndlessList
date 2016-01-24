package com.example.my.endless

import android.content.Context
import android.util.Log

import com.example.my.SaneAsyncLoader

abstract class EndlessLoader(ctx: Context, var currentWindow: WindowEndlessDataSource.Window) : SaneAsyncLoader<WindowEndlessDataSource.Window>(ctx) {
    override fun loadInBackground(): WindowEndlessDataSource.Window {
        Log.d("EndlessLoader", "Loading ${currentWindow.capacity} items from ${currentWindow.start}")
        currentWindow.data = advanceInBackground(currentWindow.start, currentWindow.capacity)
        return currentWindow
    }
    abstract fun advanceInBackground(start: Int, length: Int): List<EndlessItem>

    fun loadWindow(window: WindowEndlessDataSource.Window) {
        cancelLoad()
        this.currentWindow = window
        forceLoad()
    }
}
