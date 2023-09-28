package us.groundstate.sfsweepalert

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import us.groundstate.sfsweepalert.background.ParkingRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val appContext: Context,
    val parkingRepository: ParkingRepository
): ViewModel() {

}