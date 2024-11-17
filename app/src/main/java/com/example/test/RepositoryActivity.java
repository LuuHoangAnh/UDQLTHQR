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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RepositoryActivity extends AppCompatActivity {
    DatabaseReference productsRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/products");
    Button btnBack;
    ListView lvProducts;

    List<String> productList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repository);

        btnBack = findViewById(R.id.btnBack);
        lvProducts = findViewById(R.id.lvProducts);

        productList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, productList);
        lvProducts.setAdapter(adapter);

        // Lấy danh sách sản phẩm từ Firebase
        loadProductsFromFirebase();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RepositoryActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    private void loadProductsFromFirebase() {
        productsRepository.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();  // Xóa danh sách hiện tại
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String name = productSnapshot.child("Name").getValue(String.class);
                    if (name == null) {
                        name = productSnapshot.child("ProductName").getValue(String.class); // Dự phòng nếu không tìm thấy
                    }
                    String price = productSnapshot.child("Price").getValue(String.class);
                    String productCode = productSnapshot.child("ProductCode").getValue(String.class);
                    int quantity = productSnapshot.child("Quantity").getValue(Integer.class);

                    Product product = new Product(name, price, productCode, quantity);
                    productList.add(product.toString());
                }

                adapter.notifyDataSetChanged();  // Cập nhật giao diện
                setListViewHeightBasedOnItems(lvProducts);  // Cập nhật chiều cao ListView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseProduct", "Error: " + error.getMessage());
                Toast.makeText(RepositoryActivity.this, "Lỗi khi tải dữ liệu từ Firebase.", Toast.LENGTH_SHORT).show();
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

        // Cộng thêm khoảng cách giữa các mục
        int dividerHeight = listView.getDividerHeight() * (listAdapter.getCount() - 1);

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + dividerHeight;
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
