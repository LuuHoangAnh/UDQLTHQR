package com.example.test;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        TextView tvUser, tvPass, tvFullName, tvPhone, tvAddress, tvDay, tvMonth, tvYear;
        CheckBox cbMale, cbFemale;
        Button btnRegister;

        tvUser = (TextView) findViewById(R.id.etUserName);
        tvPass = (TextView) findViewById(R.id.etPassword);
        tvFullName = (TextView) findViewById(R.id.etFullName);
        tvPhone = (TextView) findViewById(R.id.etPhone);
        tvAddress = (TextView) findViewById(R.id.etAddress);
        tvDay = (TextView) findViewById(R.id.etDay);
        tvMonth = (TextView) findViewById(R.id.etMonth);
        tvYear = (TextView) findViewById(R.id.etYear);

        btnRegister = (Button) findViewById(R.id.btnLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = String.valueOf(tvUser.getText());
                String pass = String.valueOf(tvPass.getText());

                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
            }
        });

    }
}