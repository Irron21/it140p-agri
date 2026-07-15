package com.example.agriflow.ui.main

import com.example.agriflow.data.local.HistoryRepository
import com.example.agriflow.viewmodel.AgriFlowViewModel
import com.example.agriflow.viewmodel.SoapUiState
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class MainScreenViewModelTest {

    private val mockHistoryRepository: HistoryRepository = mock {
        on { yieldHistory } doReturn flowOf(emptyList())
        on { freightHistory } doReturn flowOf(emptyList())
        on { hubClusterHistory } doReturn flowOf(emptyList())
        on { carbonHistory } doReturn flowOf(emptyList())
    }
    
    @Test
    fun testInitialStatesAreIdle() = runTest {
        val viewModel = AgriFlowViewModel(historyRepository = mockHistoryRepository)
        assertEquals(SoapUiState.Idle, viewModel.yieldState.value)
        assertEquals(SoapUiState.Idle, viewModel.freightState.value)
        assertEquals(SoapUiState.Idle, viewModel.hubState.value)
        assertEquals(SoapUiState.Idle, viewModel.carbonState.value)
    }

    @Test
    fun testDefaultEndpointUrl() = runTest {
        val viewModel = AgriFlowViewModel(historyRepository = mockHistoryRepository)
        assertEquals("http://10.0.2.2:8000/server.php", viewModel.endpointUrl.value)
    }

    @Test
    fun testUpdateEndpointUrl() = runTest {
        val viewModel = AgriFlowViewModel(historyRepository = mockHistoryRepository)
        viewModel.updateEndpointUrl("http://192.168.1.50:8000/server.php")
        assertEquals("http://192.168.1.50:8000/server.php", viewModel.endpointUrl.value)
    }
}
