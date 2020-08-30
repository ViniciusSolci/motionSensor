package br.com.ic.motion.sensor.activity

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import br.com.ic.motion.sensor.BuildConfig
import br.com.ic.motion.sensor.R
import br.com.ic.motion.sensor.model.ExamResult
import br.com.ic.motion.sensor.model.SensorReading
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File
import java.io.FileOutputStream
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {
    lateinit var sensorManager: SensorManager
    lateinit var accelerometer: Sensor
    private val sensorReadings: MutableList<SensorReading> = mutableListOf()
    private var readCounter: Int = 0
    private var time: Float = 0.0f
    lateinit var button: Button
    private var reading: Boolean = false
    private var shouldRead: Boolean = false
    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        button = findViewById(R.id.button)
        handler = Handler(Looper.getMainLooper())
        button.setOnClickListener {
            if (reading) {
                reading = false
                stopReading()
                button.text = "Start"
            } else {
                reading = true
                startReading()
                button.text = "Stop"
            }
        }
    }


    private fun startReading() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        handler.postDelayed(object : Runnable {
            override fun run() {
                shouldRead = true
                handler.postDelayed(this, 1000)//1 sec delay
                time+=1
            }
        }, 0)
    }

    private fun stopReading() {
        sensorManager.unregisterListener(this)
        var mapper = CsvMapper()
        var schema: CsvSchema? = mapper
            .schemaFor(SensorReading::class.java)
            .withHeader()
            .withColumnSeparator(',')
            .sortedBy("id", "time", "axisX", "axisY", "axisZ")
        var writer: ObjectWriter = mapper.writer(schema)
        val examResult = ExamResult(Date(), sensorReadings)
        val csvResult = writer.writeValueAsString(examResult.list)
        Log.i("temp.csv", csvResult)
        sendEmail(csvResult)
        sensorReadings.clear()
    }

    private fun sendEmail(csvResult: String) {
        var file = File(this.cacheDir, "temp.csv")
        val stream = FileOutputStream(file)
        stream.use { stream ->
            stream.write(csvResult.toByteArray())
            stream.flush()
        }

        Log.i("=====" , file.readLines().toString())

        var u1 = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )
        Log.i("bbbbb", u1.toString())

        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Resultado do exame")
        sendIntent.putExtra(Intent.EXTRA_STREAM, u1)
        sendIntent.type = "text/csv"
        startActivity(sendIntent)

    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (shouldRead) {
            shouldRead = false
            val sensorReading = SensorReading(
                id = ++readCounter,
                time = time,
                axisX = event.values[0],
                axisY = event.values[1],
                axisZ = event.values[2]
            )
            sensorReadings.add(sensorReading)
        }
    }
}
