package com.sahlone.mmq.logging

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import java.util.UUID

@JsonDeserialize(using = TracingContext::class)
data class TracingContext(val correlationId: String = "${UUID.randomUUID()}") : JsonDeserializer<TracingContext>() {

    fun toMap(): Map<String, String> = mapOf(CORRELATION_ID to correlationId)

    companion object {
        const val CORRELATION_ID = "correlation_id"
    }

    @JsonValue
    fun serialize(): String = correlationId

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): TracingContext =
        TracingContext(p?.text ?: throw IllegalStateException("Couldn't deserialize tracing context."))
}

inline fun <reified T> T.createContextualLogger(): ContextualLogger<T> =
    ContextualLogger(LoggerFactory.getLogger(T::class.java))

typealias ContextualLoggerMarker = () -> Pair<String, Any>

class ContextualLogger<T>(private val delegate: Logger) {

    private fun generateMarker(context: TracingContext, additionalArgs: List<ContextualLoggerMarker>): Marker =
        appendEntries(context.toMap().plus(additionalArgs.map { it.invoke() }))

    fun debug(context: TracingContext, vararg additionalArgs: ContextualLoggerMarker, msg: () -> String) {
        if (delegate.isDebugEnabled) {
            delegate.debug(generateMarker(context, additionalArgs.asList()), msg.invoke())
        }
    }

    fun debugWithCause(
        context: TracingContext,
        cause: Throwable,
        vararg additionalArgs: ContextualLoggerMarker,
        msg: () -> String
    ) {
        if (delegate.isDebugEnabled) {
            delegate.debug(generateMarker(context, additionalArgs.asList()), msg.invoke(), cause)
        }
    }

    fun error(context: TracingContext, vararg additionalArgs: ContextualLoggerMarker, msg: () -> String) {
        if (delegate.isErrorEnabled) {
            delegate.error(generateMarker(context, additionalArgs.asList()), msg.invoke())
        }
    }

    fun errorWithCause(
        context: TracingContext,
        cause: Throwable,
        vararg additionalArgs: ContextualLoggerMarker,
        msg: () -> String
    ) {
        if (delegate.isErrorEnabled) {
            delegate.error(generateMarker(context, additionalArgs.asList()), msg.invoke(), cause)
        }
    }
}
