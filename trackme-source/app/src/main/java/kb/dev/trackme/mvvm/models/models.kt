package kb.dev.trackme.mvvm.models

enum class UserType{
    GUEST
}

data class User(val id: Int, val name: String, val type: UserType = UserType.GUEST)

data class Session(
    val  id: Int,
    val createdAt: Long,
    val startAt: Long,
    val endAt: Long,
    val avgSpeed: Float,
    val distance: Long,
    val mapRoute: String
)
