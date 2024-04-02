package com.example.engg41x_nav_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
        items.add(new OBD("P0301", "High", "Cylinder 1 Misfire Detected"));
        items.add(new OBD("P0300", "Medium", "Random/Multiple Cylinder Misfire Detected"));
        items.add(new OBD("P0138", "Medium", "O2 Sensor Circuit High Voltage (Bank 1, Sensor 2)"));
        items.add(new OBD("P0440", "Low", "Evaporative Emission System"));
        items.add(new OBD("P0455", "Low", "Evaporative Emission System Leak Detected (gross leak)"));
        items.add(new OBD("P0141", "Low", "O2 Sensor Heater Circuit Malfunction (Bank 1, Sensor 2)"));


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

        Button mechButton = findViewById(R.id.mechanic);
        mechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCallMechanicDialog();
            }
        });
    }

    private void showCallMechanicDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CarActivity.this);
        builder.setTitle("Call Mechanic");
        builder.setMessage("Would you like to call your mechanic at (519)-635-6042?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String number = "15196356042";
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(CarActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
