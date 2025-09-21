# Coral Fact Forensics Agent 🕵️‍♀️

An advanced LLM agent that specializes in determining if a claim, news article, or social media post contains misleading information, with a strong focus on detecting heavily edited images and deepfakes.

-----

## 🚀 Overview

In the age of digital information, discerning fact from fiction is more challenging than ever. **Coral** is a powerful AI agent designed for developers to integrate state-of-the-art fact-checking and digital media forensics into their applications. It goes beyond simple text analysis by incorporating sophisticated checks for image manipulation and AI-generated video, providing a comprehensive verdict on the authenticity of a piece of content.

Whether you're building a news aggregation platform, a social media monitoring tool, or a research application, Coral provides the forensic capabilities you need.

-----

## Architecture

[![](https://mermaid.ink/img/pako:eNp9Uk2PmzAQ_SvWnLYSiQIJIfi23WhPWSlqox5a9uDFE2ItYGRM2jTKf6-NIYU0XU7M8_vgeThDKjkCBabSg9CY6kbh5A01S0pinkzJpiKpVCx_SHPZ8E8_nuzwmpSOUKM6ihQdpWDqHXWVsxQf7AGqjk62OdN7qYpXIkrH7Q1cwp6l9rzPeDYjeZYKy1qkNXnMsNT3I13MTdrXdmqzOuOxsgNvtC7lvyKOWO3ZO3K013RVrTuYrFt8rB9W_Ilvfb3dQShOtkzp0z-tNDuK_HS137WjaWQX1Jobn7GiELW2--klL24m30QtZHlfk-dFpeRR8EH_zeaFbDvwr8rpbtdLv5DJhGzoYAcDogN6zui2HWsE9bzxBTuiuw362TJ295y68h8wBlXvssCDTAkOVKsGPShQFcyOcLb6BPQBC0yAmleOe9bkOoGkvBhZxcrvUha90mw5O4AxN_09aCrONK4FyxQrrqj5o813PMmm1ED9OFy0LkDP8AvoIl5Oo9APwygOlsEiCDw4AQ2i1XQeRfPQn8WG4QcXD363sbNpFK38aLWM_dVyMfPn0eUPPVJOgQ?type=png)](https://mermaid.live/edit#pako:eNp9Uk2PmzAQ_SvWnLYSiQIJIfi23WhPWSlqox5a9uDFE2ItYGRM2jTKf6-NIYU0XU7M8_vgeThDKjkCBabSg9CY6kbh5A01S0pinkzJpiKpVCx_SHPZ8E8_nuzwmpSOUKM6ihQdpWDqHXWVsxQf7AGqjk62OdN7qYpXIkrH7Q1cwp6l9rzPeDYjeZYKy1qkNXnMsNT3I13MTdrXdmqzOuOxsgNvtC7lvyKOWO3ZO3K013RVrTuYrFt8rB9W_Ilvfb3dQShOtkzp0z-tNDuK_HS137WjaWQX1Jobn7GiELW2--klL24m30QtZHlfk-dFpeRR8EH_zeaFbDvwr8rpbtdLv5DJhGzoYAcDogN6zui2HWsE9bzxBTuiuw362TJ295y68h8wBlXvssCDTAkOVKsGPShQFcyOcLb6BPQBC0yAmleOe9bkOoGkvBhZxcrvUha90mw5O4AxN_09aCrONK4FyxQrrqj5o813PMmm1ED9OFy0LkDP8AvoIl5Oo9APwygOlsEiCDw4AQ2i1XQeRfPQn8WG4QcXD363sbNpFK38aLWM_dVyMfPn0eUPPVJOgQ)

If the above image doesn't show, visit [here](https://mermaid.live/edit#pako:eNp9Uk2PmzAQ_SvWnLYSiQIJIfi23WhPWSlqox5a9uDFE2ItYGRM2jTKf6-NIYU0XU7M8_vgeThDKjkCBabSg9CY6kbh5A01S0pinkzJpiKpVCx_SHPZ8E8_nuzwmpSOUKM6ihQdpWDqHXWVsxQf7AGqjk62OdN7qYpXIkrH7Q1cwp6l9rzPeDYjeZYKy1qkNXnMsNT3I13MTdrXdmqzOuOxsgNvtC7lvyKOWO3ZO3K013RVrTuYrFt8rB9W_Ilvfb3dQShOtkzp0z-tNDuK_HS137WjaWQX1Jobn7GiELW2--klL24m30QtZHlfk-dFpeRR8EH_zeaFbDvwr8rpbtdLv5DJhGzoYAcDogN6zui2HWsE9bzxBTuiuw362TJ295y68h8wBlXvssCDTAkOVKsGPShQFcyOcLb6BPQBC0yAmleOe9bkOoGkvBhZxcrvUha90mw5O4AxN_09aCrONK4FyxQrrqj5o813PMmm1ED9OFy0LkDP8AvoIl5Oo9APwygOlsEiCDw4AQ2i1XQeRfPQn8WG4QcXD363sbNpFK38aLWM_dVyMfPn0eUPPVJOgQ)

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

## Examples

You can find working examples in the [examples](/examples) directory

## 🤝 Contributing

Contributions are welcome\! If you'd like to help improve the Coral agent, please fork the repository and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

Please see `CONTRIBUTING.md` for more details.

-----

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](/LICENSE) file for details.