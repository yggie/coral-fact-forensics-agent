import os
import base64
import requests
import tempfile

from PIL import Image
from transformers import pipeline  # type: ignore
from langchain_tavily import TavilySearch

from langchain_core.tools import tool  # type: ignore

# from openai import AsyncOpenAI
from mistralai import Mistral

tavily_search_tool = TavilySearch(max_results=5, topic="general", include_images=True)


@tool("analyse-image-tool", parse_docstring=True)
async def analyse_image(url: str):
    """Analyses the image in the provided URL

    Args:
        url: A URL to the image to analyse

    Returns:
        A dict summarising the results of the analysis

        {"description": "A scene of wild flowers extending to the horizon"}
    """

    img_data = requests.get(url).content

    client = Mistral(api_key=os.getenv("MISTRAL_API_KEY"))

    response = await client.chat.complete_async(
        model="pixtral-12b-2409",
        messages=[
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "What's in this image?"},
                    {
                        "type": "image_url",
                        "image_url": f"data:image/jpeg;base64,{base64.b64encode(img_data).decode("utf-8")}",
                    },
                ],
            }
        ],
    )

    deepfake_score = 0

    with tempfile.NamedTemporaryFile(delete=True) as f:
        f.write(img_data)

        img = Image.open(f.name)

        x1 = 0
        x2 = img.width
        y1 = 0
        y2 = img.height

        if img.width > img.height:
            diff = img.width - img.height
            x1 = round(diff / 2)
            x2 = img.width - x1
        elif img.height > img.width:
            diff = img.height - img.width
            y1 = round(diff / 2)
            y2 = img.height - y1

        img = (
            img.convert("RGB")
            .crop((x1, y1, x2, y2))
            .resize((224, 224), resample=Image.Resampling.LANCZOS)
        )
        img.save(f.name, format="JPEG")

        pipe = pipeline(
            "image-classification",
            model="prithivMLmods/Deep-Fake-Detector-v2-Model",
            device=0,
        )

        result = pipe(f.name)
        deepfake_score = next(r["score"] for r in result if r["label"] == "Deepfake")

    deepfake_judgement = "unlikely"
    if deepfake_score < 0.5:
        deepfake_judgement = "unlikely"
    elif deepfake_score < 0.9:
        deepfake_judgement = "unknown"
    else:
        deepfake_judgement = "likely"

    return {
        "description": str(response.choices[0].message.content),
        "deepfake-score": deepfake_judgement,
    }
