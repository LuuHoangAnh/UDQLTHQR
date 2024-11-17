package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CustomerActivity extends AppCompatActivity {
    // Firebase Database reference
    DatabaseReference customerRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/customer");
    Button btnBack;
    ListView lvCustomer;

    List<String> customerList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer);

        // Bind UI components
        btnBack = findViewById(R.id.btnBack);
        lvCustomer = findViewById(R.id.lvCustomer);

        // Initialize adapter and set it to the ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, customerList);
        lvCustomer.setAdapter(adapter);

        // Load customers from Firebase
        loadCustomersFromFirebase();

        // Set back button listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CustomerActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    private void loadCustomersFromFirebase() {
        customerRepository.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerList.clear();  // Clear the current list
                for (DataSnapshot customerSnapshot : snapshot.getChildren()) {
                    String fullname = customerSnapshot.child("fullname").getValue(String.class);
                    String phone = customerSnapshot.child("phone").getValue(String.class);

                    if (fullname != null && phone != null) {
                        Customer customer = new Customer(fullname, phone);
                        customerList.add(customer.toString());
                    } else {
                        Log.w("FirebaseCustomer", "Incomplete customer data: " + customerSnapshot.getValue());
                    }
                }
                adapter.notifyDataSetChanged();  // Update the ListView
                setListViewHeightBasedOnItems(lvCustomer);  // Adjust ListView height
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseCustomer", "Error loading data: " + error.getMessage());
                Toast.makeText(CustomerActivity.this, "Error loading data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setListViewHeightBasedOnItems(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED
            );
            totalHeight += listItem.getMeasuredHeight();
        }

        // Add divider height between items
        int dividerHeight = listView.getDividerHeight() * (listAdapter.getCount() - 1);

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + dividerHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
