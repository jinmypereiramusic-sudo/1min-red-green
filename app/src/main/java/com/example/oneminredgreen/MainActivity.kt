package com.example.oneminredgreen

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val rng = SecureRandom()
    private val prefs by lazy { getSharedPreferences("rounds", MODE_PRIVATE) }

    private lateinit var roundText: TextView
    private lateinit var timerText: TextView
    private lateinit var resultText: TextView
    private lateinit var colorText: TextView
    private lateinit var suggestionText: TextView
    private lateinit var historyText: TextView
    private lateinit var statusText: TextView

    private var activeRound = -1L
    private var suggestionRound = -1L

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateClock()
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        scroll.addView(root)
        setContentView(scroll)

        fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 8, 8, 8)
            }
        }
        fun add(view: TextView, top: Int = 8) {
            root.addView(view, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = top })
        }

        add(text("1MIN RED GREEN", 27f, Color.WHITE, true), 4)
        add(text("Sorteo simulado · una ronda por minuto", 14f, Color.LTGRAY))
        roundText = text("Ronda del día: -- / 1440", 17f, Color.WHITE, true); add(roundText, 22)
        timerText = text("00:00", 38f, Color.rgb(96, 165, 250), true); add(timerText, 4)
        add(text("RESULTADO DE LA RONDA", 14f, Color.LTGRAY, true), 20)
        resultText = text("–", 76f, Color.WHITE, true); add(resultText, 0)
        colorText = text("Esperando resultado", 19f, Color.WHITE, true); add(colorText, 0)

        val suggestButton = Button(this).apply {
            text = "GENERAR SUGERENCIA ALEATORIA"
            setOnClickListener { generateSuggestion(force = true) }
        }
        root.addView(suggestButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 24 })

        suggestionText = text("Pulsa el botón para obtener una sugerencia.", 17f, Color.WHITE, true)
        add(suggestionText, 10)
        statusText = text(
            "Probabilidades teóricas: número exacto 10 % · Red 50 % · Green 50 % · Purple 20 %.",
            13f, Color.LTGRAY
        )
        add(statusText, 8)

        add(text("REGLAS DE COLORES", 16f, Color.WHITE, true), 24)
        add(text("GREEN: 1, 3, 5, 7, 9\nRED: 0, 2, 4, 6, 8\nPURPLE: 0 y 5", 15f, Color.LTGRAY))
        add(text("HISTORIAL RECIENTE", 16f, Color.WHITE, true), 24)
        historyText = text("Aún no hay rondas registradas.", 14f, Color.LTGRAY)
        add(historyText, 4)
        add(text(
            "Aviso: esta app es una simulación local. No está conectada a una lotería oficial, " +
            "no verifica resultados externos y no puede garantizar aciertos ni una probabilidad del 80 %.",
            12f, Color.rgb(251, 191, 36)
        ), 24)
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        val roundId = now / 60_000L
        val secondsLeft = 60 - ((now / 1000L) % 60L)
        val date = Date(now)
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val roundOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                cal.get(java.util.Calendar.MINUTE) + 1

        timerText.text = String.format(Locale.US, "00:%02d", secondsLeft)
        roundText.text = "Ronda del día: $roundOfDay / 1440"

        if (roundId != activeRound) {
            activeRound = roundId
            val result = prefs.getInt("round_$roundId", -1).let {
                if (it in 0..9) it else rng.nextInt(10).also {
                    prefs.edit().putInt("round_$roundId", it).apply()
                }
            }
            showResult(result)
            saveHistory(roundId, result)
            generateSuggestion(force = true)
        }
    }

    private fun showResult(number: Int) {
        resultText.text = number.toString()
        val colorName = when {
            number == 0 || number == 5 -> "PURPLE"
            number % 2 == 1 -> "GREEN"
            else -> "RED"
        }
        val color = when (colorName) {
            "GREEN" -> Color.rgb(34, 197, 94)
            "RED" -> Color.rgb(239, 68, 68)
            else -> Color.rgb(168, 85, 247)
        }
        resultText.setTextColor(color)
        colorText.text = colorName
        colorText.setTextColor(color)
    }

    private fun generateSuggestion(force: Boolean) {
        val roundId = System.currentTimeMillis() / 60_000L
        if (!force && suggestionRound == roundId) return
        suggestionRound = roundId

        // Elección uniforme entre los diez dígitos: no es una predicción con ventaja.
        val number = rng.nextInt(10)
        val color = when {
            number == 0 || number == 5 -> "PURPLE"
            number % 2 == 1 -> "GREEN"
            else -> "RED"
        }
        val exact = "10 %"
        val category = if (color == "PURPLE") "20 %" else "50 %"
        suggestionText.text = "Sugerencia aleatoria: $number · $color\n" +
                "Probabilidad teórica del número: $exact\n" +
                "Probabilidad de su categoría: $category\n" +
                "No representa una ventaja estadística."
    }

    private fun saveHistory(roundId: Long, number: Int) {
        val key = "history"
        val old = prefs.getString(key, "") ?: ""
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(roundId * 60_000L))
        val entry = "$time  →  $number (${colorOf(number)})"
        val updated = (listOf(entry) + old.split("|").filter { it.isNotBlank() }).take(20)
        prefs.edit().putString(key, updated.joinToString("|")).apply()
        historyText.text = updated.joinToString("\n")
    }

    private fun colorOf(n: Int) = when {
        n == 0 || n == 5 -> "PURPLE"
        n % 2 == 1 -> "GREEN"
        else -> "RED"
    }
}
