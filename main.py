import os
import json
import asyncio
import logging
import traceback
import urllib.parse

from langchain.prompts import ChatPromptTemplate
from langchain.chat_models import init_chat_model
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain.agents import create_tool_calling_agent, AgentExecutor  # type: ignore
from langchain.tools import BaseTool

logger = logging.getLogger("fact-forensic")

BASE_PROMPT = """
You are an agent that specialises in analysing articles,
images, social media posts or any other content for misinformation.
You use information from a multitude of tools to make a judgement on
the factual correctness and intent of the content in question.

Always summarise and explain how you got to your conclusion, quoting
any sources while highlighting how reputable these sources could be.
"""


def get_tools_description(tools: list[BaseTool]):
    return "\n".join(
        f"Tool: {tool.name}, Schema: {json.dumps(tool.args).replace('{', '{{').replace('}', '}}')}"  # type: ignore
        for tool in tools
    )


async def create_agent(coral_tools: list[BaseTool], agent_tools: list[BaseTool]):
    coral_tools_description = get_tools_description(coral_tools)
    agent_tools_description = get_tools_description(agent_tools)
    combined_tools = coral_tools + agent_tools
    prompt = ChatPromptTemplate.from_messages(  # type: ignore
        [
            (
                "system",
                f"""You are an agent interacting with the tools from Coral Server and having your own tools. Your task is to perform any instructions coming from any agent. 
            Follow these steps in order:
            1. Call wait_for_mentions from coral tools (timeoutMs: 30000) to receive mentions from other agents.
            2. When you receive a mention, keep the thread ID and the sender ID.
            3. Take 2 seconds to think about the content (instruction) of the message and check only from the list of your tools available for you to action.
            4. Check the tool schema and make a plan in steps for the task you want to perform.
            5. Only call the tools you need to perform for each step of the plan to complete the instruction in the content.
            6. Take 3 seconds and think about the content and see if you have executed the instruction to the best of your ability and the tools. Make this your response as "answer".
            7. Use `send_message` from coral tools to send a message in the same thread ID to the sender Id you received the mention from, with content: "answer".
            8. If any error occurs, use `send_message` to send a message in the same thread ID to the sender Id you received the mention from, with content: "error".
            9. Always respond back to the sender agent even if you have no answer or error.
            9. Wait for 2 seconds and repeat the process from step 1.

            As you prepare your text response, use the following prompt to guide
            your responses:

            {BASE_PROMPT}

            These are the list of coral tools: {coral_tools_description}
            These are the list of your tools: {agent_tools_description}""",
            ),
            ("placeholder", "{agent_scratchpad}"),
        ]
    )

    model = init_chat_model(
        model=os.getenv("MODEL_NAME", "gpt-4.1"),
        model_provider=os.getenv("MODEL_PROVIDER", "openai"),
        api_key=os.getenv("MODEL_API_KEY"),
        temperature=os.getenv("MODEL_TEMPERATURE", "0.1"),
        max_tokens=os.getenv("MODEL_MAX_TOKENS", "8000"),
        base_url=os.getenv("MODEL_BASE_URL", None),
    )
    agent = create_tool_calling_agent(model, combined_tools, prompt)  # type: ignore
    return AgentExecutor(
        agent=agent, tools=combined_tools, verbose=True, handle_parsing_errors=True
    )


async def main():
    base_url = os.getenv(
        "CORAL_SSE_URL",
        default="http://localhost:5555/devmode/exampleApplication/privkey/session1/sse",
    )
    agentID = os.getenv("CORAL_AGENT_ID", default="exampleAgentId")

    coral_params = {
        "agentId": agentID,
        "agentDescription": "An agent that analyses stories and determines if it is fake",
    }

    query_string = urllib.parse.urlencode(coral_params)

    CORAL_SERVER_URL = f"{base_url}?{query_string}"
    print(f"Connecting to Coral Server: {CORAL_SERVER_URL}")
    logger.info(f"Connecting to Coral Server: {CORAL_SERVER_URL}")

    timeout = float(os.getenv("TIMEOUT_MS", "300"))
    client = MultiServerMCPClient(
        connections={
            "coral": {
                "transport": "sse",
                "url": CORAL_SERVER_URL,
                "timeout": timeout,
                "sse_read_timeout": timeout,
            },
        }
    )

    coral_tools = await client.get_tools(server_name="coral")
    # agent_tools = await client.get_tools(server_name="firecrawl-mcp")

    logger.info(f"Coral tools count: {len(coral_tools)}")

    import tools as mytools

    AGENT_TOOLS: list[BaseTool] = [mytools.analyse_image, mytools.tavily_search_tool]

    agent_executor = await create_agent(coral_tools, AGENT_TOOLS)

    print("Hello from fact-forensic-agent!")

    while True:
        try:
            logger.info("Starting new agent invocation")
            await agent_executor.ainvoke({"agent_scratchpad": []})
            logger.info("Completed agent invocation, restarting loop")
            await asyncio.sleep(1)
        except Exception as e:
            logger.error(f"Error in agent loop: {str(e)}")
            logger.error(traceback.format_exc())
            await asyncio.sleep(5)


async def other_main():
    from dotenv import load_dotenv

    load_dotenv()

    print("Non coral main!")

    import tools as mytools

    AGENT_TOOLS: list[BaseTool] = [mytools.analyse_image, mytools.tavily_search_tool]

    agent_tools_description = get_tools_description(AGENT_TOOLS)

    model = init_chat_model(
        model=os.getenv("MODEL_NAME", "gpt-4.1"),
        model_provider=os.getenv("MODEL_PROVIDER", "openai"),
        api_key=os.getenv("MODEL_API_KEY"),
        temperature=os.getenv("MODEL_TEMPERATURE", "0.1"),
        max_tokens=os.getenv("MODEL_MAX_TOKENS", "8000"),
        base_url=os.getenv("MODEL_BASE_URL", None),
    )
    prompt = ChatPromptTemplate.from_messages(  # type: ignore
        [
            (
                "system",
                f"""
            {BASE_PROMPT}

            These are the list of your tools: {agent_tools_description}""",
            ),
            ("human", "{input}"),
            ("placeholder", "{agent_scratchpad}"),
        ]
    )

    agent = create_tool_calling_agent(model, AGENT_TOOLS, prompt)  # type: ignore
    executor = AgentExecutor(
        agent=agent, tools=AGENT_TOOLS, verbose=True, handle_parsing_errors=True
    ).with_config(
        {"configurable": {"thread_id": "abc123"}, "run_name": "Forensics Agent"}
    )

    # executor = create_react_agent(model, AGENT_TOOLS, prompt=prompt).with_config(  # type: ignore
    #     {"configurable": {"thread_id": "abc123"}, "run_name": "Forensics Agent"}
    # )

    async for step in executor.astream(  # type: ignore
        {
            "input": "Can you tell me what is in this image? https://ichef.bbci.co.uk/ace/standard/976/cpsprodpb/D89E/production/_123745455_deepfake2.png. Can you find any articles that quote a similar image on the web?"
        },
    ):
        step["messages"][-1].pretty_print()


if __name__ == "__main__":
    runtime = os.getenv("CORAL_ORCHESTRATION_RUNTIME", None)
    if runtime is not None:
        asyncio.run(main())
    else:
        asyncio.run(other_main())
