package com.piaofirst.ruler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller

/**
 * Created by gjc on 2017/11/28.
 */
class RulerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : View(context, attrs, defStyle) {

    private var mScroller = Scroller(context)
    private var mVelocityTracker: VelocityTracker
    var isTextOnTop = false
        set(value) {
            field = value
            invalidate()
        }

    var minFlingVelocity = 50 // 一个fling最小的速度，单位是每秒多少像素
        set(value) {
            field = value
            invalidate()
        }
    var textColor = Color.BLACK // 文字的颜色
        set(value) {
            field = value
            mTextPaint.color = value
            invalidate()
        }
    var textSize = 30f //  //尺子刻度下方数字 大小
        set(value) {
            field = value
            mTextPaint.textSize = value
            invalidate()
        }

    var lineColor = Color.GRAY // 刻度的颜色
        set(value) {
            field = value
            invalidate()
        }
    var mineIndicateColor = Color.BLUE // 指示线的颜色
        set(value) {
            field = value
            invalidate()
        }
    var lineIndicateHeight = 100f     //指示线的高度
        set(value) {
            field = value
            invalidate()
        }

    var lineSpaceWidth = 25f //尺子刻度两条线之间的距离
        set(value) {
            field = value
            invalidate()
        }
    var lineWidth = 2f //尺子刻度两条线之间的距离
        set(value) {
            field = value
            invalidate()
        }
    var lineMaxHeight = 100f //尺子刻度分为3中不同的高度。 mLineMaxHeight表示最长的那根(也就是 10的倍数时的高度)
        set(value) {
            field = value
            invalidate()
        }

    var lineMidHeight = 60f //尺子刻度分为3中不同的高度。lineMidHeight  表示中间的高度(也就是 5  15 25 等时的高度)
        set(value) {
            field = value
            invalidate()
        }
    var lineMinHeight = 40f //尺子刻度分为3中不同的高度。 lineMinHeight 表示最短的那根(也就是 1 2 3 4 等时的高度)
        set(value) {
            field = value
            invalidate()
        }
    var textMarginTop = 10f // 数字距离刻度的高度
        set(value) {
            field = value
            invalidate()
        }

    var alphaEnable = false // 尺子两边是否需要做透明处理
        set(value) {
            field = value
            invalidate()
        }

    private var minValue = 0f // 最小的数值
    private var maxValue = 100f // 最大的数值
    private var selectorValue = 50f // 未选择时 默认的值 滑动后表示当前中间指针正在指着的值
    private var perValue = 0.1f // 最小单位  如 1:表示 每2条刻度差为1.   0.1:表示 每2条刻度差为0.1

    private var mTextHeight = 40f // 文字的高度
    private var mTextPaint = Paint() // 尺子刻度下方数字(每隔10个出现的数值)
    private var mLinePaint = Paint() // 尺子刻度
    private var mWidth = 0
    private var mHeight = 0
    private var mTotalLine = 0 //共有多少条刻度
    private var mMaxOffset = 0 //所有刻度 共有多长
    private var mOffset = 0f //默认状态下，mSelectorValue所在的位置  位于尺子总刻度的位置
    private var mLastX: Int = 0
    private var mMove: Int = 0
    private var mListener: OnValueChangedListener? = null  // 滑动后数值回调

    init {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.RulerView)

        textColor = typedArray.getColor(R.styleable.RulerView_textColor, textColor)
        textSize = typedArray.getDimension(R.styleable.RulerView_textSize, textSize)

        lineColor = typedArray.getColor(R.styleable.RulerView_lineColor, lineColor)
        mineIndicateColor = typedArray.getColor(R.styleable.RulerView_lineIndicateColor, mineIndicateColor)
        lineIndicateHeight = typedArray.getDimension(R.styleable.RulerView_lineIndicateHeight, lineIndicateHeight)
        lineSpaceWidth = typedArray.getDimension(R.styleable.RulerView_lineSpaceWidth, lineSpaceWidth)
        lineWidth = typedArray.getDimension(R.styleable.RulerView_lineWidth, lineWidth)
        lineMaxHeight = typedArray.getDimension(R.styleable.RulerView_lineMaxHeight, lineMaxHeight)
        lineMidHeight = typedArray.getDimension(R.styleable.RulerView_lineMidHeight, lineMidHeight)
        lineMinHeight = typedArray.getDimension(R.styleable.RulerView_lineMinHeight, lineMinHeight)
        textMarginTop = typedArray.getDimension(R.styleable.RulerView_textMarginTop, textMarginTop)

