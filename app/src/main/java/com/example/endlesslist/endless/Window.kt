package com.example.endlesslist.endless

import android.os.Bundle
import android.util.Log
import java.util.*

class Window(internal var start: Int = 0, internal var capacity: Int = WindowEndlessDataSource.WINDOW_SIZE, var data: List<EndlessItem> = ArrayList(capacity)) {
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

    public fun closeToEdge(newPosition: Int) = start + WindowEndlessDataSource.MARGIN > newPosition || newPosition > end - WindowEndlessDataSource.MARGIN
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
        data = ArrayList(WindowEndlessDataSource.WINDOW_SIZE)
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