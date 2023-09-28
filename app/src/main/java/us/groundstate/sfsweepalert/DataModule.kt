package us.groundstate.sfsweepalert

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.groundstate.sfsweepalert.background.ParkingRepository
import us.groundstate.sfsweepalert.background.ParkingRepositoryImpl
import us.groundstate.sfsweepalert.maps.SFGeoClient
import us.groundstate.sfsweepalert.maps.SFGeoClientImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Singleton
    @Binds
    abstract fun getParkingRepository(
        parkingRepository: ParkingRepositoryImpl
    ): ParkingRepository

    @Singleton
    @Binds
    abstract fun getGeoClient(
        geoClient: SFGeoClientImpl
    ): SFGeoClient

}