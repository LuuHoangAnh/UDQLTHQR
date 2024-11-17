package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        myRef.setValue("Hello, World!");

        TextView tvUser, tvPass;
        Button btnLogin;

        tvUser = (TextView) findViewById(R.id.etUserName);
        tvPass = (TextView) findViewById(R.id.etPassword);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = String.valueOf(tvUser.getText().toString());
                String pass = String.valueOf(tvPass.getText().toString());

                if (user.isEmpty() || pass.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
                }
                else{
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //kiểm tra tài khoản
                            if(snapshot.hasChild(user)){
                                String getPassword = snapshot.child(user).child("password").getValue(String.class);
                                if (getPassword.equals(pass)) {
                                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    String getName = snapshot.child(user).child("fullname").getValue().toString();
                                    String getRole = snapshot.child(user).child("role").getValue().toString();

                                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("nameofuser", getName);
                                    editor.putString("roleofuser", getRole);
                                    editor.apply();  // Lưu dữ liệu

                                    i.putExtra("nameofuser", getName);
                                    i.putExtra("roleofuser", getRole);
                                    startActivity(i);
                                }
                                else
                                    Toast.makeText(LoginActivity.this, "Sai mật khẩu!", Toast.LENGTH_SHORT).show();
                            }
                            else
                                Toast.makeText(LoginActivity.this, "Sai tên đăng nhập!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        });

    }
}