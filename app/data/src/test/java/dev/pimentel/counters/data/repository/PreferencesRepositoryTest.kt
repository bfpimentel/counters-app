package dev.pimentel.counters.data.repository

import dev.pimentel.counters.data.sources.local.PreferencesLocalDataSource
import dev.pimentel.counters.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PreferencesRepositoryTest {

    private val preferencesLocalDataSource = mockk<PreferencesLocalDataSource>()
    private val repository: PreferencesRepository = PreferencesRepositoryImpl(preferencesLocalDataSource)

    @Test
    fun `should return data source answer`() = runBlockingTest {
        val isFirstAccess = true

        coEvery { preferencesLocalDataSource.hasFetchedCounters() } returns isFirstAccess

        assertEquals(repository.hasFetchedCounters(), isFirstAccess)

        coVerify(exactly = 1) { preferencesLocalDataSource.hasFetchedCounters() }
        confirmVerified(preferencesLocalDataSource)
    }
}
