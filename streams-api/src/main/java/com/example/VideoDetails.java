package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoDetails {
    public int likes;
    public int views;
    public int comments;
    public String title;
}