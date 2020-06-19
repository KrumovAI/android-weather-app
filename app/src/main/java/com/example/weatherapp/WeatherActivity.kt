package com.example.weatherapp

import WeatherData
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import utils.DatabaseHelper
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class WeatherActivity : AppCompatActivity() {

    private lateinit var location: LatLng
    private lateinit var currentWeatherTempView: TextView
    private lateinit var currentWeatherHumView: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var anyChartView: AnyChartView
    private lateinit var logsButton: Button

    private var currentWeather: WeatherData? = null
    private val forecast: ArrayList<WeatherData> = ArrayList<WeatherData>()

    private lateinit var chart: Cartesian

    private var databaseHelper: DatabaseHelper =
        DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        this.currentWeatherTempView = this.findViewById(R.id.current_weather_temp)
        this.currentWeatherHumView = this.findViewById(R.id.current_weather_hum)
        this.radioGroup = this.findViewById(R.id.forecast_radio_group)
        this.anyChartView = this.findViewById(R.id.any_chart_view)
        this.logsButton = this.findViewById(R.id.logs_button)

        this.chart = AnyChart.column()
        this.anyChartView.setChart(this.chart)

        this.radioGroup.setOnCheckedChangeListener { radioGroup, optionId ->
            run {
                when (optionId) {
                    R.id.radio_today -> {
                        this.setTodayData()
                    }
                    R.id.radio_three_day -> {
                        this.setDailyData(3)
                    }
                    R.id.radio_five_day -> {
                        this.setDailyData(5)
                    }
                    // there is no 16-Days option since its not included in the free tier
                }
            }
        }

        this.logsButton.setOnClickListener {
            val intent: Intent = Intent(this, LogsActivity::class.java)
            this.startActivity(intent)
        }

        this.location = LatLng(this.intent.getDoubleExtra("latitude", 0.0), this.intent.getDoubleExtra("longitude", 0.0))

        currentWeatherTask().execute()
        forecastTask().execute()

        this.findViewById<Button>(R.id.back_button).setOnClickListener { view ->
            this.onBackPressed()
        }
    }

    private fun getUrl(endpoint: String): String {
        return "${baseContext.getString(R.string.weather_api_url)}${endpoint}?lat=${location.latitude}&lon=${location.longitude}&units=metric&appid=${baseContext.getString(R.string.weather_api_key)}"
    }

    private fun setDailyData(days: Int) {
        if (this.forecast.isEmpty()) {
            return
        }

        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val grouped = this.forecast.groupBy { dateFormat.format(it.date) }

        val tempData: List<DataEntry> = grouped
            .map { item -> ValueDataEntry(item.key, item.value.map { it.temperature }.average().roundToInt()) }
            .toTypedArray()
            .copyOfRange(0, days)
            .toList()

        val humData: List<DataEntry> = grouped
            .map { item -> ValueDataEntry(item.key, (item.value.map { it.humidity }.average()).roundToInt()) }
            .toTypedArray()
            .copyOfRange(0, days)
            .toList()

        this.updateCharts(tempData, humData)
    }

    private fun setTodayData() {
        if (this.forecast.isEmpty()) {
            return
        }

        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val timeFormat: DateFormat = SimpleDateFormat("HH:mm")
        val grouped = this.forecast.groupBy { dateFormat.format(it.date) }

        val firstKey = grouped.keys.first()

        val tempData: List<DataEntry>? = grouped[firstKey]?.map { ValueDataEntry(timeFormat.format(it.date),
            it.temperature.roundToInt()) }?.toList()
        val humData: List<DataEntry>? = grouped[firstKey]?.map { ValueDataEntry(timeFormat.format(it.date),
            it.humidity.roundToInt()) }?.toList()

        this.updateCharts(tempData, humData)
    }

    private fun updateCharts(temperatureData: List<DataEntry>?, humidityData: List<DataEntry>?) {

        this.chart.removeAllSeries()

        this.chart
            .column(temperatureData)
            .name(baseContext.getString(R.string.temperature))
            .fill("red")
            .stroke("red")
            .labels()
            .format("{%value} \u2103")
            .enabled(true)

        this.chart
            .line(humidityData)
            .name(baseContext.getString(R.string.humidity))
            .labels()
            .format("{%value}%")
            .enabled(true)

        this.chart.legend(true)
    }

    inner class currentWeatherTask() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            var response: String?

            try {
                val url: String = getUrl("weather")
                databaseHelper.addQueryLog(url)

                response = URL(url).readText(
                    Charsets.UTF_8
                )
            } catch (e: Exception) {
                response = null
            }

            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result.isNullOrBlank()) {
                return
            }

            val jsonObj = JSONObject(result)

            val main = jsonObj.getJSONObject("main")
            val temp = main.getDouble("temp")
            val humidity = main.getDouble("humidity")

            val weatherData = WeatherData(Date(), temp, humidity)
            currentWeather = weatherData

            currentWeatherTempView.text = "${weatherData.temperature.roundToInt().toString()} \u2103"
            currentWeatherHumView.text = "${weatherData.humidity.roundToInt().toString()}%"
        }
    }

    inner class forecastTask() : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String? {
            var response: String?

            try {
                val url: String = getUrl("forecast")
                databaseHelper.addQueryLog(url)

                response = URL(url).readText(
                    Charsets.UTF_8
                )
            } catch (e: Exception){
                response = null
            }

            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result.isNullOrBlank()) {
                return
            }

            val jsonObj = JSONObject(result)
            val list = jsonObj.getJSONArray("list")

            forecast.clear()

            val dateTimeFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

            for (i in 0 until list.length()) {
                val item: JSONObject = list.getJSONObject(i)
                val main = item.getJSONObject("main")
                val date = dateTimeFormat.parse(item.getString("dt_txt"))
                val temp = main.getDouble("temp")
                val humidity = main.getDouble("humidity")

                val weatherData = WeatherData(date, temp, humidity)
                forecast.add(weatherData)
            }

            setTodayData()
        }
    }
}