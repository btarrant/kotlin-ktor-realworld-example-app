package io.realworld.app.web.controllers

import io.realworld.app.domain.repository.Articles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.junit.After
import org.junit.Before
import java.sql.DriverManager
import java.util.UUID

abstract class ArticleSearchFixture {
    private lateinit var database: Database
    private lateinit var url: String

    @Before fun connectSearchDatabase() {
        url = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        database = Database.connect(url, driver = "org.h2.Driver")
        org.jetbrains.exposed.sql.transactions.transaction(database) { SchemaUtils.create(Articles) }
    }

    @After fun closeSearchDatabase() {
        try {
            DriverManager.getConnection(url).use { c -> c.createStatement().use { it.execute("SHUTDOWN") } }
        } finally {
            TransactionManager.closeAndUnregister(database)
        }
    }

    protected fun seed(slug: String, title: String?, body: String = "", description: String = "") {
        org.jetbrains.exposed.sql.transactions.transaction(database) {
            Articles.insert {
                it[Articles.slug] = slug
                it[Articles.title] = title
                it[Articles.body] = body
                it[Articles.description] = description
            }
        }
    }

    protected fun query(term: String) = "/api/articles/search?q=" +
        java.net.URLEncoder.encode(term, "UTF-8")
}
