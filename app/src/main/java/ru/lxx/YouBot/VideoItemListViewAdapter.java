package ru.lxx.YouBot;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class VideoItemListViewAdapter extends BaseAdapter {
 
    private Activity activity;
    private ArrayList<VideoItem> data;
    private static LayoutInflater inflater=null;
 
    public VideoItemListViewAdapter(Activity a, ArrayList<VideoItem> d) {
        activity = a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return data.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_item, null);
 
        TextView title = (TextView)vi.findViewById(R.id.title);
        TextView desc = (TextView)vi.findViewById(R.id.desc);
        ImageView image = (ImageView)vi.findViewById(R.id.image);

        VideoItem item = new VideoItem();
        item = data.get(position);
 
        //Setting all values in listview
        title.setText(item.Title);
        desc.setText(item.sentByName);

        if (item.ImageBitmapBytes != null){
            image.setImageBitmap(BitmapFactory.decodeByteArray(item.ImageBitmapBytes, 0, item.ImageBitmapBytes.length));
        }
        else if (item.ImageUrl != null){
            new DownLoadImageTask(image,item).execute(item.ImageUrl);
        }

        return vi;
    }
}


/*
        AsyncTask enables proper and easy use of the UI thread. This class
        allows to perform background operations and publish results on the UI
        thread without having to manipulate threads and/or handlers.
     */

/*
    final AsyncTask<Params, Progress, Result>
        execute(Params... params)
            Executes the task with the specified parameters.
 */
class DownLoadImageTask extends AsyncTask<String,Void,Bitmap> {
    ImageView imageView;
    VideoItem item;

    public DownLoadImageTask(ImageView imageView, VideoItem item){
        this.imageView = imageView;
        this.item = item;
    }

    /*
        doInBackground(Params... params)
            Override this method to perform a computation on a background thread.
     */
    protected Bitmap doInBackground(String...urls){
        String urlOfImage = urls[0];
        Bitmap logo = null;
        try{
            InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
            logo = BitmapFactory.decodeStream(is);

        }catch(Exception e){ // Catch the download exception
            e.printStackTrace();
        }
        return logo;
    }

    /*
        onPostExecute(Result result)
            Runs on the UI thread after doInBackground(Params...).
     */
    protected void onPostExecute(Bitmap result){
        imageView.setImageBitmap(result);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
        byte bitmapBytes[] = byteStream.toByteArray();
        item.ImageBitmapBytes = bitmapBytes;
    }
}