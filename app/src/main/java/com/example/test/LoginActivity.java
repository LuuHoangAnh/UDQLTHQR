package com.example.test;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");
    private NotificationManagerCompat notificationManagerCompat;

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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        notificationManagerCompat = NotificationManagerCompat.from(this);

        // Create notification channel for Android 8.0 and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Channel 1";
            String description = "Channel for battery notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(com.example.test.Notification.CHANNEL_1_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        final int[] count = {0};
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

                                    String title = "Login Acount!";
                                    String message = "Người dùng " + getName +" đang đăng nhập với vai trò "+getRole+"!";
                                    sendOnChannel1(title, message);

                                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("nameofuser", getName);
                                    editor.putString("roleofuser", getRole);
                                    editor.apply();  // Lưu dữ liệu

                                    i.putExtra("nameofuser", getName);
                                    i.putExtra("roleofuser", getRole);
                                    startActivity(i);
                                }
                                else {
                                    count[0]++;
                                    Toast.makeText(LoginActivity.this, "Sai mật khẩu!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                count[0]++;
                                Toast.makeText(LoginActivity.this, "Sai tên đăng nhập!", Toast.LENGTH_SHORT).show();
                            }
                            if (count[0] == 4)
                            {
                                String title = "Login Acount!";
                                String message = "Bạn đã nhập sai quá nhiều lần.";
                                sendOnChannel1(title, message);
                                System.exit(0);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        });

    }

    private void sendOnChannel1(String title, String message) {
        Notification notification = new NotificationCompat.Builder(this, com.example.test.Notification.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();
        int notificationId = 1;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManagerCompat.notify(notificationId, notification);
        }
    }
}