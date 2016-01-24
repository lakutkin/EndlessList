package com.example.my.myapplication;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView textView1;
        private final TextView textView2;

        public ViewHolder(View itemView) {
            super(itemView);
            textView1 = (TextView) itemView.findViewById(R.id.textView);
            textView2 = (TextView) itemView.findViewById(R.id.textView2);
        }
    }

    public class ViewHolder2 extends RecyclerView.ViewHolder{
        public ViewHolder2(View itemView) {
            super(itemView);
        }
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        public static final int VIEW_TYPE_HEADER = 1;
        public static final int VIEW_TYPE_ITEM = 0;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate;
            switch (viewType){
                case VIEW_TYPE_HEADER:
                    inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.l_header, parent, false);
                    return new ViewHolder2(inflate);
                case VIEW_TYPE_ITEM:
                    inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.l_item, parent, false);
                    return new ViewHolder(inflate);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_HEADER:
                    return;
                case VIEW_TYPE_ITEM:
                    ((ViewHolder)holder).textView1.setText(String.valueOf(position));
                    ((ViewHolder)holder).textView2.setText(String.valueOf(100-position));
                    return;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return (position == 0? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM);
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
