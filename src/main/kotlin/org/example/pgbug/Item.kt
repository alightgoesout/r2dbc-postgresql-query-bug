package org.example.pgbug

import java.util.UUID

data class Item(
	val id: UUID,
	val name: String,
	val tuple: Tuple<String, Int, Double>,
	val status: Status,
	val range: IntRange,
) {
	data class Tuple<A, B, C>(val one: A, val two: B, val three: C) {
		override fun toString(): String = "($one,$two,$three)"
	}

	enum class Status {
		GOOD, BAD
	}
}
