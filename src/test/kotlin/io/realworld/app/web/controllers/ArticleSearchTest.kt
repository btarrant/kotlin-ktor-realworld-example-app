package io.realworld.app.web.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.realworld.app.config.mainModule
import io.realworld.app.domain.repository.Articles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ArticleSearchTest {
    @Before fun setup() {
        Database.connect("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Articles)
            Articles.insert {
                it[slug] = "search"
                it[title] = "Kotlin search"
                it[description] = "An article"
                it[body] = "A literal 100%_match and café"
            }
        }
    }

    @Test fun `public route returns serialized article envelope and trims query`() = withTestApplication({ mainModule() }) {
        with(handleRequest(HttpMethod.Get, "/api/articles/search?q=%20KOTLIN%20")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val json = jacksonObjectMapper().readTree(response.content)
            assertEquals(1, json["articlesCount"].asInt())
            assertEquals(1, json["articles"].size())
            assertEquals("search", json["articles"][0]["slug"].asText())
            assertEquals("Kotlin search", json["articles"][0]["title"].asText())
            assertEquals("A literal 100%_match and café", json["articles"][0]["body"].asText())
        }
    }

    @Test fun `missing empty and whitespace queries return validation envelope`() = withTestApplication({ mainModule() }) {
        for (suffix in listOf("", "?q=", "?q=%20%09%20")) {
            with(handleRequest(HttpMethod.Get, "/api/articles/search$suffix")) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                val json = jacksonObjectMapper().readTree(response.content)
                assertEquals("must not be blank", json["errors"]["q"][0].asText())
            }
        }
    }

    @Test fun `no matches returns empty list and zero count`() = withTestApplication({ mainModule() }) {
        with(handleRequest(HttpMethod.Get, "/api/articles/search?q=absent")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val json = jacksonObjectMapper().readTree(response.content)
            assertEquals(0, json["articles"].size())
            assertEquals(0, json["articlesCount"].asInt())
        }
    }

    @Test fun `encoded literal wildcard matches through HTTP`() = withTestApplication({ mainModule() }) {
        with(handleRequest(HttpMethod.Get, "/api/articles/search?q=100%25_match")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(1, jacksonObjectMapper().readTree(response.content)["articlesCount"].asInt())
        }
    }
}
