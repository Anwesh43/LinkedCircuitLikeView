package com.anwesh.uiprojects.circuitlikeview

/**
 * Created by anweshmishra on 24/07/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#4527A0")
val backColor : Int = Color.parseColor("#BDBDBD")
val sizeFactor : Float = 2.9f
val rotDeg : Float = 90f
val cFactor : Float = 5.6f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawCircuitLine(i : Int, scale : Float, size : Float, paint : Paint) {
    val sc : Float = scale.divideScale(i, lines)
    val cSize : Float =  (2* size) / 3
    val kSize : Float = size / cFactor
    save()
    rotate(rotDeg * i)
    translate(-size, -size)
    drawLine(0f, 0f, cSize, 0f, paint)
    for (j in 0..1) {
        val y : Float = -kSize * sc.divideScale(j, 2) * (1f - 2 * j)
        save()
        drawLine(cSize, 0f, cSize, y, paint)
        drawLine(cSize, y, 2 * cSize,y, paint)
        drawLine(2 * cSize, y, 2 * cSize, 0f, paint)
        restore()
    }
    drawLine(2 * cSize, 0f, 3 * cSize, 0f, paint)
    restore()
}

fun Canvas.drawCLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size :Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    save()
    translate(gap * (i + 1), h / 2)
    rotate(rotDeg * sc2)
    for (j in 0..(lines - 1)) {
        drawCircuitLine(j, sc1, size, paint)
    }
    restore()
}

class CircuitLikeView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CLNode(var i : Int, val state : State = State()) {

        private var next : CLNode? = null
        private var prev : CLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = CLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : CLNode {
            var curr : CLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class CircuitLike(var i : Int) {

        private val root : CLNode = CLNode(0)
        private var curr : CLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CircuitLikeView) {

        private val animator : Animator = Animator(view)
        private val cl : CircuitLike = CircuitLike(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            cl.draw(canvas, paint)
            animator.animate {
                cl.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            cl.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : CircuitLikeView {
            val view : CircuitLikeView = CircuitLikeView(activity)
            activity.setContentView(view)
            return view
        }
    }
}