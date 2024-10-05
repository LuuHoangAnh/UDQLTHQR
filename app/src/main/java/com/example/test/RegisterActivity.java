package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RegisterActivity extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        TextView tvUser, tvPass, tvFullName, tvPhone, tvRole;
        Button btnRegister, btnBack;

        tvUser = (TextView) findViewById(R.id.etUserName);
        tvPass = (TextView) findViewById(R.id.etPassword);
        tvFullName = (TextView) findViewById(R.id.etFullName);
        tvPhone = (TextView) findViewById(R.id.etPhone);
        tvRole = (TextView) findViewById(R.id.etRole);

        btnRegister = (Button) findViewById(R.id.btnLogin);
        btnBack = (Button) findViewById(R.id.btnBack);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //lấy thông tin từ EditText chuyển thành chuỗi
                String fullname = String.valueOf(tvFullName.getText().toString());
                String username = String.valueOf(tvUser.getText().toString());
                String pass = String.valueOf(tvPass.getText().toString());
                String phone = String.valueOf(tvPhone.getText().toString());
                String role = String.valueOf(tvRole.getText().toString());

                //kiểm tra xem đã nhập đủ hết các thông tin chưa
                if (fullname.isEmpty() || username.isEmpty() || pass.isEmpty() || phone.isEmpty() || role.isEmpty())
                {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                }
                else {  //gửi dữ liệu lên Realtime Database
                    databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //kiểm tra xem username đã được đăng ký chưa
                            if (snapshot.hasChild(username)){
                                Toast.makeText(RegisterActivity.this, "Username đã tồn tại", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                databaseReference.child("users").child(username).child("fullname").setValue(fullname);
                                databaseReference.child("users").child(username).child("password").setValue(pass);
                                databaseReference.child("users").child(username).child("phone").setValue(phone);
                                databaseReference.child("users").child(username).child("role").setValue(role);

                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }
}