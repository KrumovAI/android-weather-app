package utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "logs"
private const val QUERY_LOGS_TABLE = "query_logs"

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        val query: String = "CREATE TABLE $QUERY_LOGS_TABLE(id INTEGER PRIMARY KEY, url TEXT)"
        db?.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val query: String = "DROP TABLE IF EXISTS $QUERY_LOGS_TABLE"
        db?.execSQL(query)
        this.onCreate(db)
    }

    public fun addQueryLog(url: String) {
        val database = this.writableDatabase
        val content = ContentValues()
        content.put("url", url)

        database.insert(QUERY_LOGS_TABLE, null, content)
    }

    public fun getQueryLogs(): ArrayList<String> {
        val logs = ArrayList<String>()
        val query: String = "SELECT * FROM $QUERY_LOGS_TABLE"

        val cursor = this.readableDatabase.rawQuery(query, null)
        cursor.moveToFirst()

        while (!cursor.isAfterLast) {
            logs.add(cursor.getString(cursor.getColumnIndex("url")))
            cursor.moveToNext()
        }

        return logs
    }
}
