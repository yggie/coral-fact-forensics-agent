# syntax=docker/dockerfile:1
FROM ghcr.io/astral-sh/uv:debian AS build

COPY . /build

WORKDIR /build

RUN uv sync --no-dev
RUN uv run pyinstaller main.py

FROM scratch
COPY --from=build /build/dist/main/main /bin/main

EXPOSE 3001 2375 5555
CMD ["/bin/main"]