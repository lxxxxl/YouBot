package ru.lxx.YouBot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity a = new MainActivity();
        a.ShouldUpdateListview = false;
        a.currentContext = context;
        a.InitYoutubeClient();
        a.restoreVideoItemListFromFile();
        a.telegramReceiveMessages();
    }
}
