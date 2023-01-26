package com.siddhantkushwaha.thugtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.flow

class MainActivity : AppCompatActivity() {

    private val flow = flow<Long> { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }
}