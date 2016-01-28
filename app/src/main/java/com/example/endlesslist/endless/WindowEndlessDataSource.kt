package com.example.endlesslist.endless

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
import java.security.Timestamp
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
        //TODO pass as a parameter
        public val VIEWPORT_SIZE = 12
        public val WINDOW_SIZE = 100
        public val MARGIN = VIEWPORT_SIZE / 2
    }

    private var handler = Handler(Looper.getMainLooper())

    private var listener: (() -> Unit)? = null

    override fun setListener(function: () -> Unit) {
        this.listener = function
    }

    public interface Callback {
        fun needData(start: Int, length: Int): List<Any>
    }

    class Window(internal var start: Int = 0, internal var capacity: Int = WINDOW_SIZE, var data: List<EndlessItem> = ArrayList(capacity)) {
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

        public fun closeToEdge(newPosition: Int) = start + MARGIN > newPosition || newPosition > end - MARGIN
        private fun insideEdges(newPosition: Int) = !closeToEdge(newPosition)

        internal fun hasPosition(position: Int): Boolean {
            return this.start <= position && position < this.start + this.capacity
        }

        //TODO функция мержа бажная для, например, случая
        //Merging [start: 0, end: 99, capacity: 100, size: 100] and [start: 246, end: 301, capacity: 56, size: 56]
        //Merged: [start: 202, end: 301, capacity: 100, size: 100]
        //должно было получиться merged: [start: 246, end: 301, capacity: 56, size: 56], т.к. эти множества не пересекаются
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

    public fun List<ViewPortRecord>.middle(): ViewPortRecord {
        return this[this.size / 2]
    }

    public inner class RestartLoaderRunnable(): Runnable{
        public override fun run() {
            val sorted = viewPort.sortedBy { it.timestamp }.takeLast(VIEWPORT_SIZE)
            viewPort.clear()
            val stub = sorted.any{ !window.hasPosition(it.value) || window.getDataByIndex(it.value).type == EndlessItem.Type.STUB }
            val closeToTop = sorted.any { window.start <= it.value && it.value <= window.start + MARGIN }
            val closeToBottom = sorted.any {window.end >= it.value && it.value >= window.end - MARGIN}

            if (!closeToTop && !closeToBottom && !stub){
                return
            }

            val newWindow: Window

            if (closeToTop){
                Log.d("EndlessLoader", "closeToTop")
                if (window.start == 0){
                    return
                }
                newWindow = Window(Math.max(0, sorted.last().value - WINDOW_SIZE + 3 * VIEWPORT_SIZE), WINDOW_SIZE - 3 * VIEWPORT_SIZE)
            } else if (closeToBottom) {
                Log.d("EndlessLoader", "closeToBottom")
                newWindow = Window(sorted.last().value, WINDOW_SIZE - 3 * VIEWPORT_SIZE)
            } else {//stub
                Log.d("EndlessLoader", "stubs")
                newWindow = Window(Math.max(0, sorted.middle().value - WINDOW_SIZE / 2), WINDOW_SIZE)
            }

            Log.d("EndlessLoader", "Restarting loader for window $newWindow, $this")
            activity.loaderManager.restartLoader(loaderId, newWindow.asBundle(), this@WindowEndlessDataSource)
        }
    }

    val restartLoaderRunnable = RestartLoaderRunnable()

    data class ViewPortRecord(val timestamp: Long, val value: Int)

    val viewPort = ArrayList<ViewPortRecord>()

    override fun getItem(position: Int): EndlessItem {
        val viewPortRecord = ViewPortRecord(System.currentTimeMillis(), position)
        viewPort.add(viewPortRecord)
        Log.d("EndlessLoader", "tracking position $viewPortRecord")
        handler.removeCallbacks(restartLoaderRunnable)
        handler.postDelayed(restartLoaderRunnable, 500)
        if (!window.hasPosition(position)){
            return EndlessItem(EndlessItem.Type.STUB, null)
        } else {
            return window.getDataByIndex(position)
        }
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
