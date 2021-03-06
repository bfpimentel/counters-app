package dev.pimentel.counters.presentation.examples.data

sealed class ExamplesIntention {

    object GetExamples : ExamplesIntention()

    data class SelectExample(val name: String) : ExamplesIntention()

    object Close : ExamplesIntention()
}
