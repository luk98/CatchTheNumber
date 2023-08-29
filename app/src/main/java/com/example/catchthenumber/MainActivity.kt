package com.example.catchthenumber

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics

class MainActivity : AppCompatActivity() {

    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val display: DisplayMetrics = resources.displayMetrics

        gameView = GameView(this, display.widthPixels, display.heightPixels, display.xdpi, display.ydpi)

        setContentView(gameView)

    }

    override fun onPause() {
        super.onPause()
        gameView?.pause()
    }

    override fun onRestart() {
        super.onRestart()
        gameView?.firstRefresh()
    }
}