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

class EndlessListActivity : Activity() {
    private var adapter: EndlessAdapter? = null

    private val testArray = arrayOf("one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eightteen", "nineteen", "twenty", "twenty1", "twenty2", "twenty3", "twenty4", "twenty5",
            "twenty6", "twenty7", "twenty8", "twenty9", "thirty", "thirty1", "thirty2", "thirty3",
            "thirty4", "thirty5", "thirty6", "thirty7", "thirty8", "thirty9", "forty")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endless_list)

        val callback = object: WindowEndlessDataSource.Callback {
            override fun needData(start: Int, length: Int): List<Any> {
                Thread.sleep(1000)
                if (start >= testArray.size){
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
    }

    companion object {
        const private val ENDLESS_LOADER = 0
    }
}
