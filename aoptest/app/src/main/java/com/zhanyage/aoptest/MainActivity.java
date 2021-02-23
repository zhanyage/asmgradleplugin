package com.zhanyage.aoptest;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnMainOne;
    private Button btnMainTwo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnMainOne = findViewById(R.id.btn_main_one);
        btnMainTwo = findViewById(R.id.btn_main_two);
        btnMainOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TestIgnoreFile.TestGc();
                Toast.makeText(MainActivity.this, "click one click one", Toast.LENGTH_LONG).show();
            }
        });
        btnMainTwo.setOnClickListener(this);
        int sum = sum(1, 2);
        Log.i("MainActivity", "sum result = " + sum);
    }

    private int sum(int i, int j) {
        return i + j;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_main_two:
                Toast.makeText(MainActivity.this, "click two click two", Toast.LENGTH_LONG).show();
                break;
        }
    }
}