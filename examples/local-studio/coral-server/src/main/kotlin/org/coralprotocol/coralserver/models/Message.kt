package org.coralprotocol.coralserver.models

import org.coralprotocol.coralserver.session.models.SessionAgent
import java.util.*

/**
 * Represents a message in a thread.
 */
class Message private constructor (
    val id: String = UUID.randomUUID().toString(),
    val thread: Thread,
    val sender: SessionAgent,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mentions: List<String> = emptyList(),
    var telemetry: Telemetry?
)  {
    companion object {
        fun create(thread: Thread, sender: SessionAgent, content: String, mentions: List<String> = emptyList()): Message {
            if (thread.isClosed) throw IllegalArgumentException("Thread $thread is closed")

            if (!thread.participants.contains(sender.id)) {
                throw IllegalArgumentException("Sender agent not a member of thread $thread")
            }

            val validMentions = mentions.filter { thread.participants.contains(it) }
            return Message (
                thread = thread,
                sender = sender,
                content = content,
                mentions = validMentions,
                telemetry = null
            )
        }
    }
}

fun Message.resolve(): ResolvedMessage = ResolvedMessage(
    id = id,
    threadName = this.thread.name,
    threadId = this.thread.id,
    senderId = this.sender.id,
    content = content,
    timestamp = timestamp,
    mentions = mentions
)