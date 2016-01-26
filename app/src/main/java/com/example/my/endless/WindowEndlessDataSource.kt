package com.example.my.endless

import android.app.Activity
import android.app.LoaderManager
import android.content.Loader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.util.Log
import android.widget.BaseAdapter
import android.widget.ListAdapter
import java.util.*

class WindowEndlessDataSource(private val activity: Activity,
                              private val loaderId: Int,
                              private val callback: WindowEndlessDataSource.Callback) : EndlessDataSource, LoaderManager.LoaderCallbacks<WindowEndlessDataSource.Window> {
    override var totalCount = 1
    private var window: Window

    init{
        window = Window()
        activity.loaderManager.initLoader(loaderId, window.asBundle(), this@WindowEndlessDataSource)
    }

    companion object {
        public val WINDOW_SIZE = 40
        public val SMALL_WINDOW_SIZE = 20
    }


    private var handler = Handler(Looper.getMainLooper())

    private var listener: (() -> Unit)? = null

    override fun setListener(function: () -> Unit) {
        this.listener = function
    }

    public interface Callback {
        fun needData(start: Int, length: Int): List<Any>
    }

    public class Window(internal var start: Int = 0, internal var capacity: Int = WINDOW_SIZE, var data: List<EndlessItem> = ArrayList(capacity)) {
        public val end: Int
            get() = start + capacity - 1

        private val endOfData: Int
            get() = start + size - 1


        public val size: Int
            get() = data.size

        public fun getDataByIndex(externalIndex: Int): EndlessItem {
            //external index can be any value, maybe large
            //internal index is used to access data in the data array
            val internalIndex = externalIndex - start
            if (internalIndex >= size || internalIndex < 0) {
                return EndlessItem(EndlessItem.Type.STUB)
            }
            return data[internalIndex]
        }

        private fun calculateStartBoundary(position: Int, capacity: Int): Int{
            return (position / capacity) * capacity
        }

        public fun advanceToNewPosition(newPosition: Int): Window {
            if (start <= newPosition && newPosition <= end) {
                return Window(start, capacity, data)
            }
            val retVal: Window
            if (newPosition < start) {
                retVal = Window(Math.max(0, calculateStartBoundary(newPosition, SMALL_WINDOW_SIZE)), SMALL_WINDOW_SIZE)
            } else {
                //newPosition >end
                retVal = Window(calculateStartBoundary(newPosition, SMALL_WINDOW_SIZE), SMALL_WINDOW_SIZE)
            }
            Log.d("EndlessLoader", "New position: $newPosition: window moved to $retVal")
            return retVal
        }

        internal fun hasPosition(position: Int): Boolean {
            return this.start <= position && position < this.start + this.capacity
        }

        public fun merge(otherWindow: Window) {
            Log.d("EndlessLoader", "Merging $this and $otherWindow")

            var newStart: Int
            var newEnd: Int

            if (start <= otherWindow.start) {
                //new window is to the right
                newEnd = Math.max(end, otherWindow.endOfData)
                newStart = (newEnd + 1) - capacity
            } else {
                //new window is to the left
                newStart = Math.min(start, otherWindow.start)
            }
            var newData = Array(capacity, fun (i: Int): EndlessItem{
                val index = i + newStart
                if (otherWindow.hasPosition(index)){
                    return otherWindow.getDataByIndex(index)
                } else if (hasPosition(index)){
                    return getDataByIndex(index)
                } else {
                    return  EndlessItem(EndlessItem.Type.STUB)
                }
            })
            data = newData.asList()
            start = newStart
            Log.d("EndlessLoader", "Merged: $this")
        }

        public fun reset() {
            data = ArrayList(WINDOW_SIZE)
        }

        public fun asBundle(): Bundle {
            val b = Bundle()
            b.putInt("start", start)
            b.putInt("capacity", capacity)
            return b
        }

        public override fun toString(): String {
            return "[start: $start, end: $end, capacity: $capacity, size: $size]"
        }
    }

    public inner class RestartLoaderRunnable(var newWindow: Window): Runnable{
        public override fun run() {
            Log.d("EndlessLoader", "Restarting loader for window $newWindow, $this")
            activity.loaderManager.restartLoader(loaderId, newWindow.asBundle(), this@WindowEndlessDataSource)
        }
    }

    val restartLoaderRunnable = RestartLoaderRunnable(window)

    override fun getItem(position: Int): EndlessItem {
        val dataByIndex = window.getDataByIndex(position)
        if (!window.hasPosition(position) || dataByIndex.type == EndlessItem.Type.STUB) {
            val newWindow = window.advanceToNewPosition(position)
            restartLoaderRunnable.newWindow = newWindow
            Log.d("EndlessLoader", "removing callback $restartLoaderRunnable from $this")
            handler.removeCallbacks(restartLoaderRunnable)
            Log.d("EndlessLoader", "postDelayed $restartLoaderRunnable")
            handler.postDelayed(restartLoaderRunnable, 500)
            return EndlessItem(EndlessItem.Type.STUB, null)
        }

        return dataByIndex
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Window> {
        return object : EndlessLoader(activity, args!!.getInt("start"), args.getInt("capacity")) {
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
        window.merge(data)
        if (data.size < data.capacity) {
            totalCount = window.start + window.size
        } else {
            if (totalCount < window.start + window.size + 1) {
                totalCount = window.start + window.size + 1
            }
        }
        Log.d("EndlessList", "OnLoadFinished: totalCount = $totalCount")
        notifyListeners()
    }

    override fun dropCache() {
        window.reset()
        notifyListeners()
    }

    private fun notifyListeners() {
        listener?.invoke()
    }
}
