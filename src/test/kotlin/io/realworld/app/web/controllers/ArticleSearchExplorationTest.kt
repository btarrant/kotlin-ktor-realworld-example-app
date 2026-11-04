package io.realworld.app.web.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.realworld.app.config.mainModule
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.Articles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.*
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

interface SearchExploration

/** Characterization and bounded measurements, not a promise of new API semantics or a benchmark gate. */
@Category(SearchExploration::class)
class ArticleSearchExplorationTest : ArticleSearchFixture() {
    @Test fun `uppercase Q HTTP parameter is accepted by Ktor lookup`() {
        seed("kotlin", "Kotlin")
        withTestApplication({ mainModule() }) {
            with(handleRequest(HttpMethod.Get, "/api/articles/search?Q=kotlin")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val json = jacksonObjectMapper().readTree(response.content)
                assertEquals(1, json["articlesCount"].asInt())
                assertEquals("kotlin", json["articles"][0]["slug"].asText())
            }
        }
    }

    @Test fun `observe repeated and ambiguous q parameters`() {
        seed("hit", "Kotlin")
        withTestApplication({ mainModule() }) {
            for (suffix in listOf("q=kotlin&q=absent", "q=absent&q=kotlin", "q=&q=kotlin",
                "q=kotlin&q=", "q=kotlin&%71=absent", "q", "Q=kotlin")) {
                with(handleRequest(HttpMethod.Get, "/api/articles/search?$suffix")) {
                    assertTrue(response.status() in listOf(HttpStatusCode.OK, HttpStatusCode.BadRequest))
                    println("REPEATED $suffix status=${response.status()} body=${response.content}")
                }
            }
        }
    }

    @Test fun `observe unusually large queries in the test host`() {
        seed("hit", "Kotlin")
        withTestApplication({ mainModule() }) {
            for (size in listOf(4096, 65536, 1048576)) {
                for (q in listOf("x".repeat(size), " ".repeat(size) + "kotlin" + " ".repeat(size))) {
                    val start = System.nanoTime()
                    with(handleRequest(HttpMethod.Get, query(q))) {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val json = jacksonObjectMapper().readTree(response.content)
                        assertEquals(if (q.startsWith("x")) 0 else 1, json["articlesCount"].asInt())
                        println("LARGE_QUERY decodedChars=${q.length} encodedUriChars=${query(q).length} elapsedMs=${(System.nanoTime()-start)/1e6} count=${json["articlesCount"]}")
                    }
                }
            }
        }
    }

    @Test fun `observe Unicode normalization and case folding boundaries`() {
        seed("unicode", "CAFÉ Straße İstanbul ΟΣ", "東京")
        withTestApplication({ mainModule() }) {
            for (q in listOf("café", "cafe", "cafe\u0301", "STRASSE", "straße", "istanbul", "i\u0307stanbul", "οσ", "ος")) {
                with(handleRequest(HttpMethod.Get, query(q))) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    println("UNICODE codepoints=${q.codePoints().toArray().joinToString()} q=$q body=${response.content}")
                }
            }
        }
    }

    @Test fun `observe large result sets timing response size and heap snapshots`() {
        withTestApplication({ mainModule() }) {
            for (size in listOf(100, 1000, 5000)) {
                transaction {
                    Articles.deleteAll()
                    Articles.batchInsert(0 until size) { i ->
                        this[Articles.slug] = "article-$i"
                        this[Articles.title] = "Title $i"
                        this[Articles.description] = ""
                        this[Articles.body] = (if (i % 2 == 0) "needle " else "other ") + "x".repeat(1024)
                    }
                }
                val pools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
                pools.forEach { it.resetPeakUsage() }
                val repository = ArticleRepository()
                val queryStart = System.nanoTime()
                assertEquals(size / 2, repository.search("needle").size)
                val queryMs = (System.nanoTime() - queryStart) / 1e6
                val before = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
                val start = System.nanoTime()
                with(handleRequest(HttpMethod.Get, query("needle"))) {
                    val elapsedMs = (System.nanoTime() - start) / 1e6
                    val after = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
                    assertEquals(HttpStatusCode.OK, response.status())
                    val json = jacksonObjectMapper().readTree(response.content)
                    assertEquals(size / 2, json["articlesCount"].asInt())
                    assertEquals((0 until size step 2).map { "article-$it" }, json["articles"].map { it["slug"].asText() })
                    println("LOAD rows=$size matches=${size/2} repositoryMs=$queryMs httpMs=$elapsedMs responseBytes=${response.content!!.toByteArray(Charsets.UTF_8).size} heapBefore=$before heapAfter=$after sumPoolPeaks=${pools.sumOf { it.peakUsage.used }}")
                }
            }
        }
    }

    @Test fun `characterize pagination parameters without adding pagination`() {
        repeat(25) { seed("item-$it", "match") }
        withTestApplication({ mainModule() }) {
            val baseline = handleRequest(HttpMethod.Get, query("match")).response.content
            for (params in listOf("", "&limit=1&offset=2", "&limit=-1&offset=invalid")) {
                with(handleRequest(HttpMethod.Get, query("match") + params)) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(baseline, response.content)
                    println("PAGINATION params=$params count=${jacksonObjectMapper().readTree(response.content)["articlesCount"]}")
                }
            }
        }
    }
}
