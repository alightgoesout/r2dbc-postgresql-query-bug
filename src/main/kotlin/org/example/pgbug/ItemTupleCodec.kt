package org.example.pgbug

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.r2dbc.postgresql.client.EncodedParameter
import io.r2dbc.postgresql.codec.Codec
import io.r2dbc.postgresql.codec.PostgresTypes
import io.r2dbc.postgresql.extension.CodecRegistrar
import io.r2dbc.postgresql.message.Format
import io.r2dbc.postgresql.util.ByteBufUtils
import kotlinx.coroutines.reactor.mono

class ItemTupleCodec(
	private val oid: Int,
	private val allocator: ByteBufAllocator
) : Codec<Item.Tuple<String, Int, Double>> {

	override fun canDecode(dataType: Int, format: Format, type: Class<*>) =
		dataType == oid && format == Format.FORMAT_TEXT && type == Item.Tuple::class.java

	override fun canEncode(value: Any) = value is Item.Tuple<*, *, *>

	override fun canEncodeNull(type: Class<*>) = type == Item.Tuple::class.java

	override fun decode(
		buffer: ByteBuf?,
		dataType: Int,
		format: Format,
		type: Class<out Item.Tuple<String, Int, Double>>,
	): Item.Tuple<String, Int, Double>? =
		if (buffer != null && dataType == oid && format == Format.FORMAT_TEXT) {
			ByteArray(buffer.readableBytes())
				.also(buffer::readBytes)
				.let(::String)
				.let(regex::matchEntire)
				.let {
					Item.Tuple(
						it!!.groups[1]!!.value,
						it.groups[2]!!.value.toInt(),
						it.groups[3]!!.value.toDouble(),
					)
				}
		} else {
			null
		}

	override fun encode(value: Any) = encode(value, oid)

	override fun encode(value: Any, dataType: Int): EncodedParameter {
		val (one, two, three) = value as Item.Tuple<*, *, *>
		return EncodedParameter(Format.FORMAT_TEXT, oid, mono {
			ByteBufUtils.encode(allocator, "($one,$two,$three)")
		})
	}

	override fun encodeNull() = EncodedParameter(Format.FORMAT_BINARY, oid, EncodedParameter.NULL_VALUE)

	companion object {
		private val regex = """\((\w+),(\d+),([\d.]+)\)""".toRegex()

		val registrar: CodecRegistrar
			get() = CodecRegistrar { connection, allocator, registry ->
				PostgresTypes.from(connection).lookupType("item_tuple")
					.doOnNext { type ->
						registry.addFirst(ItemTupleCodec(type.oid, allocator))
					}
					.then()
			}
	}
}
