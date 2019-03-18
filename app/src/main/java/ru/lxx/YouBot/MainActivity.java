package ru.lxx.YouBot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;

import com.google.api.services.youtube.YouTube;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


// http://codesfor.in/android-swipe-to-delete-listview/
// https://github.com/pengrad/java-telegram-bot-api
// https://core.telegram.org/bots/api

public class MainActivity extends AppCompatActivity {

    ListView youVideosListView;
    VideoItemListViewAdapter videoItemListViewAdapter;
    ArrayList<VideoItem> videoItemArrayList = new ArrayList<>();
    TelegramBot telegramBot = new TelegramBot("TELEGRAM_API_KEY");
    int updateId = 0;
    YoutubeHelper youtubeHelper = new YoutubeHelper();
    YouTube youtube;

    public boolean ShouldUpdateListview = true;
    public Context currentContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init Youtube client
        InitYoutubeClient();

        // restore previously saved videos
        restoreVideoItemListFromFile();

        // get new urls from Telegram
        //telegramReceiveMessages();
        // do it in onResume to prevent duplicates






        //setting array adapter to listview
        videoItemListViewAdapter = new VideoItemListViewAdapter(this, videoItemArrayList);
        youVideosListView = (ListView) findViewById(R.id.youUrlsListView);
        youVideosListView.setAdapter(videoItemListViewAdapter);

        // set onclick listener
        youVideosListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(videoItemArrayList.get(position).URL)));

                // send inform that video is viewing
                SendMessage message = new SendMessage(videoItemArrayList.get(position).sentById,"Watching "+videoItemArrayList.get(position).URL );
                telegramBot.execute(message, new Callback<SendMessage, SendResponse>() {
                    @Override
                    public void onResponse(SendMessage request, SendResponse response) {

                    }

                    @Override
                    public void onFailure(SendMessage request, IOException e) {

                    }
                });
            }
        });

        // set swipe-to-remove listener
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        youVideosListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {

                                    videoItemArrayList.remove(position);
                                    videoItemListViewAdapter.notifyDataSetChanged();

                                }

                            }
                        });
        youVideosListView.setOnTouchListener(touchListener);


    }

    @Override
    protected void onResume() {
        telegramReceiveMessages();
        super.onResume();
    }

    @Override
    protected void onPause() {
        saveVideoItemListToFile();
        super.onPause();
    }

    private void telegramTest() {
        GetMe me = new GetMe();
        telegramBot.execute(me, new Callback<GetMe, GetMeResponse>() {
            @Override
            public void onResponse(GetMe request, GetMeResponse response) {

            }

            @Override
            public void onFailure(GetMe request, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void telegramReceiveMessages() {

        GetUpdates getUpdates = new GetUpdates().limit(100).offset(updateId).timeout(0);

        telegramBot.execute(getUpdates, new Callback<GetUpdates, GetUpdatesResponse>() {
            @Override
            public void onResponse(GetUpdates request, GetUpdatesResponse response) {
                List<Update> updates = response.updates();
                String videoIds = "";

                // check if we need to get names for previous videos
                for (VideoItem item :videoItemArrayList) {
                    if (item.Title.equals("Loading...")) {
                        if (videoIds.length() != 0)
                            videoIds += ",";
                        videoIds += item.videoId;
                    }
                }

                // parse telegramm messages
                if (updates != null && updates.size() > 0) {
                    for (Update update : updates) {
                        Message message = update.message();
                        updateId = update.updateId() + 1;
                        VideoItem item = new VideoItem();
                        item.videoId = youtubeHelper.extractVideoIdFromUrl(message.text());
                        if ((item.videoId != null) && (!item.videoId.isEmpty())) {
                            item.URL = message.text();
                            item.Title = "Loading...";
                            //item.Title =  message.text();
                            item.sentById = message.from().id();
                            item.sentByName = message.from().firstName();
                            videoItemArrayList.add(item);
                            if (videoIds.length() != 0)
                                videoIds += ",";
                            videoIds += item.videoId;
                        }
                    }

                    // get video names
                    try {
                        YouTube.Videos.List videosListByIdRequest = youtube.videos().list("snippet");
                        videosListByIdRequest.setKey("YOUTUBE_API_KEY");
                        videosListByIdRequest.setId(videoIds);
                        VideoListResponse videosListByIdResponse = videosListByIdRequest.execute();
                        for (Video video : videosListByIdResponse.getItems()){
                            for (VideoItem item: videoItemArrayList){
                                if (Objects.equals(item.videoId, video.getId()) && (Objects.equals(item.Title, "Loading..."))){
                                    item.Title = video.getSnippet().getTitle();
                                    item.ImageUrl = video.getSnippet().getThumbnails().getDefault().getUrl();
                                    break;
                                }
                            }
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    telegramReceiveMessages();
                }

                // update data view
                if (ShouldUpdateListview)
                    updateYouVideosListView();
                else
                    saveVideoItemListToFile();

            }

            @Override
            public void onFailure(GetUpdates request, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void InitYoutubeClient(){
        try {
            youtube = new YouTube.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                }
            }).build();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveVideoItemListToFile() {
        try {
            FileOutputStream fos = currentContext.openFileOutput("videos", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(videoItemArrayList);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restoreVideoItemListFromFile() {
        try {
            FileInputStream fis = currentContext.openFileInput("videos");
            ObjectInputStream ois = new ObjectInputStream(fis);
            videoItemArrayList = (ArrayList<VideoItem>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateYouVideosListView() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoItemListViewAdapter.notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
