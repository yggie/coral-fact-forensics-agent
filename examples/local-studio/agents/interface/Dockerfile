FROM python:3.13-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install uv

COPY . .

RUN uv sync --no-dev

EXPOSE 5555

CMD ["uv", "run", "python", "main.py"]