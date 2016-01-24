package com.example.my.endless

import android.app.Activity
import android.app.LoaderManager
import android.content.Loader
import android.os.Bundle
import android.telecom.Call
import android.widget.BaseAdapter
import android.widget.ListAdapter
import java.util.*

class WindowEndlessDataSource(private val activity: Activity,
                              private val loaderId: Int,
                              private val callback: WindowEndlessDataSource.Callback) : EndlessDataSource, LoaderManager.LoaderCallbacks<WindowEndlessDataSource.Window> {
    override var count = 1

    companion object {
        public val WINDOW_SIZE = 20
    }

    val loader: EndlessLoader?
        get() {
            return activity.loaderManager.getLoader<List<EndlessItem>>(loaderId) as EndlessLoader?
        }

    private var window: Window
    init{
        window = Window()
        activity.loaderManager.initLoader(loaderId, null, this)
    }

    private var listener: (() -> Unit)? = null

    override fun setListener(function: () -> Unit) {
        this.listener = function
    }

    public interface Callback {
        fun needData(start: Int, length: Int): List<Any>
    }

    public class Window(internal var start: Int = 0, internal var capacity: Int = WINDOW_SIZE, var data: List<EndlessItem> = ArrayList()) {
        val end:Int
            get() = start + capacity - 1

        public val size: Int
            get() = data.size

        public fun getDataByIndex(externalIndex: Int): EndlessItem{
            //external index can be any value, maybe large
            //internal index is used to access data in the data array
            val internalIndex = externalIndex  - start
            if (internalIndex >= size || internalIndex <0){
                return EndlessItem(EndlessItem.Type.STUB)
            }
            return data[internalIndex]
        }

        fun advanceToNewPosition(newPosition: Int): Window{
            if (start<=newPosition && newPosition < end){
                return Window(start, capacity, data)
            }
            val realOffset: Int
            if (newPosition < start){
                val offset = this.start - newPosition
                realOffset = - capacity * (1 + (offset / capacity))
            } else { //newPosition >=end
                val offset = newPosition - this.end
                realOffset = capacity * (1 + offset / capacity)
            }
            return Window(start + realOffset / 2, WINDOW_SIZE)
        }

        internal fun hasPosition(position: Int): Boolean {
            return this.start <= position && position < this.start + this.capacity
        }
    }

    override fun getItem(position: Int): EndlessItem {
        if (!this.window.hasPosition(position)) {
            val newWindow = window.advanceToNewPosition(position)
            loader!!.loadWindow(newWindow)
            return EndlessItem(EndlessItem.Type.STUB, null)
        }

        return window.getDataByIndex(position)
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Window> {
        return object : EndlessLoader(activity, window) {
            override fun advanceInBackground(start: Int, length: Int): List<EndlessItem> {
                val data = callback.needData(start, length)
                val windowedData = data.map { EndlessItem(EndlessItem.Type.REAL, it) }
                return windowedData
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Window>) {
    }

    override fun onLoadFinished(loader: Loader<Window>, data: Window) {
        window = data
        if (data.size < data.capacity){
            count = window.start + window.size
        } else {
            if (count < window.start + window.size + 1){
                count = window.start + window.size + 1
            }
        }
        notifyListeners()
    }

    private fun notifyListeners() {
        listener?.invoke()
    }
}
