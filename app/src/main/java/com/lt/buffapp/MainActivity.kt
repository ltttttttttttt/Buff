package com.lt.buffapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lt.buffapp.ui.theme.BuffAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuffAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    View()
                }
            }
        }
    }

}

@Composable
private fun View() {
    Column {
        //普通方式(自行copy)
        Common()
        Spacer(modifier = Modifier.height(30.dp))
        //手动增加不序列化的state
        Manual()
        Spacer(modifier = Modifier.height(30.dp))
        //使用Buff来自动增加不序列化的state
        UseBuff()
    }
}


