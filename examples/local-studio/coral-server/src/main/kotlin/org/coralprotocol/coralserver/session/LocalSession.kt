package org.coralprotocol.coralserver.session

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.*
import kotlinx.coroutines.CompletableDeferred
import org.coralprotocol.coralserver.EventBus
import org.coralprotocol.coralserver.agent.graph.AgentGraph
import org.coralprotocol.coralserver.models.Message
import org.coralprotocol.coralserver.models.Thread
import org.coralprotocol.coralserver.models.resolve
import org.coralprotocol.coralserver.payment.PaymentSessionId
import org.coralprotocol.coralserver.session.models.SessionAgent
import org.coralprotocol.coralserver.session.models.SessionAgentState
import org.coralprotocol.coralserver.session.models.isConnected
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Session class to hold stateful information for a specific application and privacy key.
 * [devRequiredAgentStartCount] is the number of agents that need to register before the session can proceed. This is for devmode only.
 */
class LocalSession(
    override val id: String,
    override val paymentSessionId: PaymentSessionId? = null,
    val applicationId: String,
    val privacyKey: String,
    val agentGraph: AgentGraph?,
    val groups: List<Set<String>> = listOf(),
    var devRequiredAgentStartCount: Int = 0,
): Session() {
    var agents = ConcurrentHashMap<String, SessionAgent>()
    private val debugAgents = ConcurrentSet<String>()

    private val threads = ConcurrentHashMap<String, Thread>()

    private val agentNotifications = ConcurrentHashMap<String, CompletableDeferred<List<Message>>>()

    private val lastReadMessageIndex = ConcurrentHashMap<Pair<String, String>, Int>()

    private val agentGroupScheduler = GroupScheduler(groups)
    private val countBasedScheduler = CountBasedScheduler()

    private val eventBus = EventBus<SessionEvent>()
    val events get() = eventBus.events

    init {
        agentGraph?.run {
            for (id in agents.keys) {
                registerAgent(id.toString())
                setAgentState(agentId = id.toString(), state = SessionAgentState.Connecting)
            }
        }
    }


    fun getAllThreadsAgentParticipatesIn(agentId: String): List<Thread> {
        return threads.values.filter { it.participants.contains(agentId) }
    }

    fun clearAll() {
        agents.clear()
        threads.clear()
        agentNotifications.clear()
        lastReadMessageIndex.clear()
        countBasedScheduler.clear()
        agentGroupScheduler.clear()
    }

    fun connectAgent(agentId: String): SessionAgent? {
        val agent = agents[agentId] ?: return null
//        if (agent.state.isConnected()) throw AssertionError("Agent $agentId is already connected")
        if (agent.state.isConnected()) logger.warn { "Agent $agentId is already connected" }
        agent.state = SessionAgentState.Busy;
        agentGroupScheduler.markAgentReady(agentId)
        countBasedScheduler.markAgentReady(agent.id)
        eventBus.emit(SessionEvent.AgentStateUpdated(agent.id, agent.state))
        return agent
    }

    fun setAgentState(agentId: String, state: SessionAgentState): SessionAgentState? {
        val agent = agents[agentId] ?: return null
        val oldState = agent.state
        if (oldState == SessionAgentState.Connecting && state.isConnected()) {
            agentGroupScheduler.markAgentReady(agentId)
            countBasedScheduler.markAgentReady(agent.id)
        }
        agent.state = state
        eventBus.emit(SessionEvent.AgentStateUpdated(agent.id, agent.state))
        return oldState

    }

    fun disconnectAgent(agentId: String) {
        val agent = agents[agentId] ?: return
        agent.state = SessionAgentState.Disconnected;
        eventBus.emit(SessionEvent.AgentStateUpdated(agent.id, agent.state))
    }

    fun registerAgent(
        agentId: String,
        agentUri: String? = null,
        agentDescription: String? = null,
        force: Boolean = false
    ): SessionAgent? {
        if (agents.containsKey(agentId)) {
            logger.warn { "$agentId has already been registered" }
            if (!force) {
                return null;
            }
        }

        val graphAgent = agentGraph?.agents[agentId]
        val sessionAgent = SessionAgent(
            id = agentId,
            description = graphAgent?.description ?: graphAgent?.registryAgent?.info?.description ?: agentDescription ?: "",
            extraTools = agentGraph?.let {
                it.agents[agentId]?.customToolAccess?.mapNotNull { tool -> it.customTools[tool] }?.toSet()
            } ?: setOf(),
            coralPlugins = graphAgent?.plugins ?: setOf(),
            mcpUrl = agentUri
        )
        agents[sessionAgent.id] = sessionAgent

        return sessionAgent
    }

    fun getRegisteredAgentsCount(): Int = countBasedScheduler.getRegisteredAgentsCount()

    suspend fun waitForGroup(agentId: String, timeoutMs: Long): Boolean =
        agentGroupScheduler.waitForGroup(agentId, timeoutMs)

    suspend fun waitForAgentCount(targetCount: Int, timeoutMs: Long): Boolean =
        countBasedScheduler.waitForAgentCount(targetCount, timeoutMs)

    fun getAgent(agentId: String): SessionAgent? = agents[agentId]

    fun getAllAgents(includeDebug: Boolean = false): List<SessionAgent> = when (includeDebug) {
        true -> agents.values.toList()
        false -> agents.values.filter { !debugAgents.contains(it.id) }
    }

    fun getAllThreads(): List<Thread> = threads.values.toList()

    fun registerDebugAgent(): SessionAgent {
        val id = UUID.randomUUID().toString()
        if (agents[id] !== null) throw AssertionError("Debug agent id collision")
        val sessionAgent = SessionAgent(id = id, description = "", mcpUrl = "n/a")
        agents[id] = sessionAgent
        debugAgents.add(id)
        return sessionAgent
    }

    fun createThread(name: String, creatorId: String, participantIds: List<String>): Thread {
        if (creatorId != "debug" && !agents.containsKey(creatorId)) {
            throw IllegalArgumentException("Creator agent $creatorId not found")
        }

        val validParticipants = participantIds.filter { agents.containsKey(it) }.toMutableList()

        if (!validParticipants.contains(creatorId)) {
            validParticipants.add(creatorId)
        }

        val thread = Thread(
            name = name,
            creatorId = creatorId,
            participants = validParticipants
        )
        threads[thread.id] = thread

        eventBus.emit(
            SessionEvent.ThreadCreated(
                id = thread.id,
                name = name,
                creatorId = creatorId,
                participants = validParticipants,
                summary = null
            )
        )
        return thread
    }

    fun getThread(threadId: String): Thread? = threads[threadId]

    fun getThreadsForAgent(agentId: String): List<Thread> {
        return threads.values.filter { it.participants.contains(agentId) }
    }

    fun addParticipantToThread(threadId: String, participantId: String): Boolean {
        val thread = threads[threadId] ?: return false
        val agent = agents[participantId] ?: return false

        if (thread.isClosed) return false

        if (!thread.participants.contains(participantId)) {
            thread.participants.add(participantId)
            lastReadMessageIndex[Pair(participantId, threadId)] = thread.messages.size
        }
        return true
    }

    fun removeParticipantFromThread(threadId: String, participantId: String): Boolean {
        val thread = threads[threadId] ?: return false

        if (thread.isClosed) return false

        return thread.participants.remove(participantId)
    }

    fun closeThread(threadId: String, summary: String): Boolean {
        val thread = threads[threadId] ?: return false

        thread.isClosed = true
        thread.summary = summary

        return true
    }

    fun getColorForSenderId(senderId: String): String {
        val colors = listOf(
            "#FF5733", "#33FF57", "#3357FF", "#F3FF33", "#FF33F3",
            "#33FFF3", "#FF8033", "#8033FF", "#33FF80", "#FF3380"
        )
        val hash = senderId.hashCode()
        val index = Math.abs(hash) % colors.size
        return colors[index]
    }

    fun sendMessage(
        threadId: String,
        senderId: String,
        content: String,
        mentions: List<String> = emptyList()
    ): Message {
        val thread = getThread(threadId) ?: throw IllegalArgumentException("Thread with id $threadId not found")
        val sender = getAgent(senderId) ?: throw IllegalArgumentException("Agent with id $senderId not found")

        val message = Message.create(thread, sender, content, mentions)
        thread.messages.add(message)
        eventBus.emit(SessionEvent.MessageSent(threadId, message.resolve()))
        notifyMentionedAgents(message)
        return message
    }

    private fun notifyMentionedAgents(message: Message) {
        if (message.sender.id == "system") {
            val thread = threads[message.thread.id] ?: return
            thread.participants.forEach { participantId ->
                val deferred = agentNotifications[participantId]
                if (deferred != null && !deferred.isCompleted) {
                    deferred.complete(listOf(message))
                }
            }
            return
        }

        message.mentions.forEach { mentionId ->
            val deferred = agentNotifications[mentionId]
            if (deferred != null && !deferred.isCompleted) {
                deferred.complete(listOf(message))
            }
        }
    }

    suspend fun waitForMentions(agentId: String, timeoutMs: Long): List<Message> {
        if (timeoutMs <= 0) {
            throw IllegalArgumentException("Timeout must be greater than 0")
        }

        val agent = agents[agentId] ?: return emptyList()

        val unreadMessages = getUnreadMessagesForAgent(agentId)
        if (unreadMessages.isNotEmpty()) {
            updateLastReadIndices(agentId, unreadMessages)
            return unreadMessages
        }

        val deferred = CompletableDeferred<List<Message>>()
        agentNotifications[agentId] = deferred

        val result = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: emptyList()

        agentNotifications.remove(agentId)

        updateLastReadIndices(agentId, result)

        return result
    }

    fun getUnreadMessagesForAgent(agentId: String): List<Message> {
        val agent = agents[agentId] ?: return emptyList()

        val result = mutableListOf<Message>()

        val agentThreads = getThreadsForAgent(agentId)

        for (thread in agentThreads) {
            val lastReadIndex = lastReadMessageIndex[Pair(agentId, thread.id)] ?: 0

            val unreadMessages = thread.messages.subList(lastReadIndex, thread.messages.size)

            result.addAll(unreadMessages.filter {
                it.mentions.contains(agentId) || it.sender.id == "system"
            })
        }

        return result
    }

    fun updateLastReadIndices(agentId: String, messages: List<Message>) {
        val messagesByThread = messages.groupBy { it.thread }

        for ((thread, threadMessages) in messagesByThread) {
            val messageIndices = threadMessages.map { thread.messages.indexOf(it) }
            if (messageIndices.isNotEmpty()) {
                val maxIndex = messageIndices.maxOrNull() ?: continue
                lastReadMessageIndex[Pair(agentId, thread.id)] = maxIndex + 1
            }
        }
    }

    override suspend fun destroy(sessionCloseMode: SessionCloseMode) {
        super.destroy(sessionCloseMode)
        clearAll()
    }
}
