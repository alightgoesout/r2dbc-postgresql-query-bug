package org.example.pgbug

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.codec.EnumCodec
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import java.util.UUID

const val host = "localhost"
const val port = 5432
const val username = "test_user"
const val password = "test_password"

fun main() {
	val connectionFactory = PostgresqlConnectionFactory(
		PostgresqlConnectionConfiguration.builder()
			.host(host)
			.port(port)
			.username(username)
			.password(password)
			.codecRegistrar(EnumCodec.builder().withEnum("item_status", Item.Status::class.java).build())
			.codecRegistrar(ItemTupleCodec.registrar)
			.codecRegistrar(IntRangeCodec.registrar)
			.build()
	)

	val item = Item(
		id = UUID.randomUUID(),
		name = "Test Item",
		tuple = Item.Tuple("one", 2, 3.3),
		status = Item.Status.GOOD,
		range = 4..7,
	)

	runBlocking {
		connectionFactory.create()
			.awaitFirst()
			.createStatement("INSERT INTO items (id, name, tuple, status, range) values ($1, $2, $3, $4, $5)")
			.bind("$1", item.id)
			.bind("$2", item.name)
			.bind("$3", item.tuple)
			.bind("$4", item.status)
			.bind("$5", item.range)
			.execute()
			.awaitFirstOrNull()
			?.rowsUpdated
			?.awaitFirst()
			?.also { println("rowsUpdated: $it") }
	}

}
