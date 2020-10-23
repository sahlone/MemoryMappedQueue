package com.sahlone.mmq.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import com.fasterxml.jackson.core.JsonGenerator
import net.logstash.logback.composite.AbstractFieldJsonProvider
import net.logstash.logback.composite.JsonWritingUtils.writeStringField

/**
 * provides error class and message as json fields.
 */
class ErrorProvider : AbstractFieldJsonProvider<ILoggingEvent>() {

    override fun writeTo(generator: JsonGenerator, event: ILoggingEvent) {
        if (event.throwableProxy != null) {
            writeStringField(generator, "error-class", event.throwableProxy.className)
            writeStringField(generator, "error-message", event.throwableProxy.message)
        }
    }
}
