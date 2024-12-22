# Youtube Data Streaming & Notification System

*This readme is incomplete*
## Table of contents

- [About](#about)
- [How to Run](#how-to-run)
- [Components](#components)
    1. [Fetching Data](#fetching-data)
    2. [Transforming the Stream of Data](#transforming-the-stream-of-data)
    3. [Consuming the Stream of Data](#consuming-the-stream-of-data)
- [Docker](#docker)
    1. [Dockerfile](#dockerfile)
    2. [Docker-Compose](#docker-compose)
- [Improvements](#improvements)
- [My Experience](#my-experience)
## About

This project uses a long-running Python script to fetch video data from a Youtube playlist. This data is sent int Kafka, and a Kafka-streams node then transforms the stream into changes in video data using persistent storage (Kafka state store). Then there is a Python discord bot that consumes the changes and sends them to a discord channel.

This system can be generalized to any form of data, not just Youtube video data. It is useful if you want to be notified of changed data and there is no existing notification system in place.

This project was inspired by a tutorial by confluent https://www.youtube.com/watch?v=jItIQ-UvFI4, but has been heavily modified. This project does not use confluent services and relies on docker to run components.
## How to run

**Required config:**
There are some secrets needed:
- A google API key for Youtube Data API (placed in .env in ./fetch-data)
Set up a discord bot and register it in your server:
- A discord token for your discord bot (placed in .env in ./bot)
- A discord channel id for your bot (placed in .env in ./bot)

**Optional config:**
Other configs can be can be changed in the docker-compose.yml file, some important configs:
- `DISCORD_CHANNEL_ID` specifies the channel id the bot will send data to.
- `FETCH_INTERVAL` specifies seconds in between the fetching of data.
- `YT_PLAYLIST_ID` specifies the playlist video data that will be fetched.

**Building and running:**
run `make build-all` to build the required source files into images
run `make docker-up` to spin all the containers locally
run `make docker-down` to remove all the containers locally
## Components

### Fetching data

Fetches data using a python script. I use a `while True:` loop and `time.sleep` to manage the interval between data fetching.
### Transforming the stream of data

The Kafka-streams API allows for an easy and scalable way to transforming stream data. I used this along with the Kafka processor API to write custom and stateful transforming logic.

The Kafka-streams API is written natively in Java, so I made a Maven project to handle data transformation. Since I didn't want to have to pass around schemas, I implemented serialization/deserialization using JSON. For the current implementation, the Kafka data is deserialized into a Java type with required fields (comments, likes, views, etc...). The previously received data is stored inside of a Kafka state store and changes are pushed into another Kafka topic to be consumed.

What I don't like about this implementation is that it can only handle a specific input from Kafka, so If I want to change the data source, new types/logic would have to be written for Kafka-streams. If there was a way to serialize/deserialize a list of freeform key:value types and detect changes with that instead, it would be much better.
### Consuming the stream of data

A simple discord bot was created to consume the output Kafka topic and send notification messages to the corresponding channel.
## Docker

### Dockerfile

### Docker-compose

## Improvement ideas

- [ ] Generalize data to not be specific for Youtube video data
- [ ] Use a more efficient encoding than JSON, maybe Protocol buffers or Avro (requires sharing a schema)
- [ ] How to not rely on channel ID for the discord bot?
## My Experience

Working with java & maven...

Working with python (VENV & PIP)...

Managing secrets... 