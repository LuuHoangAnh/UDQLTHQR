package com.example.test;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        TextView tvUser, tvPass;
        Button btnLogin;

        tvUser = (TextView) findViewById(R.id.etUserName);
        tvPass = (TextView) findViewById(R.id.etPassword);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = String.valueOf(tvUser.getText());
                String pass = String.valueOf(tvPass.getText());
                
                if (user.equalsIgnoreCase("admin") && pass.equalsIgnoreCase("123456"))
                    Toast.makeText(LoginActivity.this, "Login thành công", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(LoginActivity.this, "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });

    }
}