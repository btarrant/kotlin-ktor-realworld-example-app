package io.realworld.app.domain.repository

import io.realworld.app.domain.User
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.junit.Assert.*
import org.junit.Test

class UserRepositoryCompatibilityTest : UserRepositoryFixture() {
    @Test fun `creation round trips fields and missing lookups return null`() {
        val input = User(email = "alice@example.com", username = "alice", password = "stored-password",
            bio = "About Alice", image = "https://example.com/alice.png")
        val id = requireNotNull(repository.create(input))
        val expected = input.copy(id = id)
        assertEquals(expected, repository.findByEmail(input.email))
        assertEquals(expected, repository.findByUsername(input.username!!))
        assertNull(repository.findByEmail("missing@example.com"))
        assertNull(repository.findByUsername("missing"))
    }

    @Test fun `generated identities are positive distinct and match persisted rows`() {
        val alice = user("alice")
        val bob = user("bob")
        assertTrue(alice.id!! > 0)
        assertTrue(bob.id!! > 0)
        assertNotEquals(alice.id, bob.id)
        assertEquals(alice, repository.findByEmail(alice.email))
        assertEquals(bob, repository.findByUsername(bob.username!!))
    }

    @Test fun `update changes fields and lookup keys while preserving identity and other users`() {
        val alice = user("alice")
        val bob = user("bob")
        val updated = alice.copy(email = "new@example.com", username = "new-alice",
            password = "new-password", bio = "Updated bio", image = "https://example.com/new.png")
        assertEquals(updated, repository.update(alice.email, updated))
        assertEquals(updated, repository.findByEmail(updated.email))
        assertEquals(updated, repository.findByUsername(updated.username!!))
        assertNull(repository.findByEmail(alice.email))
        assertNull(repository.findByUsername(alice.username!!))
        assertEquals(bob, repository.findByEmail(bob.email))
    }

    @Test fun `partial update preserves omitted fields`() {
        val alice = user("alice")
        val populated = alice.copy(bio = "Original", image = "https://example.com/original.png")
        repository.update(alice.email, populated)
        assertEquals(populated.copy(bio = "Changed"),
            repository.update(alice.email, User(email = alice.email, bio = "Changed")))
    }

    @Test fun `composite primary key rejects duplicate pairs without losing original relationship`() {
        val alice = user("alice")
        val bob = user("bob")
        repository.follow(alice.email, bob.username!!)
        val error = assertThrows(ExposedSQLException::class.java) {
            repository.follow(alice.email, bob.username)
        }
        assertEquals("23505", error.sqlState)
        assertEquals(setOf(bob.id!! to alice.id!!), relationships())
    }

    @Test fun `multiple followers followees and reciprocal pairs preserve relationship direction`() {
        val alice = user("alice")
        val bob = user("bob")
        val carol = user("carol")
        assertEquals(bob, repository.follow(alice.email, bob.username!!))
        repository.follow(alice.email, carol.username!!)
        repository.follow(carol.email, bob.username)
        repository.follow(bob.email, alice.username!!)
        assertEquals(setOf(bob.id!! to alice.id!!, carol.id!! to alice.id,
            bob.id to carol.id, alice.id to bob.id), relationships())
    }
}
