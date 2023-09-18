package us.groundstate.sfsweepalert

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.groundstate.sfsweepalert.background.LocationRepository
import us.groundstate.sfsweepalert.background.LocationRepositoryImpl
import us.groundstate.sfsweepalert.maps.SFGeoClient
import us.groundstate.sfsweepalert.maps.SFGeoClientImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Singleton
    @Binds
    abstract fun getLocationRepository(
        locationRepository: LocationRepositoryImpl
    ): LocationRepository

    @Singleton
    @Binds
    abstract fun getGeoClient(
        geoClient: SFGeoClientImpl
    ): SFGeoClient

}