package com.example.test;

import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
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
    List<String> customerKeys = new ArrayList<>();
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

        registerForContextMenu(lvCustomer);

        // Set back button listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CustomerActivity.this, MainActivity.class);
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
        if (id == R.id.delete) {
            // Lấy key từ danh sách customerKeys
            String key = customerKeys.get(pos);

            // Xóa dữ liệu trong Firebase
            customerRepository.child(key).removeValue((error, ref) -> {
                if (error == null) {
                    Toast.makeText(CustomerActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CustomerActivity.this, "Delete failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        } else if (id == R.id.edit) {
            edit(pos);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void loadCustomersFromFirebase() {
        customerRepository.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerList.clear();  // Clear the current list
                for (DataSnapshot customerSnapshot : snapshot.getChildren()) {
                    String key = customerSnapshot.getKey(); // Lấy key từ Firebase
                    String fullname = customerSnapshot.child("fullname").getValue(String.class);
                    String phone = customerSnapshot.child("phone").getValue(String.class);

                    if (fullname != null && phone != null) {
                        Customer customer = new Customer(fullname, phone);
                        customerKeys.add(key); // Lưu key vào danh sách
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

    public void edit(int pos) {
        View v = LayoutInflater.from(this).inflate(R.layout.customerdialog, null);
        EditText name = v.findViewById(R.id.edtextmenu_name);
        EditText phone = v.findViewById(R.id.edtextmenu_phone);

        // Lấy chuỗi khách hàng từ danh sách
        String customerData = customerList.get(pos);

        // Kiểm tra định dạng chuỗi và tách thông tin
        String currentName = "";
        String currentPhone = "";
        if (customerData.contains(":") && customerData.contains(",")) {
            try {
                // Tách chuỗi để lấy fullname và phone
                String[] parts = customerData.split(", ");
                currentName = parts[0].split(": ")[1]; // Lấy phần sau "Họ và tên: "
                currentPhone = parts[1].split(": ")[1]; // Lấy phần sau "SDT: "
            } catch (Exception e) {
                Toast.makeText(this, "Error parsing customer data!", Toast.LENGTH_SHORT).show();
                return; // Ngừng xử lý nếu dữ liệu không hợp lệ
            }
        }

        // Hiển thị thông tin hiện tại
        name.setText(currentName);
        phone.setText(currentPhone);

        // Lấy key của khách hàng cần sửa
        String key = customerKeys.get(pos);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setView(v);
        b.setPositiveButton("edit", (dialogInterface, i) -> {
            String newName = name.getText().toString();
            String newPhone = phone.getText().toString();

            if (!newName.isEmpty() && !newPhone.isEmpty()) {
                // Cập nhật dữ liệu trong Firebase
                customerRepository.child(key).child("fullname").setValue(newName);
                customerRepository.child(key).child("phone").setValue(newPhone);

                // Cập nhật chuỗi trong danh sách
                customerList.set(pos, "Họ và tên: " + newName + ", SDT: " + newPhone);

                // Làm mới ListView
                adapter.notifyDataSetChanged();

                Toast.makeText(CustomerActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CustomerActivity.this, "Name or phone cannot be empty", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("cancel", (dialogInterface, i) -> {
            // Không làm gì khi hủy
        }).show();
    }
}
