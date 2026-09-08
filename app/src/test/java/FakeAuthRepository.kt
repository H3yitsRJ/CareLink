class FakeAuthRepository {

    private val users = mutableMapOf<String, String>()

    var currentUserEmail: String? = null
        private set

    fun register(
        email: String,
        password: String
    ): Boolean {
        if (email in users) {
            return false
        }

        users[email] = password
        currentUserEmail = email
        return true
    }

    fun login(
        email: String,
        password: String
    ): Boolean {
        val savedPassword = users[email]

        return if (savedPassword == password) {
            currentUserEmail = email
            true
        } else {
            false
        }
    }

    fun logout() {
        currentUserEmail = null
    }

    fun isLoggedIn(): Boolean {
        return currentUserEmail != null
    }
}