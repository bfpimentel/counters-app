package dev.pimentel.counters.presentation.counters

import androidx.lifecycle.viewModelScope
import dev.pimentel.counters.di.NavigatorRouterQualifier
import dev.pimentel.counters.domain.entity.Counter
import dev.pimentel.counters.domain.usecase.DecreaseCount
import dev.pimentel.counters.domain.usecase.DeleteCounters
import dev.pimentel.counters.domain.usecase.GetCounters
import dev.pimentel.counters.domain.usecase.IncreaseCount
import dev.pimentel.counters.domain.usecase.NoParams
import dev.pimentel.counters.domain.usecase.SearchCounters
import dev.pimentel.counters.presentation.counters.data.CounterViewData
import dev.pimentel.counters.presentation.counters.data.CountersIntention
import dev.pimentel.counters.presentation.counters.data.CountersState
import dev.pimentel.counters.presentation.counters.mappers.CountersDeletionMapper
import dev.pimentel.counters.presentation.counters.mappers.CountersSharingMapper
import dev.pimentel.counters.shared.dispatchers.DispatchersProvider
import dev.pimentel.counters.shared.mvi.StateViewModelImpl
import dev.pimentel.counters.shared.mvi.toEvent
import dev.pimentel.counters.shared.navigator.NavigatorRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountersViewModel @Inject constructor(
    @NavigatorRouterQualifier private val navigator: NavigatorRouter,
    private val getCounters: GetCounters,
    private val searchCounters: SearchCounters,
    private val increaseCount: IncreaseCount,
    private val decreaseCount: DecreaseCount,
    private val deleteCounters: DeleteCounters,
    private val deletionMapper: CountersDeletionMapper,
    private val sharingMapper: CountersSharingMapper,
    dispatchersProvider: DispatchersProvider,
    @CountersStateQualifier initialState: CountersState
) : StateViewModelImpl<CountersState, CountersIntention>(
    dispatchersProvider = dispatchersProvider,
    initialState = initialState
), CountersContract.ViewModel {

    private lateinit var counters: List<Counter>

    private var isSearching: Boolean = false
    private var isEditing: Boolean = false
    private var selectedCountersIds: List<String> = emptyList()

    init {
        viewModelScope.launch(dispatchersProvider.io) { getCounters() }
    }

    override suspend fun handleIntentions(intention: CountersIntention) {
        when (intention) {
            is CountersIntention.SearchCounters -> searchCounters(intention.query)
            is CountersIntention.Increase -> increaseCount(IncreaseCount.Params(intention.counterId))
            is CountersIntention.Decrease -> decreaseCount(DecreaseCount.Params(intention.counterId))
            is CountersIntention.StartEditing -> startEditing(intention.counterId)
            is CountersIntention.SelectOrDeselectCounter -> selectOrDeselectCounter(intention.counterId)
            is CountersIntention.TryDeleting -> tryDeleting()
            is CountersIntention.DeleteSelectedCounters -> deleteSelectedCounters()
            is CountersIntention.ShareSelectedCounters -> shareSelectedCounters()
            is CountersIntention.FinishEditing -> finishEditing()
            is CountersIntention.NavigateToCreateCounter -> navigateToCreateCounter()
        }
    }

    private suspend fun getCounters() {
        getCounters(NoParams).collect { counters ->
            this.counters = counters
            updateScreenState()
        }
    }

    private suspend fun searchCounters(query: String?) {
        this.isSearching = !query.isNullOrEmpty()
        searchCounters(SearchCounters.Params(query))
    }

    private suspend fun startEditing(counterId: String) {
        this.isEditing = true
        selectOrDeselectCounter(counterId)
    }

    private suspend fun selectOrDeselectCounter(counterId: String) {
        this.selectedCountersIds = this.selectedCountersIds.toMutableList().apply {
            if (contains(counterId)) remove(counterId)
            else add(counterId)
        }
        updateScreenState()
    }

    private suspend fun tryDeleting() {
        val itemsToBeDeleted = this.counters.filter { counter -> this.selectedCountersIds.contains(counter.id) }
        updateState { copy(deleteConfirmationEvent = deletionMapper.map(itemsToBeDeleted).toEvent()) }
    }

    private suspend fun deleteSelectedCounters() {
        deleteCounters(DeleteCounters.Params(this.selectedCountersIds))
        this.selectedCountersIds = emptyList()
    }

    private suspend fun shareSelectedCounters() {
        val itemsToBeShared = this.counters.filter { counter -> this.selectedCountersIds.contains(counter.id) }
        updateState { copy(shareEvent = sharingMapper.map(itemsToBeShared).toEvent()) }
    }

    private suspend fun finishEditing() {
        this.isEditing = false
        this.selectedCountersIds = emptyList()
        updateScreenState()
    }

    private suspend fun navigateToCreateCounter() {
        val directions = CountersFragmentDirections.toCreateCounterFragment()
        navigator.navigate(directions)
    }

    private suspend fun updateScreenState() {
        val countersViewData = if (this.isEditing) {
            this.counters.map { counter ->
                CounterViewData.Edit(
                    id = counter.id,
                    title = counter.title,
                    isSelected = selectedCountersIds.contains(counter.id),
                    count = counter.count
                )
            }
        } else {
            this.counters.map { counter ->
                CounterViewData.Counter(
                    id = counter.id,
                    title = counter.title,
                    count = counter.count
                )
            }
        }

        val topLayout = if (this.isEditing) CountersState.TopLayout.Editing else CountersState.TopLayout.Default
        val mainLayout = when {
            this.isSearching && countersViewData.isEmpty() -> CountersState.MainLayout.NoResults
            countersViewData.isEmpty() -> CountersState.MainLayout.NoCounters
            else -> CountersState.MainLayout.Default
        }

        val numberOfSelectedCounters = countersViewData
            .filterIsInstance<CounterViewData.Edit>()
            .filter(CounterViewData.Edit::isSelected)
            .count()

        val isSearchEnabled = mainLayout != CountersState.MainLayout.NoCounters

        updateState {
            copy(
                countersEvent = countersViewData.toEvent(),
                totalItemCount = countersViewData.size,
                totalTimesCount = countersViewData.sumBy(CounterViewData::count),
                topLayoutEvent = topLayout.toEvent(),
                mainLayoutEvent = mainLayout.toEvent(),
                areMenusEnabled = numberOfSelectedCounters > 0,
                isSearchEnabled = isSearchEnabled,
                numberOfSelectedCounters = numberOfSelectedCounters
            )
        }
    }
}
