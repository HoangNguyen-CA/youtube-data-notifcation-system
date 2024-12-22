import json
import os
import discord
from confluent_kafka import Consumer, KafkaError
import threading
from dotenv import load_dotenv

load_dotenv(override=False)

# Discord bot token
DISCORD_TOKEN = os.getenv("DISCORD_TOKEN")

# Kafka consumer configuration
KAFKA_CONFIG = {
    "bootstrap.servers": os.getenv("KAFKA_BOOTSTRAP_SERVERS"),
    "group.id": "discord-bot",
}

# Kafka topic to listen to
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC")

# Discord channel ID where messages will be posted
DISCORD_CHANNEL_ID = int(os.getenv("DISCORD_CHANNEL_ID"))

# Initialize the Discord client
intents = discord.Intents.default()
intents.messages = True  # Enables message-related events like on_message
client = discord.Client(intents=intents)

# Initialize the Kafka consumer
consumer = Consumer(KAFKA_CONFIG)


# Set up a listener for Kafka messages
def consume_messages():
    consumer.subscribe([KAFKA_TOPIC])

    while True:
        msg = consumer.poll(1.0)  # Poll every second

        if msg is None:
            continue  # No message received
        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                print(f"End of partition {msg.partition} reached")
            else:
                print(f"Error: {msg.error()}")
        else:
            # If message is valid, process it
            print(f"Message received: {msg.value().decode('utf-8')}")
            post_to_discord(transform_message(msg.value().decode("utf-8")))


def format_change(label, old_value, new_value):
    res = f"{label}: {old_value} -> {new_value},"
    if (new_value - old_value) > 0:
        res += f" +{new_value - old_value}"
    elif (new_value - old_value) < 0:
        res += f" {new_value - old_value}"
    res += "\n"
    return res


def transform_message(str_message):
    message = json.loads(str_message)

    result = f"Stats changed for video: {message.get('title')}\n"

    if message.get("oldLikes") is not None:
        result += format_change("likes", message["oldLikes"], message["likes"])
    if message.get("oldViews") is not None:
        result += format_change("views", message["oldViews"], message["views"])
    if message.get("oldComments") is not None:
        result += format_change("comments", message["oldComments"], message["comments"])
    return result


# Function to post a message to Discord
def post_to_discord(message):
    channel = client.get_channel(DISCORD_CHANNEL_ID)
    print(f"Posting message to Discord: {message}")
    if channel:
        client.loop.create_task(channel.send(message))


# Discord event when the bot is ready
@client.event
async def on_ready():
    print(f"Logged in as {client.user}")

    # Start the Kafka consumer in a separate thread
    kafka_thread = threading.Thread(target=consume_messages)
    kafka_thread.daemon = True
    kafka_thread.start()


# Start the bot
client.run(DISCORD_TOKEN)
