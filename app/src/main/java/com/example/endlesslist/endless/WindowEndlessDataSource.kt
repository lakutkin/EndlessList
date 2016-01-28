package com.example.endlesslist.endless

import android.app.Activity
import android.app.LoaderManager
import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.util.Log
import android.widget.BaseAdapter
import android.widget.ListAdapter
import com.example.endlesslist.SaneAsyncLoader
import java.security.Timestamp
import java.util.*

class WindowEndlessDataSource(private val activity: Activity,
                              private val loaderId: Int,
                              private val callback: WindowEndlessDataSource.Callback) : EndlessDataSource, LoaderManager.LoaderCallbacks<Window> {

    companion object {
        //TODO pass as a parameter
        public val VIEWPORT_SIZE = 12
        public val WINDOW_SIZE = 100
        public val MARGIN = VIEWPORT_SIZE / 2
    }

    public interface Callback {
        fun needData(start: Int, length: Int): List<Any>
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

    data class ViewPortRecord(val timestamp: Long, val value: Int)

    val viewPort = ArrayList<ViewPortRecord>()
    val restartLoaderRunnable = RestartLoaderRunnable()
    override var totalCount = 1
    private var window: Window

    init{
        window = Window()
        activity.loaderManager.initLoader(loaderId, window.asBundle(), this@WindowEndlessDataSource)
    }

    private var handler = Handler(Looper.getMainLooper())

    private var listener: (() -> Unit)? = null

    override fun setListener(function: () -> Unit) {
        this.listener = function
    }



    public fun List<ViewPortRecord>.middle(): ViewPortRecord {
        return this[this.size / 2]
    }



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

    //just a subclass for AsyncLoader with start and capacity properties
    abstract class EndlessLoader(ctx: Context, var start: Int, var capacity: Int) : SaneAsyncLoader<Window>(ctx) {
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Window> {
        return object : EndlessLoader(activity, args!!.getInt("start"), args.getInt("capacity")) {
            override fun loadInBackground(): Window {
                Log.d("EndlessLoader", "Loading ${capacity} items from ${start}")
                val window = Window(start, capacity)
                window.data = callback.needData(window.start, window.capacity).map { EndlessItem(EndlessItem.Type.REAL, it) }
                Log.d("EndlessLoader", "data: ${window.data}")
                return window
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
