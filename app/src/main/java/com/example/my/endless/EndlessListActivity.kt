package com.example.my.endless

import android.app.Activity
import android.app.LoaderManager
import android.content.Loader
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView

import com.example.my.myapplication.R
import java.util.*
import kotlin.jvm.internal.iterator

class EndlessListActivity : Activity() {
    private var adapter: EndlessAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endless_list)

        var testArray = Array(1000, { "number $it" })

        val callback = object: WindowEndlessDataSource.Callback {
            override fun needData(start: Int, length: Int): List<Any> {
                Thread.sleep(500)
                if (start >= testArray.size || start < 0){
                    return Collections.emptyList()
                }
                return testArray.slice(start..Math.min(testArray.size - 1, start + length - 1))
            }
        }

        adapter = object : EndlessAdapter(WindowEndlessDataSource(this, ENDLESS_LOADER, callback)) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = LayoutInflater.from(this@EndlessListActivity).inflate(android.R.layout.simple_list_item_1, parent, false)
                val text = view.findViewById(android.R.id.text1) as TextView
                text.text = getItem(position).toString()
                return view
            }
        }

        val listView = findViewById(R.id.list) as ListView
        listView.adapter = adapter

        findViewById(R.id.button).setOnClickListener {
            (adapter as EndlessAdapter).dropCache()
        }
    }

    companion object {
        const private val ENDLESS_LOADER = 0
    }
}
