package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    List<String> productKeys = new ArrayList<>();
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

        registerForContextMenu(lvProducts);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RepositoryActivity.this, MainActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menucontext, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int pos = info.position; //lay duoc vi tri can xoa
        int id = item.getItemId();

        if (id == R.id.edit) {
            editProduct(pos); // Gọi phương thức edit
            return true;
        } else if (id == R.id.delete) {
            String key = productKeys.get(pos); // Lấy key từ danh sách productKeys
            productsRepository.child(key).removeValue((error, ref) -> {
                if (error == null) {
                    Toast.makeText(RepositoryActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    productList.remove(pos);
                    productKeys.remove(pos); // Xóa key khỏi danh sách
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(RepositoryActivity.this, "Delete failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void loadProductsFromFirebase() {
        productsRepository.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();  // Xóa danh sách hiện tại
                productKeys.clear();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String key = productSnapshot.getKey(); // Lấy key thực tế từ Firebase
                    productKeys.add(key); // Lưu key vào danh sách

                    String productCode = productSnapshot.child("ProductCode").getValue(String.class);
                    String name = productSnapshot.child("Name").getValue(String.class);
                    String price = productSnapshot.child("Price").getValue(String.class);
                    Integer quantity = productSnapshot.child("Quantity").getValue(Integer.class);

                    if (productCode != null && name != null && price != null && quantity != null) {
                        // Định dạng chuỗi hiển thị sản phẩm
                        String productString = "Mã: " + productCode + ", Tên: " + name + ", Giá: " + price + ", SL: " + quantity;
                        productList.add(productString);
                    }
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

    public void editProduct(int pos) {
        View v = LayoutInflater.from(this).inflate(R.layout.productdialog, null);
        EditText code = v.findViewById(R.id.edtextmenu_code);
        EditText name = v.findViewById(R.id.edtextmenu_name);
        EditText price = v.findViewById(R.id.edtextmenu_price);
        EditText quantity = v.findViewById(R.id.edtextmenu_quantity);

        // Lấy dữ liệu hiện tại
        String productData = productList.get(pos);
        String currentCode = "";
        String currentName = "";
        String currentPrice = "";
        int currentQuantity = 0;

        try {
            String[] fields = productData.split(", ");
            currentCode = fields[0].split(": ")[1];
            currentName = fields[1].split(": ")[1];
            currentPrice = fields[2].split(": ")[1];
            currentQuantity = Integer.parseInt(fields[3].split(": ")[1]);
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing product data!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gán dữ liệu vào các ô nhập
        code.setText(currentCode);
        name.setText(currentName);
        price.setText(currentPrice);
        quantity.setText(String.valueOf(currentQuantity));

        // Lấy key từ danh sách productKeys
        String key = productKeys.get(pos);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setView(v);
        b.setPositiveButton("edit", (dialogInterface, i) -> {
            String newCode = code.getText().toString();
            String newName = name.getText().toString();
            String newPrice = price.getText().toString();
            int newQuantity;

            try {
                newQuantity = Integer.parseInt(quantity.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(RepositoryActivity.this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newName.isEmpty() && !newPrice.isEmpty() && !newCode.isEmpty()) {
                // Cập nhật trong Firebase
                productsRepository.child(key).child("Name").setValue(newName);
                productsRepository.child(key).child("Price").setValue(newPrice);
                productsRepository.child(key).child("ProductCode").setValue(newCode);
                productsRepository.child(key).child("Quantity").setValue(newQuantity);

                // Cập nhật trong danh sách
                String updatedProduct = "Mã: " + newCode + ", Tên: " + newName + ", Giá: " + newPrice + ", SL: " + newQuantity;
                productList.set(pos, updatedProduct);

                // Làm mới giao diện
                adapter.notifyDataSetChanged();
                Toast.makeText(RepositoryActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RepositoryActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("cancel", (dialogInterface, i) -> {
            // Không làm gì nếu nhấn hủy
        }).show();
    }
}
