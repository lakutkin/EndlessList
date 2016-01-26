package com.example.my.myapplication;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.example.my.SaneAsyncLoader;

public class Main2Activity extends Activity
        implements LoaderManager.LoaderCallbacks<Integer> {

    private Handler handler = new Handler(Looper.getMainLooper());

    private int i = 0;

    @Override
    public Loader<Integer> onCreateLoader(int id, Bundle args) {
        return new SaneAsyncLoader<Integer>(this) {
            @Override
            public Integer loadInBackground() {
                return i++;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Integer> loader, Integer data) {
        Log.d("LoaderTest", "LoadFinished: " + data);
    }

    @Override
    public void onLoaderReset(Loader<Integer> loader) {

    }

    private class RestartRunnable implements Runnable {
        private Bundle args;
        @Override
        public void run() {
            getLoaderManager().restartLoader(0, args, Main2Activity.this);
        }
    }

    private RestartRunnable runnable = new RestartRunnable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);



    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i< 50; i++){
                    handler.removeCallbacks(runnable);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(runnable, 500);
                }
            }
        });
        thread.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
