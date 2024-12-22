#!/usr/bin/env python

import json
import logging
from pprint import pformat
import time
import requests
from dotenv import load_dotenv
import os
from confluent_kafka import Producer


def fetch_playlist_items_page(page_token=None):
    response = requests.get(
        "https://www.googleapis.com/youtube/v3/playlistItems",
        params={
            "key": os.getenv("GOOGLE_API_KEY"),
            "playlistId": os.getenv("YT_PLAYLIST_ID"),
            "part": "contentDetails",
            "maxResults": 50,
            "pageToken": page_token,
        },
    )

    payload = json.loads(response.text)

    return payload


def fetch_playlist_items(pageToken=None):
    payload = fetch_playlist_items_page(pageToken)

    yield from payload["items"]

    next_page_token = payload.get("nextPageToken")

    if next_page_token is not None:
        yield from fetch_playlist_items(next_page_token)


def fetch_video_page(video_id, page_token=None):
    response = requests.get(
        "https://www.googleapis.com/youtube/v3/videos",
        params={
            "key": os.getenv("GOOGLE_API_KEY"),
            "id": video_id,
            "part": "snippet,statistics",
            "maxResults": 50,
            "pageToken": page_token,
        },
    )

    payload = json.loads(response.text)

    return payload


def fetch_video(video_id, pageToken=None):
    payload = fetch_video_page(video_id, pageToken)

    yield from payload["items"]

    next_page_token = payload.get("nextPageToken")

    if next_page_token is not None:
        yield from fetch_playlist_items(next_page_token)


def transform_video(video):
    return {
        "id": video["id"],
        "title": video["snippet"]["title"],
        "views": int(video["statistics"].get("viewCount", 0)),
        "likes": int(video["statistics"].get("likeCount", 0)),
        "comments": int(video["statistics"].get("commentCount", 0)),
    }


def main():
    load_dotenv(override=False)

    logging.info("START")

    producer = Producer({"bootstrap.servers": os.getenv("KAFKA_BOOTSTRAP_SERVERS")})

    while True:
        for video_item in fetch_playlist_items():
            video_id = video_item["contentDetails"]["videoId"]
            for video in fetch_video(video_id):
                t_video = transform_video(video)
                logging.info(pformat(t_video))
                producer.produce(
                    topic="youtube", key=t_video["id"], value=json.dumps(t_video)
                )

        producer.flush(timeout=1)
        time.sleep(int(os.getenv("FETCH_INTERVAL")))


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
