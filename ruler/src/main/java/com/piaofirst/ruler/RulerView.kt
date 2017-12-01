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
    private lateinit var mVelocityTracker: VelocityTracker

    private var mMinFlingVelocity = 50 // 一个fling最小的速度，单位是每秒多少像素
    private var mTextColor = Color.BLACK // 文字的颜色
    private var mTextSize = 30f //  //尺子刻度下方数字 大小

    private var mLineColor = Color.GRAY // 刻度的颜色
    private var mLineIndicateColor = Color.BLUE // 刻度的颜色
    private var mLineSpaceWidth = 25f //尺子刻度两条线之间的距离
    private var mLineWidth = 2f //尺子刻度两条线之间的距离
    private var mLineMaxHeight = 100f //尺子刻度分为3中不同的高度。 mLineMaxHeight表示最长的那根(也就是 10的倍数时的高度)
    private var mLineMidHeight = 60f //尺子刻度分为3中不同的高度。mLineMidHeight  表示中间的高度(也就是 5  15 25 等时的高度)
    private var mLineMinHeight = 40f //尺子刻度分为3中不同的高度。 mLineMinHeight 表示最短的那根(也就是 1 2 3 4 等时的高度)
    private var mTextMarginTop = 10f // 数字距离刻度的高度

    private var mAlphaEnable = false // 尺子两边是否需要做透明处理

    private var mMinValue = 100f // 最小的数值
    private var mMaxValue = 200f // 最大的数值
    private var mSelectorValue = 50f // 未选择时 默认的值 滑动后表示当前中间指针正在指着的值
    private var mPerValue = 0.1f // 最小单位  如 1:表示 每2条刻度差为1.   0.1:表示 每2条刻度差为0.1

    private var mTextHeight = 40f // 文字的高度
    private var mTextPaint: Paint // 尺子刻度下方数字(每隔10个出现的数值)
    private var mLinePaint: Paint // 尺子刻度
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

        mTextColor = typedArray.getColor(R.styleable.RulerView_textColor, Color.BLACK)
        mTextSize = typedArray.getDimension(R.styleable.RulerView_textSize, 30f)

        mLineColor = typedArray.getColor(R.styleable.RulerView_lineColor, Color.GRAY)
        mLineIndicateColor = typedArray.getColor(R.styleable.RulerView_lineIndicateColor, Color.BLUE)
        mLineSpaceWidth = typedArray.getDimension(R.styleable.RulerView_lineSpaceWidth, 25f)
        mLineWidth = typedArray.getDimension(R.styleable.RulerView_lineWidth, 2f)
        mLineMaxHeight = typedArray.getDimension(R.styleable.RulerView_lineMaxHeight, 100f)
        mLineMidHeight = typedArray.getDimension(R.styleable.RulerView_lineMidHeight, 60f)
        mLineMinHeight = typedArray.getDimension(R.styleable.RulerView_lineMinHeight, 40f)
        mTextMarginTop = typedArray.getDimension(R.styleable.RulerView_textMarginTop, 10f)

        mAlphaEnable = typedArray.getBoolean(R.styleable.RulerView_alphaEnable, false)
        mMinValue = typedArray.getDimension(R.styleable.RulerView_minValue, 0f)
        mMaxValue = typedArray.getDimension(R.styleable.RulerView_minValue, 100f)
        mSelectorValue = typedArray.getDimension(R.styleable.RulerView_minValue, 0f)
        mPerValue = typedArray.getDimension(R.styleable.RulerView_minValue, 0.1f)

        mMinFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.textSize = mTextSize
        mTextPaint.color = mTextColor
        mTextHeight = getFontHeight(mTextPaint)

        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.strokeWidth = mLineWidth
        mLinePaint.color = mLineColor

        mVelocityTracker = VelocityTracker.obtain()

        setValue(2015f, 1990f, 2020f, 0.1f)
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
        this.mSelectorValue = selectorValue
        this.mMinValue = minValue
        this.mMaxValue = maxValue
        this.mPerValue = per * 10.0f
        this.mTotalLine = ((mMaxValue - mMinValue) * 10 / mPerValue + 1).toInt()

        mMaxOffset = (-(mTotalLine - 1) * mLineSpaceWidth).toInt()
        mOffset = (mMinValue - mSelectorValue) / mPerValue * mLineSpaceWidth * 10
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
//        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        mLinePaint.strokeWidth = mLineWidth
//        mLinePaint.color = mLineIndicateColor
//        canvas?.drawLine((mWidth / 2).toFloat(), mHeight, (mWidth / 2).toFloat(), if (isTop) mHeight - indicateHeight else indicateHeight, lPaint)
    }

    /**
     * 绘制刻度线
     */
    private fun drawLine(canvas: Canvas?) {
        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.strokeWidth = mLineWidth
        mLinePaint.color = mLineColor
        var left: Float
        var height: Float
        var value: String
        var alpha = 0
        var scale: Float
        var srcPointX = mWidth / 2
        for (i in 0 until mTotalLine) {
            left = srcPointX + mOffset + i * mLineSpaceWidth
            //先画默认值在正中间，左右各一半的view。多余部分暂时不画(也就是从默认值在中间，画旁边左右的刻度线)
            if (left < 0 || left > mWidth) {
                continue
            }
            if (i % 10 == 0) {
                height = mLineMaxHeight
            } else if (i % 5 == 0) {
                height = mLineMidHeight
            } else {
                height = mLineMinHeight
            }
            if (mAlphaEnable) {
                scale = 1 - Math.abs(left - srcPointX) / srcPointX
                alpha = (255 * scale * scale).toInt()
                mLinePaint.alpha = alpha
            }
            canvas?.drawLine(left, 0f, left, height, mLinePaint)
            // 数值为10的倍数是 画数值
            if (i % 10 == 0) {
                value = (mMinValue + i * mPerValue / 10).toInt().toString()
                if (mAlphaEnable) {
                    mTextPaint.alpha = alpha
                }
                canvas?.drawText(value, left - mTextPaint.measureText(value) / 2,
                        height + mTextMarginTop + mTextHeight, mTextPaint)
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
        mSelectorValue = mMinValue + Math.round(mOffset) * 1.0f / mLineSpaceWidth * mPerValue / 10.0f
        notifyValueChange()
        postInvalidate()
    }

    private fun notifyValueChange() {
        if (mListener != null) {
            mListener?.onValueChanged(mSelectorValue)
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
        mSelectorValue = mMinValue + Math.round(Math.abs(mOffset) * 1.0f / mLineSpaceWidth) * mPerValue / 10.0f
        mOffset = (mMinValue - mSelectorValue) * 10.0f / mPerValue * mLineSpaceWidth
        notifyValueChange()
        postInvalidate()
    }

    private fun computeVelocityTracker() {
        mVelocityTracker.computeCurrentVelocity(1000) //代表 1秒内运动了多少像素
        val xVelocity = mVelocityTracker.xVelocity
        if (Math.abs(xVelocity) > mMinFlingVelocity) {
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
