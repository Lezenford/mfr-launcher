package api

data class UpdatePlan(
    val updates: List<String>,

    val refreshSchema: Boolean,

    val refreshApplied: Boolean
)