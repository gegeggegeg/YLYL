package com.example.peter.faceapi;

import java.io.Serializable;

public class YoutubevideoUnit implements Serializable{
    String videoID;
    int startTime;
    int endTime;

    public YoutubevideoUnit(String videoID, int startTime, int endTime) {
        this.videoID = videoID;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getVideoID() {
        return videoID;
    }

    public void setVideoID(String videoID) {
        this.videoID = videoID;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
}
