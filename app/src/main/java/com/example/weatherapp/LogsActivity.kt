package com.example.weatherapp

import android.app.LauncherActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import utils.DatabaseHelper

class LogsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val dbHelper: DatabaseHelper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)

        this.listView = this.findViewById(R.id.list_view)

        val logs: List<String> = this.dbHelper.getQueryLogs()
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, logs)
        this.listView.adapter = adapter

        this.findViewById<Button>(R.id.back_button).setOnClickListener { view ->
            this.onBackPressed()
        }
    }
}