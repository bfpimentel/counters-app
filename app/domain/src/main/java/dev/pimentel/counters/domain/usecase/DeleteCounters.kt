package dev.pimentel.counters.domain.usecase

import dev.pimentel.counters.domain.repository.CountersRepository

class DeleteCounters(private val repository: CountersRepository) : SuspendedUseCase<DeleteCounters.Params, Unit> {

    override suspend fun invoke(params: Params) = repository.deleteCounters(params.countersToBeDeletedIds)

    data class Params(val countersToBeDeletedIds: List<String>)
}
