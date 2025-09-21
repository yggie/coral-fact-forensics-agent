# Coral Fact Forensics Agent 🕵️‍♀️

An advanced LLM agent that specializes in determining if a claim, news article, or social media post contains misleading information, with a strong focus on detecting heavily edited images and deepfakes.

-----

## 🚀 Overview

In the age of digital information, discerning fact from fiction is more challenging than ever. **Coral** is a powerful AI agent designed for developers to integrate state-of-the-art fact-checking and digital media forensics into their applications. It goes beyond simple text analysis by incorporating sophisticated checks for image manipulation and AI-generated video, providing a comprehensive verdict on the authenticity of a piece of content.

Whether you're building a news aggregation platform, a social media monitoring tool, or a research application, Coral provides the forensic capabilities you need.

-----

## 🛠️ Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

  * Python 3.12+
  * **uv** (see detailed instructions on the [official site](https://docs.astral.sh/uv/))
  * An API key from your chosen LLM provider (e.g., OpenAI, Google AI).
  * API keys for [Mistral](https://mistral.ai/) (for vision capabilities) and [Tavily](https://www.tavily.com/) (internet search)

### Installation

1.  **Clone the repository:**

    ```sh
    git clone https://github.com/yggie/coral-fact-forensics-agent.git
    cd coral-fact-forensics-agent
    ```

2.  **Create and activate a virtual environment using uv:**

    ```sh
    # Create the virtual environment
    uv venv
    ```

3.  **Install dependencies using uv:**

    ```sh
    uv sync
    ```

4.  **Configure your environment:**
    Create a `.env` file in the project root by copying the example file.

    ```sh
    cp .env.example .env
    ```

    Now, edit the `.env` file and add your API keys and any other necessary configuration.

    ```env
    # .env
    MODEL_API_KEY="model-api-key"
    MISTRAL_API_KEY="mistral-api-key"
    TAVILY_API_KEY="tavily-api-key"
    ```

-----

## 💻 Usage

You can find the agent on the Coral marketplace (TBC)

## 🤝 Contributing

Contributions are welcome\! If you'd like to help improve the Coral agent, please fork the repository and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

Please see `CONTRIBUTING.md` for more details.

-----

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](/LICENSE) file for details.