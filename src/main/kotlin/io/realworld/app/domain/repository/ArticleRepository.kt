package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Locale

internal object Articles : LongIdTable() {
    val slug = varchar("slug", 255).uniqueIndex()
    val title = text("title").nullable()
    val description = text("description").nullable()
    val body = text("body")
}

class ArticleRepository {
    init {
        transaction { SchemaUtils.create(Articles) }
    }

    fun search(term: String): List<Article> = transaction {
        // LOCATE treats %, _ and backslashes literally; QueryParameter binds user input.
        val query = QueryParameter(term.lowercase(Locale.ROOT), TextColumnType())
        Articles.select {
            (CustomFunction<Int>("LOCATE", IntegerColumnType(), query, Articles.title.lowerCase()) greater 0) or
                (CustomFunction<Int>("LOCATE", IntegerColumnType(), query, Articles.body.lowerCase()) greater 0)
        }.orderBy(Articles.id).map {
            Article(slug = it[Articles.slug], title = it[Articles.title],
                description = it[Articles.description], body = it[Articles.body])
        }
    }
}
