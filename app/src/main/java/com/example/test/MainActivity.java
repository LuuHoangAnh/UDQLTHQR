package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    CardView cardSell, cardRestock, cardCreateAccount, cardReports, cardCustomer, cardExit;
    TextView tvUserInfo, tvUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvUserInfo = (TextView) findViewById(R.id.tvUserInfo);
        tvUserRole = (TextView) findViewById(R.id.tvUserRole);
        cardSell = (CardView) findViewById(R.id.cardSell);
        cardRestock = (CardView) findViewById(R.id.cardRestock);
        cardCreateAccount = (CardView) findViewById(R.id.cardCreateAccount);
        cardReports = (CardView) findViewById(R.id.cardReports);
        cardCustomer = (CardView) findViewById(R.id.cardCustomer);
        cardExit = (CardView) findViewById(R.id.cardExit);

        String name = getIntent().getExtras().getString("nameofuser");
        String role = getIntent().getExtras().getString("roleofuser");

        tvUserInfo.setText(name);
        tvUserRole.setText("Chức vụ: "+role);

        //chức năng bán hàng
        cardSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, SellActivity.class);
                startActivity(i);
            }
        });

        //chức năng nhập hàng (dành cho quản lý)
        cardRestock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("manager"))
                {
                    Intent i = new Intent(MainActivity.this, SellActivity.class);
                    startActivity(i);
                }
                else
                    Toast.makeText(MainActivity.this, "Bạn không có quyền sử dụng chức năng này!", Toast.LENGTH_SHORT).show();
            }
        });

        //chức năng tạo tài khoản (dành cho quản lý)
        cardCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("manager"))
                {
                    Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(i);
                }
                else
                    Toast.makeText(MainActivity.this, "Bạn không có quyền sử dụng chức năng này!", Toast.LENGTH_SHORT).show();
            }
        });

        //chức năng thống kê báo cáo
        //chức năng thông tin khách hàng

        //chức năng logout
        cardExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
                Toast.makeText(MainActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}