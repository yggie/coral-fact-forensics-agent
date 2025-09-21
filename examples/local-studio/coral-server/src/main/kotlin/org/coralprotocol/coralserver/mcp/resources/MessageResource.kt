package org.coralprotocol.coralserver.mcp.resources

import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XML
import org.coralprotocol.coralserver.mcp.McpResources.MESSAGE_RESOURCE_URI
import org.coralprotocol.coralserver.models.ResolvedThread
import org.coralprotocol.coralserver.models.resolve
import org.coralprotocol.coralserver.server.CoralAgentIndividualMcp

private fun CoralAgentIndividualMcp.handler(request: ReadResourceRequest): ReadResourceResult {
    val threadsAgentPrivyIn: List<ResolvedThread> = this.localSession.getAllThreadsAgentParticipatesIn(this.connectedAgentId).map { it -> it.resolve() }
    val renderedThreads: String = XML.encodeToString(threadsAgentPrivyIn, rootName = QName("threads"))
    return ReadResourceResult(
        contents = listOf(
            TextResourceContents(
                text = renderedThreads,
                uri = request.uri,
                mimeType = "application/xml",
            )
        )
    )
}

fun CoralAgentIndividualMcp.addMessageResource() {
    addResource(
        name = "message",
        description = "Message resource",
        uri = MESSAGE_RESOURCE_URI.toString(),
        mimeType = "application/json",
        readHandler = { request: ReadResourceRequest ->
            handler(request)
        },
    )
}
