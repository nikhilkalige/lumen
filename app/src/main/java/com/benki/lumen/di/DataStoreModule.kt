package com.benki.lumen.di

import android.content.Context
import com.benki.lumen.datastore.FuelEntriesDataStore
import com.benki.lumen.datastore.SettingsDataStore
import com.benki.lumen.network.SelectedSpreadsheet
import com.benki.lumen.repository.SheetIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Singleton
    @Provides
    fun provideSettingsDataStore(@ApplicationContext appContext: Context): SettingsDataStore {
        return SettingsDataStore(appContext)
    }

    @Singleton
    @Provides
    fun provideFuelEntriesDataStore(@ApplicationContext appContext: Context): FuelEntriesDataStore {
        return FuelEntriesDataStore(appContext)
    }

    @Singleton
    @Provides
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton // This provider can be a singleton if SettingsDataStore is a singleton
    @JvmSuppressWildcards
    fun provideSheetIdProvider(settingsDataStore: SettingsDataStore): SheetIdProvider =
        object : SheetIdProvider {
            override suspend fun invoke(): SelectedSpreadsheet? = settingsDataStore.sheetInfoFlow.firstOrNull()
        }
}
