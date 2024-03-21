package com.example.engg41x_nav_app;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class CarActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<OBD> items;
    private OBDAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        listView = (ListView) findViewById(R.id.listView);
        items = new ArrayList<>();
        items.add(new OBD("1", "High", "Detailed information here."));
        items.add(new OBD("2", "Low", "Detailed information here."));
        items.add(new OBD("3", "High", "Detailed information here."));

        adapter = new OBDAdapter(this, items);


        View headerView = getLayoutInflater().inflate(R.layout.header_layout, listView, false);
        listView.addHeaderView(headerView, null, false);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OBD item = items.get(position);
                Toast.makeText(getApplicationContext(), item.getDesc(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
