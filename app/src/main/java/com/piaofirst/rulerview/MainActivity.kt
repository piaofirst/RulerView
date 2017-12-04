package com.piaofirst.rulerview

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.piaofirst.ruler.RulerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rulerView.setValue(250F, 0f , 500f, 1f)
        rulerView.mineIndicateColor = Color.RED
        rulerView.textColor = Color.BLUE
        rulerView.lineMaxHeight = 80f
        rulerView.setOnValueChangedListener(object : RulerView.OnValueChangedListener {
            override fun onValueChanged(value: Float) {
                text.text = value.toString()
            }
        })
        rulerView2.setOnValueChangedListener(object : RulerView.OnValueChangedListener{
            override fun onValueChanged(value: Float) {
                text2.text = value.toString()
            }

        })
    }
}
