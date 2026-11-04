package io.realworld.app.domain.repository

import io.realworld.app.domain.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import java.sql.DriverManager
import java.util.UUID

abstract class UserRepositoryFixture {
    protected lateinit var repository: UserRepository
    private lateinit var database: Database
    private lateinit var jdbcUrl: String

    @Before fun setupRepository() {
        jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        database = Database.connect(jdbcUrl, driver = "org.h2.Driver")
        repository = UserRepository()
    }

    @After fun closeDatabase() {
        try {
            DriverManager.getConnection(jdbcUrl).use { connection ->
                connection.createStatement().use { it.execute("SHUTDOWN") }
            }
        } finally {
            TransactionManager.closeAndUnregister(database)
        }
    }

    protected fun user(name: String): User {
        val input = User(email = "$name@example.com", username = name, password = "stored-password")
        val id = requireNotNull(repository.create(input))
        return input.copy(id = id)
    }

    // Stored pairs are (target, follower); do not use the known-defective lookup as the oracle.
    protected fun relationships(): Set<Pair<Long, Long>> = transaction(database) {
        Follows.selectAll().map { it[Follows.user] to it[Follows.follower] }.toSet()
    }
}
