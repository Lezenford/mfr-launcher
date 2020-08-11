package api

data class Version(
    val version: String,

    val clientVersionIsDefined: Boolean,

    val needUpdate: Boolean,

    val updatePlan: UpdatePlan
)