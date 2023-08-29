package com.example.catchthenumber

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class GameView (context: Context, private val x: Int, private val y: Int, xdpi: Float, ydpi: Float): SurfaceView(context), Runnable{


    private var thread: Thread? = null

    private var timer: Timer = Timer()

    private var defaultTime: Int = 100
    private var defaultSecondTime: Int = 100

    private var time: Int = defaultTime
    private var secondTime: Int = defaultSecondTime

    private var paint: Paint = Paint()

    private var myBitmap: Bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.RGB_565)
    private var myCanvas: Canvas = Canvas(myBitmap)

    private val xINcm: Int = (xdpi/3.2).toInt()
    private val yINcm: Int = (ydpi/3.2).toInt()

    private var timeBitmap: Bitmap = Bitmap.createBitmap(xINcm*2, yINcm*2, Bitmap.Config.RGB_565)
    private var timeCanvas: Canvas = Canvas(timeBitmap)

    private val yCircles = y/yINcm
    private val xCircles = x/xINcm - 6

    private val borderY: Int = (y%yINcm)/2
    private var borderX: Int = (x-xCircles*xINcm)/2

    private var permission: Boolean = true
    private var postTimer: Boolean = false

    private var isRunning: Boolean = false

    private var preGame: Boolean = true

    private var gameOver: Boolean = false
    private var readyToEnd: Boolean = false

    private val numbers = arrayOf(
        bMap(R.drawable.zero),
        bMap(R.drawable.one),
        bMap(R.drawable.two),
        bMap(R.drawable.thre),
        bMap(R.drawable.four),
        bMap(R.drawable.five),
        bMap(R.drawable.six),
        bMap(R.drawable.seven),
        bMap(R.drawable.eight),
        bMap(R.drawable.nine)
    )

    private val circles = arrayOf(
        bMap(R.drawable.grey),
        bMap(R.drawable.black),
        bMap(R.drawable.doubleblack),
        bMap(R.drawable.tripleblack)
    )

    private val indicators = arrayOf(
        bMap(R.drawable.hud, 2),
        bMap(R.drawable.doubletimehud, 2, 2)
    )

    private val buttons = arrayOf(
        bMap(R.drawable.playbut),
        bMap(R.drawable.pausebut),
        bMap(R.drawable.stopbut)
    )

    private val play: Bitmap = bMap(R.drawable.play, 3, 2)
    private val gOver: Bitmap = bMap(R.drawable.gameover, 7, 2)

    private var delta = arrayOf(2,1)
    private var quantity = Array(9) { 0 }
    private var matrix = mutableMapOf<Point, Point>()

    private var sharedPreferences: SharedPreferences = context.getSharedPreferences("CTN_SCORE", AppCompatActivity.MODE_PRIVATE)

    private var bestScore: Int = getScore()
    private var newScore: Int = 0


    init {

        paint.color = Color.WHITE

        firstDraw()

        firstRefresh()

        timer.scheduleAtFixedRate(createTask(), 0, 100)

    }


    override fun run() {

        while(!holder.surface.isValid){
            sleep()
        }
        refresh()
    }

    private fun sleep(){

        try {
            Thread.sleep(5)
        } catch (e:InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun refresh(){
        if(holder.surface.isValid){
            val canvas:Canvas = holder.lockCanvas()
            canvas.drawBitmap(myBitmap, 0.toFloat(), 0.toFloat(), paint)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun firstRefresh(){
        thread=Thread(this)
        thread?.start()
    }

    private fun bMap (id: Int, x: Int = 1, y: Int = 1): Bitmap{
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, id), xINcm*x, yINcm*y, false)
    }

    private fun clearScreen(){
        myCanvas.drawRect((borderX).toFloat(), (borderY).toFloat(), (x-borderX).toFloat(), (y-borderY).toFloat(), paint)
    }

    private fun toMatrix(x: Int, y: Int):Point{

        val a = if((x-borderX)%xINcm == 0) {
            (x-borderX)/xINcm-1
        } else {
            (x-borderX)/xINcm
        }

        val b = if((y-borderY)%yINcm == 0) {
            (y-borderY)/yINcm-1
        } else {
            (y-borderY)/yINcm
        }

        return P(a,b)
    }

    private fun saveScore(){
        sharedPreferences.edit().putInt("CTN_SCORE", newScore).apply()
    }

    private fun getScore(): Int{
        return sharedPreferences.getInt("CTN_SCORE", 0)
    }



    fun pause(){
        isRunning = false
        if(!gameOver) secondTime = 0
        myCanvas.drawBitmap(circles[1], (borderX+xINcm*xCircles+xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
        myCanvas.drawBitmap(buttons[0], (borderX+xINcm*xCircles+xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
    }

    private fun unPause(){
        myCanvas.drawBitmap(circles[1], (borderX+xINcm*xCircles+xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
        myCanvas.drawBitmap(buttons[1], (borderX+xINcm*xCircles+xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
        refresh()
        isRunning = true
    }



    private fun firstDraw(){

        myCanvas.drawARGB(255, 255, 255, 255)
        myCanvas.drawBitmap(play, (x/2 - play.width/2).toFloat(), (y/2 - play.height/2).toFloat(), paint)

        timeDraw()
        myCanvas.drawBitmap(timeBitmap, (borderX+xINcm*xCircles + xINcm).toFloat(), borderY.toFloat(), paint)

        scoreDraw(true)

        myCanvas.drawBitmap(circles[1], (borderX+xINcm*xCircles + xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
        myCanvas.drawBitmap(buttons[0], (borderX+xINcm*xCircles + xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)

        myCanvas.drawBitmap(circles[1], (borderX+xINcm*xCircles+ 2*xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)
        myCanvas.drawBitmap(buttons[2], (borderX+xINcm*xCircles+ 2*xINcm).toFloat(), (borderY+yINcm*2).toFloat(), paint)

    }

    private fun partialDraw(i: Point){

        if(matrix[i]?.x == delta[0]) {

            matrix[i]!!.y = matrix[i]!!.y -1

            val j = matrix[i]!!

            quantity[j.x-1]--

            myCanvas.drawBitmap(circles[j.y], (borderX+ xINcm*i.x).toFloat(), (borderY+ yINcm*i.y).toFloat(), paint)
            myCanvas.drawBitmap(numbers[j.x], (borderX+ xINcm*i.x).toFloat(), (borderY+ yINcm*i.y).toFloat(), paint)

            if (j.y == 0) {
                matrix.remove(i)
                if(quantity[j.x-1] == 0) delta[0]++
            }

        }

    }

    private fun fullDraw(){

        if(newScore < 99) newScore++

        var placedCircles = 0

        var currentNumber = 1

        var fullPlaces = 0


        scoreDraw()

        //DYNAMIC ALLOCATION OF VALUES INSIDE THE MATRIX
        delta[0] = 1
        delta[1] = 0

        while(placedCircles <= newScore){

            var innerX = (0 until xCircles).random()
            var innerY = (0 until yCircles).random()

            while(matrix[P(innerX,innerY)] != null){

                fullPlaces++
                if (fullPlaces == 5) break

                innerX = (0 until xCircles).random()
                innerY = (0 until yCircles).random()

            }

            fullPlaces = 0

            var rep = (0..3).random()
            if (rep == 0 ){
                if(quantity[currentNumber-1] == 0){
                    rep = (1..3).random()
                } else {
                    if (currentNumber <9) {
                        currentNumber ++
                    } else {
                        currentNumber = 1
                    }
                    continue
                }

            }
            matrix[P(innerX,innerY)] = P(currentNumber, rep)
            quantity[currentNumber-1] += rep
            placedCircles += rep
            if(delta[1] < 9) delta[1]++
            if (currentNumber <9) {
                currentNumber ++
            } else {
                currentNumber = 1
            }
        }
        //DYNAMIC ALLOCATION OF VALUES INSIDE THE MATRIX

        clearScreen()

        for ((i, j) in matrix){

            myCanvas.drawBitmap(circles[j.y], (borderX+ xINcm*i.x).toFloat(), (borderY+ yINcm*i.y).toFloat(), paint)
            myCanvas.drawBitmap(numbers[j.x], (borderX+ xINcm*i.x).toFloat(), (borderY+ yINcm*i.y).toFloat(), paint)

        }
    }

    private fun dataReset(){
        delta[0] = 2
        delta [1] = 1

        for(i in 0..8){
          quantity[i] = 0
        }

        matrix.clear()

        time = defaultTime
        secondTime = defaultSecondTime

        newScore = 0
    }

    private fun scoreDraw(drawBoth: Boolean = false){

        val startX: Float = (borderX-indicators[0].width-xINcm).toFloat()

        var triggerBoth: Boolean = drawBoth

        if(newScore > bestScore){
            bestScore = newScore
            saveScore()
            triggerBoth=true
        }

        if(triggerBoth){

            myCanvas.drawBitmap(indicators[0], startX, borderY.toFloat(), paint)

            myCanvas.drawBitmap(numbers[bestScore/100], startX-xINcm/25, borderY.toFloat(), paint)
            myCanvas.drawBitmap(numbers[bestScore%100/10], startX+xINcm/2, borderY.toFloat(), paint)
            myCanvas.drawBitmap(numbers[bestScore%10], startX+xINcm+xINcm/24, borderY.toFloat(), paint)

        }

        myCanvas.drawBitmap(indicators[0], startX, (borderY+yINcm).toFloat(), paint)

        myCanvas.drawBitmap(numbers[newScore/100], startX-xINcm/25, (borderY+yINcm).toFloat(), paint)
        myCanvas.drawBitmap(numbers[newScore%100/10], startX+xINcm/2, (borderY+yINcm).toFloat(), paint)
        myCanvas.drawBitmap(numbers[newScore%10], startX+xINcm+xINcm/24, (borderY+yINcm).toFloat(), paint)
    }

    private fun timeDraw(){
        timeCanvas.drawBitmap(indicators[1], 0.toFloat(), 0.toFloat(), paint)

        timeCanvas.drawBitmap(numbers[time/100], -(xINcm/25).toFloat(), 0.toFloat(), paint)
        timeCanvas.drawBitmap(numbers[secondTime/100], -(xINcm/25).toFloat(), yINcm.toFloat(), paint)

        timeCanvas.drawBitmap(numbers[(time%100 - time%10)/10], xINcm/2.toFloat(), 0.toFloat(), paint)
        timeCanvas.drawBitmap(numbers[(secondTime%100 - secondTime%10)/10], xINcm/2.toFloat(), yINcm.toFloat(), paint)

        timeCanvas.drawBitmap(numbers[time%10], (xINcm+xINcm/24).toFloat(), 0.toFloat(), paint)
        timeCanvas.drawBitmap(numbers[secondTime%10], (xINcm+xINcm/24).toFloat(), yINcm.toFloat(), paint)

    }



    private fun createTask(): TimerTask {
        return object: TimerTask(){
            override fun run() {

                if(gameOver){

                    if(!readyToEnd){

                        time-=1

                        if(secondTime != 0) secondTime-=1

                        pause()

                        clearScreen()

                        timeDraw()
                        myCanvas.drawBitmap(timeBitmap, (borderX+xINcm*xCircles+xINcm).toFloat(), borderY.toFloat(), paint)

                        myCanvas.drawBitmap(gOver, (x/2 - gOver.width/2).toFloat(), (y/2 - gOver.height/2).toFloat(), paint)

                        dataReset()

                        refresh()

                        readyToEnd  = true

                    }

                    return
                }

                if (isRunning){

                    time-=1

                    if(secondTime != 0) secondTime-=1

                    if (time == 1) gameOver = true

                    timeDraw()

                    if(permission){
                        permission = false

                        myCanvas.drawBitmap(timeBitmap, (borderX+xINcm*xCircles + xINcm).toFloat(), borderY.toFloat(), paint)

                        refresh() //REFRESH NEEDED!

                        permission = true
                    }

                }

            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == null || event.action != MotionEvent.ACTION_DOWN && !postTimer) return true
        postTimer = false

        val myPoint = toMatrix(event.x.toInt(), event.y.toInt())

        if(gameOver){
            if(readyToEnd){
                if (myPoint.x == xCircles + 1 && myPoint.y == 2){

                    preGame = true
                    gameOver = false
                    readyToEnd = false
                    firstDraw()
                    refresh()

                }
            }
            return true
        }

        if(!isRunning){

            if(preGame){

                if(event.x >x/2 - play.width/2 && event.x <x/2 + play.width/2 && event.y >y/2 - play.height/2 && event.y <y/2 + play.height/2){
                    newScore = -1
                    fullDraw()
                    unPause()
                    preGame = false
                }

                if (myPoint.x == xCircles+1 && myPoint.y == 2){

                    newScore = -1
                    fullDraw()
                    unPause()
                    preGame = false

                }

            } else {

                if (myPoint.x == xCircles+1 && myPoint.y == 2){

                    unPause()

                }

                if (myPoint.x == xCircles+2 && myPoint.y == 2){
                    gameOver = true
                    time+=1
                    secondTime+=1
                }

            }

            return true
        }

        if (permission){
            permission = false

            if (myPoint.x == xCircles+1 && myPoint.y == 2) {
                pause()
            }

            if (myPoint.x == xCircles+2 && myPoint.y == 2) gameOver = true

            if(matrix[myPoint] != null) partialDraw(myPoint)

            if(delta[0] > delta[1]) {
                fullDraw()
                time += secondTime
                secondTime = defaultSecondTime
                if(time > 999) time = 999
            }

            refresh() //REFRESH NEEDED!

            permission = true
        } else {
            postTimer = true
        }

        return true

    }

}

typealias P = Point