package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class GetCustomerInfoActivity extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");
    Button btnConfirmTransaction, btnBack;
    EditText etName, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sell2);

        btnConfirmTransaction = (Button) findViewById(R.id.btnConfirmTransaction);
        btnBack = (Button) findViewById(R.id.btnBack);
        etName = (EditText) findViewById(R.id.etCustomerName);
        etPhone = (EditText) findViewById(R.id.etCustomerPhone);

        btnConfirmTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = String.valueOf(etName.getText().toString());
                String phone  = String.valueOf(etPhone.getText().toString());

                Random random = new Random();
                int number = random.nextInt(999999) + 1;

                // Định dạng số để đảm bảo nó có 6 chữ số
                String id = String.format("%06d", number);

                if (name.isEmpty() || phone.isEmpty())
                    Toast.makeText(GetCustomerInfoActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                else {  // gửi dữ liệu lên RealTime Database
                    databaseReference.child("customer").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            if (snapshot.hasChild(id))
                                Toast.makeText(GetCustomerInfoActivity.this, "ID đã tồn tại", Toast.LENGTH_SHORT).show();
                            else{
                                databaseReference.child("customer").child("ID: "+id).child("fullname").setValue(name);
                                databaseReference.child("customer").child("ID: "+id).child("phone").setValue(phone);

                                Toast.makeText(GetCustomerInfoActivity.this, "Lấy thông tin thành công", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(GetCustomerInfoActivity.this, MainActivity.class);
                                startActivity(i);
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
                Intent i = new Intent(GetCustomerInfoActivity.this, SellActivity.class);
                startActivity(i);
            }
        });
    }
}
