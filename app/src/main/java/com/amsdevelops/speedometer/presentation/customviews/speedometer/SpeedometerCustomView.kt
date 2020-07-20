package com.amsdevelops.speedometer.presentation.customviews.speedometer

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.amsdevelops.speedometer.R
import com.amsdevelops.speedometer.constants.SpeedometerConstants
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

class SpeedometerCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs),
    SpeedChangeListener {

    private var maxSpeed = 0f
    private var currentSpeed = 0f
    private lateinit var objectAnimator: ObjectAnimator

    private lateinit var dashPaintThin: Paint
    private lateinit var dashPaintWide: Paint
    private lateinit var digitsPaintBold: Paint
    private lateinit var digitsPaintThin: Paint
    private lateinit var readingPaint: Paint
    private lateinit var paintBackground: Paint
    private lateinit var paintArrow: Paint
    private lateinit var paintArrowHolderFill: Paint
    private lateinit var paintArrowHolderStroke: Paint
    private val rect = Rect()

    //Drawing colors
    private var DASH_COLOR = Color.WHITE
    private var DIGIT_COLOR = Color.WHITE
    private var ARROW_COLOR = Color.RED
    private var BACKGROUND_COLOR = Color.DKGRAY
    private var SCALE_SIZE = 60f
    private var DASHES_STEP = 2
    private var DIGIT_SPEED_ENABLED = true
    private var HORIZONTAL_DIGITS = false

    //Scale configuration
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SpeedometerCustomView, 0, 0)

        try {
            maxSpeed = a.getFloat(
                R.styleable.SpeedometerCustomView_maxSpeed,
                SpeedometerConstants.DEFAULT_MAX_SPEED
            )
            currentSpeed = a.getFloat(R.styleable.SpeedometerCustomView_currentSpeed, 0f)
            DIGIT_COLOR = a.getColor(R.styleable.SpeedometerCustomView_digitColor, DIGIT_COLOR)
            DASH_COLOR = a.getColor(R.styleable.SpeedometerCustomView_dashColor, DASH_COLOR)
            ARROW_COLOR = a.getColor(R.styleable.SpeedometerCustomView_arrowColor, ARROW_COLOR)
            BACKGROUND_COLOR = a.getColor(R.styleable.SpeedometerCustomView_backgroundColor, BACKGROUND_COLOR)
            SCALE_SIZE = a.getDimension(R.styleable.SpeedometerCustomView_scaleTextSize, SCALE_SIZE)
            DIGIT_SPEED_ENABLED =
                a.getBoolean(R.styleable.SpeedometerCustomView_digitSpeedTextEnabled, DIGIT_SPEED_ENABLED)
            HORIZONTAL_DIGITS = a.getBoolean(R.styleable.SpeedometerCustomView_horizontalDigits, HORIZONTAL_DIGITS)
            DASHES_STEP = a.getInt(R.styleable.SpeedometerCustomView_dashesStep, DASHES_STEP)
        } finally {
            a.recycle()
        }

        initDrawingTools()
    }

    private fun initDrawingTools() {
        dashPaintThin = Paint().apply {
            color = DASH_COLOR
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 0.01f
        }
        dashPaintWide = Paint().apply {
            color = DASH_COLOR
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 0.03f
        }
        digitsPaintBold = Paint(dashPaintThin).apply {
            strokeWidth = 2f
            textSize = SCALE_SIZE
            setShadowLayer(5f, 0f, 0f, Color.RED)
            color = DIGIT_COLOR
        }
        digitsPaintThin = Paint(dashPaintThin).apply {
            strokeWidth = 2f
            textSize = SCALE_SIZE * 0.7f
            setShadowLayer(5f, 0f, 0f, Color.RED)
            color = DIGIT_COLOR
        }
        readingPaint = Paint(digitsPaintBold).apply {
            style = Paint.Style.FILL_AND_STROKE
            setShadowLayer(3f, 0f, 0f, Color.WHITE)
            textSize = SCALE_SIZE
            typeface = Typeface.SANS_SERIF
            color = Color.WHITE
        }
        paintBackground = Paint().apply {
            color = BACKGROUND_COLOR
            style = Paint.Style.FILL
        }
        paintArrow = Paint().apply {
            color = ARROW_COLOR
            strokeWidth = 15f
            setShadowLayer(5f, 0f, 0f, ARROW_COLOR)
        }
        paintArrowHolderFill = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.FILL
            setShadowLayer(5f, 0f, 0f, DASH_COLOR)
        }
        paintArrowHolderStroke = Paint().apply {
            strokeWidth = 5f
            color = DASH_COLOR
            style = Paint.Style.STROKE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = if (width > height) {
            height.div(2f)
        } else {
            width.div(2f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val chosenWidth = chooseDimension(widthMode, widthSize)
        val chosenHeight = chooseDimension(heightMode, heightSize)

        val chosenDimension = min(chosenWidth, chosenHeight)
        centerX = chosenDimension.div(2f)
        centerY = chosenDimension.div(2f)

        setMeasuredDimension(chosenDimension, chosenDimension)
    }

    private fun chooseDimension(mode: Int, size: Int) =
        when (mode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> size
            else -> 300
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawGaugeBackground(canvas)
        drawScaleBackground(canvas)
        if (HORIZONTAL_DIGITS) {
            drawLegendHorizontally(canvas)
        } else {
            drawLegendAroundCircle(canvas)
        }
        drawArrow(canvas)
        if (DIGIT_SPEED_ENABLED) drawReadings(canvas)
    }

    private fun drawGaugeBackground(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, paintBackground)
    }

    private fun drawScaleBackground(canvas: Canvas) {
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(135f)
        canvas.scale(centerX, centerY)

        val scale = 0.93f

        val step = Math.PI * 1.5 / maxSpeed

        for (i in 0 .. maxSpeed.toInt() step DASHES_STEP) {
            val angle = Math.PI * 1.5 - (step * i)
            val x1 = (cos(angle)).toFloat()
            val y1 = (sin(angle)).toFloat()

            val x2: Float
            val y2: Float

            x2 = x1 * scale
            y2 = y1 * scale

            if (i % 10 == 0) {
                canvas.drawLine( x1, y1, x2, y2, dashPaintWide)
            } else {
                canvas.drawLine( x1, y1, x2, y2, dashPaintThin)
            }

        }
        canvas.restore()
    }

    private fun drawLegendHorizontally(canvas: Canvas) {
        canvas.save()

        val countOfDashes = maxSpeed / 10
        val space = maxSpeed / 20

        for (i in 0 .. maxSpeed.toInt() step 10) {
            val tmp = i.toString()

            digitsPaintBold.getTextBounds(tmp, 0, tmp.length, rect)

            val angle = Math.PI * 1.5 / countOfDashes * (i / 10 + space)

            val x = (centerX + cos(angle) * radius * 0.8 - rect.width() / 2).toFloat()
            val y = (centerY + sin(angle) * radius * 0.8 + rect.height() / 2).toFloat()

            canvas.drawText(tmp, x, y, digitThickness(i))
        }

        canvas.restore()
    }

    private fun drawLegendAroundCircle(canvas: Canvas) {
        canvas.save()

        canvas.rotate(135f, centerX, centerY)
        val circle = Path()

        val theeForthCircumference = radius * 0.8 * Math.PI * 1.5
        val increment = 10

        for (i in 0..maxSpeed.toInt() step increment) {
            val digitText = i.toString()
            val digitTextLength = round(digitsPaintBold.measureText(digitText))

            circle.addCircle(centerX, centerY, radius * 0.8f, Path.Direction.CW)
            canvas.drawTextOnPath(
                digitText,
                circle,
                ((i * theeForthCircumference / maxSpeed) - digitTextLength/2).toFloat(),
                0f,
                digitThickness(i)
            )
        }

        canvas.restore()
    }

    private fun digitThickness(digit: Int): Paint =
        if (digit % 20 == 0) {
            digitsPaintBold
        } else {
            digitsPaintThin
        }

    private fun drawArrow(canvas: Canvas) {
        val limit = (currentSpeed / maxSpeed * 270 - 135)

        canvas.save()

        canvas.rotate(limit, centerX, centerY)

        canvas.drawLine(centerX, centerY, radius, radius * 0.15f, paintArrow)

        canvas.drawCircle(centerX, centerY, radius / 7, paintArrowHolderFill)
        canvas.drawCircle(centerX, centerY, radius / 7, paintArrowHolderStroke)

        canvas.restore()
    }

    private fun drawReadings(canvas: Canvas) {
        val path = Path()

        val message = String.format("%d", currentSpeed.toInt())
        val widths = FloatArray(message.length)
        readingPaint.getTextWidths(message, widths)

        var advance = 0f
        for (width in widths) advance += width

        path.moveTo(centerX - advance / 2, centerY)
        path.lineTo(centerX + advance / 2, centerY)
        canvas.drawTextOnPath(message, path, 0f, SCALE_SIZE / 2.5f, readingPaint)

    }

    override fun setSpeedChanged(speed: Float) {
        setSpeedAnimated(speed)
    }

    private fun setSpeedAnimated(speed: Float) {
        objectAnimator = ObjectAnimator.ofFloat(this, "currentSpeed", currentSpeed, speed).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        objectAnimator.start()
    }

    private fun setCurrentSpeed(speed: Float) {
        currentSpeed = checkLimits(speed)
        invalidate()
    }

    private fun checkLimits(speed: Float)= when {
        speed > maxSpeed -> {
            maxSpeed
        }
        speed < 0 -> {
            0f
        }
        else -> {
            speed
        }
    }

    fun getCurrentSpeed() = currentSpeed
}