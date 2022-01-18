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

class IntRangeCodec(
	private val oid: Int,
	private val allocator: ByteBufAllocator,
) : Codec<IntRange> {
	override fun canDecode(dataType: Int, format: Format, type: Class<*>) =
		dataType == oid && format == Format.FORMAT_TEXT && type == IntRange::class.java

	override fun canEncode(value: Any) = value is IntRange

	override fun canEncodeNull(type: Class<*>) = type == IntRange::class.java

	override fun decode(buffer: ByteBuf?, dataType: Int, format: Format, type: Class<out IntRange>): IntRange? =
		if (buffer != null && dataType == oid && format == Format.FORMAT_TEXT) {
			ByteArray(buffer.readableBytes())
				.also(buffer::readBytes)
				.let(::String)
				.let(regex::matchEntire)
				.let { it!!.groups[1]!!.value.toInt()..it.groups[2]!!.value.toInt() }
		} else {
			null
		}

	override fun encode(value: Any) = encode(value, oid)

	override fun encode(value: Any, dataType: Int) = EncodedParameter(Format.FORMAT_TEXT, oid, mono {
		value as IntRange
		ByteBufUtils.encode(allocator, "[${value.first},${value.last}]")
	})

	override fun encodeNull() = EncodedParameter(Format.FORMAT_BINARY, oid, EncodedParameter.NULL_VALUE)

	companion object {
		private val regex = """\[(\d+),(\d+)]""".toRegex()

		val registrar: CodecRegistrar
			get() = CodecRegistrar { connection, allocator, registry ->
				PostgresTypes.from(connection).lookupType("item_range")
					.doOnNext { type ->
						registry.addFirst(IntRangeCodec(type.oid, allocator))
					}
					.then()
			}
	}
}
