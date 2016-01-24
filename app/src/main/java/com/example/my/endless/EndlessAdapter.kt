package com.example.my.endless

import android.widget.BaseAdapter

abstract class EndlessAdapter(private val endlessDataSource: EndlessDataSource) : BaseAdapter() {
    init{
        endlessDataSource.setListener{
            notifyDataSetChanged()
        }
    }
    override fun getCount(): Int {
        return endlessDataSource.count
    }

    override fun getItem(position: Int): EndlessItem {
        return endlessDataSource.getItem(position)
    }

    override fun getItemId(position: Int): Long {
        return endlessDataSource.getItemId(position)
    }
}
