package ru.lxx.YouBot;

import android.graphics.Bitmap;

import java.io.Serializable;

public class VideoItem implements Serializable {
    public String videoId;
    public String URL;
    public String Title;
    public String ImageUrl;
    public int sentById;
    public String sentByName;
    public byte[] ImageBitmapBytes;
}
