package kb.dev.trackme.mvvm

enum class UserType{
    GUEST
}

data class User(val id: Long, val name: String, val type: UserType = UserType.GUEST)

data class Session(
    val  id: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val startAt: Long = System.currentTimeMillis(),
    val endAt: Long? = null,
    val avgSpeed: Double = 0.0,
    val distance: Long = 0,
    val mapRoute: String? = null
)
