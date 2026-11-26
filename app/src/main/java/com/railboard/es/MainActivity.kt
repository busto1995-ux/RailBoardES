package com.railboard.es

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.railboard.es.di.AppContainer
import com.railboard.es.ui.board.BoardScreen
import com.railboard.es.ui.board.BoardViewModel
import com.railboard.es.ui.theme.RailBoardTheme

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RailBoardTheme {
                val vm: BoardViewModel = viewModel(
                    factory = BoardViewModel.Factory(
                        repository = appContainer.repository,
                        locator = appContainer.stationLocator
                    )
                )
                BoardScreen(viewModel = vm)
            }
        }
    }
}
