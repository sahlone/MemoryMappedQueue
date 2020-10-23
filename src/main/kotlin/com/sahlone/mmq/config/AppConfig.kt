package com.sahlone.mmq.config

data class AppConfig(
    val baseQueueDir: String,
    val maxQueueSizeBytes: Int,
    val queueName: String,
    val mmFileMode: String = "rw"
) {

    companion object {
        /*
            The size of metadata associated with each message
        */
        const val MESSAGE_META_SIZE = 4
        const val QUEUE_META_SIZE = 8
    }
}
