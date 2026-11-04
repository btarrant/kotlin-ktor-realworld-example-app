package io.realworld.app.domain.repository

import org.junit.Assert.*
import org.junit.Test
import org.junit.experimental.categories.Category

interface KnownUpstreamDefect

/** Intended behavior assertions: run explicitly with upstreamDefectDiagnostics; failures are expected today. */
@Category(KnownUpstreamDefect::class)
class UserFollowDiagnosticsTest : UserRepositoryFixture() {
    @Test fun `following lookup recognizes actor following target`() {
        val alice = user("alice")
        val bob = user("bob")
        repository.follow(alice.email, bob.username!!)
        assertTrue("Alice follows Bob", repository.findIsFollowUser(alice.email, bob.id!!))
    }

    @Test fun `unfollow removes the forward relationship`() {
        val alice = user("alice")
        val bob = user("bob")
        repository.follow(alice.email, bob.username!!)
        repository.unfollow(alice.email, bob.username)
        assertEquals("Alice's forward relationship should be removed", emptySet<Pair<Long, Long>>(), relationships())
    }

    @Test fun `unfollow preserves reciprocal and unrelated relationships`() {
        val alice = user("alice")
        val bob = user("bob")
        val carol = user("carol")
        repository.follow(alice.email, bob.username!!)
        repository.follow(bob.email, alice.username!!)
        repository.follow(carol.email, bob.username)
        repository.unfollow(alice.email, bob.username)
        assertEquals("Bob's reciprocal relationship must survive",
            setOf(alice.id!! to bob.id!!, bob.id to carol.id!!), relationships())
    }
}
