package io.realworld.app.domain.repository

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ArticleRepositoryTest {
    private lateinit var repository: ArticleRepository

    @Before
    fun setup() {
        Database.connect("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        repository = ArticleRepository()
        transaction {
            fun article(slug: String, title: String?, body: String, description: String = "") {
                Articles.insert {
                    it[Articles.slug] = slug
                    it[Articles.title] = title
                    it[Articles.body] = body
                    it[Articles.description] = description
                }
            }
            article("title", "Kotlin guide", "Intro")
            article("body", null, "Learn KOTLIN here")
            article("both", "Kotlin", "Kotlin")
            article("description", "Unrelated", "Other", "Kotlin")
            article("literal", "100%_done\\path", "O'Reilly café")
        }
    }

    @Test fun `matches title or body case insensitively once per article`() {
        assertEquals(listOf("title", "body", "both"), repository.search("kOtLiN").map { it.slug })
    }

    @Test fun `matches partial text and preserves fields`() {
        val article = repository.search("GUID").single()
        assertEquals("title", article.slug)
        assertEquals("Kotlin guide", article.title)
        assertEquals("Intro", article.body)
        assertEquals("", article.description)
    }

    @Test fun `treats wildcard and sql characters literally`() {
        for (term in listOf("%", "_", "\\", "O'Reilly", "café")) {
            assertEquals(term, listOf("literal"), repository.search(term).map { it.slug })
        }
        assertEquals(emptyList<String>(), repository.search("' OR 1=1 --").map { it.slug })
    }

    @Test fun `returns empty results for no match`() {
        assertEquals(emptyList<String>(), repository.search("absent").map { it.slug })
    }
}
