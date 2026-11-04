package io.realworld.app.web.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.realworld.app.config.mainModule
import org.junit.Assert.*
import org.junit.Test

class ArticleSearchEdgeCasesTest : ArticleSearchFixture() {
    @Test fun `multiple title and body matches have exact count and stable insertion order`() {
        seed("z-title", "Kotlin title")
        seed("a-body", null, "Learn KOTLIN")
        seed("m-both", "Kotlin", "Kotlin")
        seed("description-only", "Other", "Other", "Kotlin")
        withTestApplication({ mainModule() }) {
            repeat(3) {
                with(handleRequest(HttpMethod.Get, query("kOtLiN"))) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val json = jacksonObjectMapper().readTree(response.content)
                    assertEquals(3, json["articlesCount"].asInt())
                    assertEquals(listOf("z-title", "a-body", "m-both"),
                        json["articles"].map { it["slug"].asText() })
                }
            }
        }
    }

    @Test fun `long literal term matches without truncation`() {
        val term = "x".repeat(2048)
        seed("long", "Long text", "before${term}after")
        withTestApplication({ mainModule() }) {
            for ((q, count) in listOf(term to 1, (term + "y") to 0)) {
                with(handleRequest(HttpMethod.Get, query(q))) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val json = jacksonObjectMapper().readTree(response.content)
                    assertEquals(count, json["articlesCount"].asInt())
                    assertEquals(count, json["articles"].size())
                }
            }
        }
    }

    @Test fun `whitespace padding is trimmed while internal whitespace remains literal`() {
        seed("spaces", "space  stays")
        withTestApplication({ mainModule() }) {
            val padding = " \t\n".repeat(256)
            for ((q, count) in listOf((padding + "space  stays" + padding) to 1, "space stays" to 0)) {
                with(handleRequest(HttpMethod.Get, query(q))) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(count, jacksonObjectMapper().readTree(response.content)["articlesCount"].asInt())
                }
            }
            with(handleRequest(HttpMethod.Get, query(padding))) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test fun `accented and non ASCII literals match with supported case conversion`() {
        seed("unicode", "CAFÉ ÉCOLE", "naïve Москва 東京 😀")
        withTestApplication({ mainModule() }) {
            for (q in listOf("café", "école", "NAÏVE", "МОСКВА", "東京", "😀")) {
                with(handleRequest(HttpMethod.Get, query(q))) {
                    assertEquals(q, HttpStatusCode.OK, response.status())
                    val json = jacksonObjectMapper().readTree(response.content)
                    assertEquals(q, 1, json["articlesCount"].asInt())
                    assertEquals("unicode", json["articles"].single()["slug"].asText())
                    assertEquals("CAFÉ ÉCOLE", json["articles"].single()["title"].asText())
                }
            }
        }
    }
}