        alphaEnable = typedArray.getBoolean(R.styleable.RulerView_alphaEnable, false)
        minValue = typedArray.getFloat(R.styleable.RulerView_minValue, minValue)
        maxValue = typedArray.getFloat(R.styleable.RulerView_maxValue, maxValue)
        selectorValue = typedArray.getFloat(R.styleable.RulerView_selectorValue, selectorValue)
        perValue = typedArray.getFloat(R.styleable.RulerView_perValue, perValue)

        minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

        mTextPaint.isAntiAlias = true
        mTextPaint.textSize = textSize
        mTextPaint.color = textColor
        mTextHeight = getFontHeight(mTextPaint)

        mLinePaint.isAntiAlias = true
        mLinePaint.strokeWidth = lineWidth
        mLinePaint.color = lineColor

        mVelocityTracker = VelocityTracker.obtain()

        mTotalLine = ((maxValue - minValue) / perValue + 1).toInt()
        mMaxOffset = (-(mTotalLine - 1) * lineSpaceWidth).toInt()
        mOffset = (minValue - selectorValue) / perValue * lineSpaceWidth
        visibility = VISIBLE
    }

    /**
     * 计算文字的高度
     */
    private fun getFontHeight(paint: Paint): Float {
        var fm = paint.fontMetrics
        return fm.descent - fm.ascent
    }

    /**
     * @param selectorValue 未选择时 默认的值 滑动后表示当前中间指针正在指着的值
     * @param minValue 最小数值
     * @param maxValue 最大数值
     * @param per 最小单位  如 1:表示 每2条刻度差为1. 0.1:表示 每2条刻度差为0.1
     */
    fun setValue(selectorValue: Float, minValue: Float, maxValue: Float, per: Float) {
        this.selectorValue = selectorValue
        this.minValue = minValue
        this.maxValue = maxValue
//        this.perValue = per * 10.0f
        this.perValue = per
//        this.mTotalLine = ((this.maxValue - this.minValue) * 10 / perValue + 1).toInt()
        this.mTotalLine = ((this.maxValue - this.minValue) / perValue + 1).toInt()

        mMaxOffset = (-(mTotalLine - 1) * lineSpaceWidth).toInt()
//        mOffset = (this.minValue - this.selectorValue) / perValue * lineSpaceWidth * 10
        mOffset = (this.minValue - this.selectorValue) / perValue * lineSpaceWidth
        invalidate()
        visibility = VISIBLE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mWidth = w
            mHeight = h
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawLine(canvas)
        drawLineIndicate(canvas)
    }

    /**
     * 绘制指示线
     */
    private fun drawLineIndicate(canvas: Canvas?) {
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.strokeWidth = lineWidth
        mLinePaint.color = mineIndicateColor
//        canvas?.drawLine((mWidth / 2).toFloat(), mHeight.toFloat(), (mWidth / 2).toFloat(), lineIndicateHeight, mLinePaint)
        if (isTextOnTop) {
            canvas?.drawLine((mWidth / 2).toFloat(), mHeight.toFloat(),
                    (mWidth / 2).toFloat(), mHeight - lineIndicateHeight, mLinePaint)
        } else {
            canvas?.drawLine((mWidth / 2).toFloat(), 0.0f,
                    (mWidth / 2).toFloat(), lineIndicateHeight, mLinePaint)
        }
    }

    /**
     * 绘制刻度线
     */
    private fun drawLine(canvas: Canvas?) {
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.strokeWidth = lineWidth
        mLinePaint.color = lineColor
        var left: Float
        var height: Float
        var value: String
        var alpha = 0
        var scale: Float
        var srcPointX = mWidth / 2
        if (isTextOnTop) {
            canvas?.drawLine(0f, mHeight.toFloat(), mWidth.toFloat(), mHeight.toFloat(), mLinePaint)
        } else {
            canvas?.drawLine(0f, 0f, mWidth.toFloat(), 0f, mLinePaint)
        }
        for (i in 0 until mTotalLine) {
            left = srcPointX + mOffset + i * lineSpaceWidth
            //先画默认值在正中间，左右各一半的view。多余部分暂时不画(也就是从默认值在中间，画旁边左右的刻度线)
            if (left < 0 || left > mWidth) {
                continue
            }
            if (i % 10 == 0) {
                height = lineMaxHeight
            } else if (i % 5 == 0) {
                height = lineMidHeight
            } else {
                height = lineMinHeight
            }
            if (alphaEnable) {
                scale = 1 - Math.abs(left - srcPointX) / srcPointX
                alpha = (255 * scale * scale).toInt()
                mLinePaint.alpha = alpha
            }
            if (isTextOnTop) {
                canvas?.drawLine(left, mHeight.toFloat(), left, mHeight - height, mLinePaint)
            } else {
                canvas?.drawLine(left, 0f, left, height, mLinePaint)
            }
            // 数值为10的倍数是 画数值
            if (i % 10 == 0) {
//                value = (minValue + i * perValue / 10).toInt().toString()
                value = (minValue + i * perValue).toInt().toString()
                if (alphaEnable) {
                    mTextPaint.alpha = alpha
                }
                if (isTextOnTop) {
                    canvas?.drawText(value, left - mTextPaint.measureText(value) / 2,
                            mHeight - height - textMarginTop, mTextPaint)
                } else {
                    canvas?.drawText(value, left - mTextPaint.measureText(value) / 2,
                            height + textMarginTop + mTextHeight, mTextPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        var action = event?.action
        var x = event?.getX()?.toInt()
        mVelocityTracker.addMovement(event)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScroller.forceFinished(true)
                mLastX = x!!
                mMove = 0
            }
            MotionEvent.ACTION_MOVE -> {
                mMove = mLastX - x!!
                changeMoveAndValue()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                computeMoveEnd()
                computeVelocityTracker()
                return false
            }
        }
        mLastX = x!!
        return true
    }

    /**
     * 滑动后的操作
     */
    private fun changeMoveAndValue() {
        mOffset -= mMove
        if (mOffset <= mMaxOffset) { // 偏移超出最大刻度
            mOffset = mMaxOffset.toFloat()
            mMove = 0
            mScroller.forceFinished(true)
        } else if (mOffset > 0) {// 偏移超出最小刻度
            mOffset = 0f
            mMove = 0
            mScroller.forceFinished(true)
        }
//        selectorValue = minValue + Math.round(Math.abs(mOffset)) * 1.0f / lineSpaceWidth * perValue / 10.0f
        selectorValue = minValue + Math.round(Math.abs(mOffset)) * 1.0f / lineSpaceWidth * perValue
        notifyValueChange()
        postInvalidate()
    }

    private fun notifyValueChange() {
        if (mListener != null) {
            mListener?.onValueChanged(selectorValue)
        }
    }

    /**
     * 滑动结束后，如果指针在两条刻度之间时，改变mOffset 让指针正好在刻度上。
     */
    private fun computeMoveEnd() {
        mOffset -= mMove
        if (mOffset <= mMaxOffset) { // 偏移超出最大刻度
            mOffset = mMaxOffset.toFloat()
        } else if (mOffset > 0) {// 偏移超出最小刻度
            mOffset = 0f
        }
        mLastX = 0
        mMove = 0
//        selectorValue = minValue + Math.round(Math.abs(mOffset) * 1.0f / lineSpaceWidth) * perValue / 10.0f
        selectorValue = minValue + Math.round(Math.abs(mOffset) * 1.0f / lineSpaceWidth) * perValue
//        mOffset = (minValue - selectorValue) * 10.0f / perValue * lineSpaceWidth
        mOffset = (minValue - selectorValue) / perValue * lineSpaceWidth
        notifyValueChange()
        postInvalidate()
    }

    private fun computeVelocityTracker() {
        mVelocityTracker.computeCurrentVelocity(1000) //代表 1秒内运动了多少像素
        val xVelocity = mVelocityTracker.xVelocity
        if (Math.abs(xVelocity) > minFlingVelocity) {
            mScroller.fling(0, 0, xVelocity.toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0)
        }
    }

    fun setOnValueChangedListener(listener: OnValueChangedListener) {
        mListener = listener
    }

    /**
     *  滑动后的回调
     */
    interface OnValueChangedListener {
        fun onValueChanged(value: Float)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.computeScrollOffset()) { // 滑动还没有结束 the animation is not yet finished.
            if (mScroller.currX == mScroller.finalX) {
                computeMoveEnd()
            } else {
                val x = mScroller.currX
                mMove = mLastX - x
                changeMoveAndValue()
                mLastX = x
            }
        }
    }
}
