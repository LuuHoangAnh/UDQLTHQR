package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BuyActivity extends AppCompatActivity {
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");
    DatabaseReference productsRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/products");
    DatabaseReference receiptRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/receipt");

    ImageButton btnScanQR;
    EditText etProductCode, etProductName, etPrice, etQuantity;
    Button btnDone, btnBack, btnAdd;
    ListView lvProducts;

    List<String> productList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    private int defaultListViewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buy);

        etProductCode = (EditText) findViewById(R.id.etProductCode);
        etProductName = (EditText) findViewById(R.id.etProductName);
        etPrice = (EditText) findViewById(R.id.etPrice);
        etQuantity = (EditText) findViewById(R.id.etQuantity);
        btnScanQR = (ImageButton) findViewById(R.id.btnScanQR);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvProducts = findViewById(R.id.lvProducts);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, productList);
        lvProducts.setAdapter(adapter);

        lvProducts.post(() -> defaultListViewHeight = lvProducts.getHeight());  //lưu chiều cao mặc định

        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcodeLauncher.launch(new ScanOptions());
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addProduct();
                setListViewHeightBasedOnItems(lvProducts);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(BuyActivity.this, MainActivity.class);
                startActivity(i);
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                int number = random.nextInt(999999) + 1;

                // Định dạng số để đảm bảo nó có 6 chữ số
                String receiptCode = String.format("%06d", number);

                if (productList.isEmpty()) {
                    Toast.makeText(BuyActivity.this, "Danh sách sản phẩm trống.", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference receiptRef = receiptRepository.child(receiptCode);

                for (String productDetails : productList) {
                    String[] parts = productDetails.split(", ");
                    String productCode = parts[0].split(": ")[1];
                    String productName = parts[1].split(": ")[1];
                    String priceText = parts[2].split(": ")[1];
                    int quantity = Integer.parseInt(parts[3].split(": ")[1]);

                    // Lưu vào receiptRepository
                    DatabaseReference productRef = receiptRef.child(productCode);
                    productRef.child("Name").setValue(productName);
                    productRef.child("Price").setValue(priceText);
                    productRef.child("Quantity").setValue(quantity);

                    // Cập nhật số lượng trong productsRepository
                    DatabaseReference productDbRef = productsRepository.child(productCode);
                    productDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            /*if (snapshot.exists()) {
                                Integer currentQuantity = snapshot.child("Quantity").getValue(Integer.class);
                                if (currentQuantity != null) {
                                    int updatedQuantity = currentQuantity + quantity;
                                    productDbRef.child("Quantity").setValue(updatedQuantity);
                                }
                            } else {
                                Toast.makeText(BuyActivity.this, "Sản phẩm không tồn tại trong kho: " + productName, Toast.LENGTH_SHORT).show();
                            }*/
                            if (snapshot.exists()) {
                                // Sản phẩm đã tồn tại, cập nhật số lượng
                                Integer currentQuantity = snapshot.child("Quantity").getValue(Integer.class);
                                if (currentQuantity != null) {
                                    int updatedQuantity = currentQuantity + quantity;
                                    productDbRef.child("Quantity").setValue(updatedQuantity);
                                }
                            } else {
                                // Sản phẩm chưa tồn tại, thêm mới
                                productDbRef.child("Name").setValue(productName);
                                productDbRef.child("ProductCode").setValue(productCode);
                                productDbRef.child("Price").setValue(priceText);
                                productDbRef.child("Quantity").setValue(quantity);
                                Toast.makeText(BuyActivity.this, "Đã thêm sản phẩm mới: " + productName, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(BuyActivity.this, "Lỗi khi cập nhật sản phẩm: " + productName, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                // Hiển thị thông báo thành công
                Toast.makeText(BuyActivity.this, "Hóa đơn đã được thêm vào cơ sở dữ liệu với mã: " + receiptCode, Toast.LENGTH_SHORT).show();

                // Xóa danh sách và cập nhật ListView
                productList.clear();
                adapter.notifyDataSetChanged();

                // Đặt lại chiều cao mặc định cho ListView
                ViewGroup.LayoutParams params = lvProducts.getLayoutParams();
                params.height = defaultListViewHeight;
                lvProducts.setLayoutParams(params);
                lvProducts.requestLayout();
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
                    View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.UNSPECIFIED
            );
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    /*private void addProduct() {
        String productCode = etProductCode.getText().toString().trim();
        String productName = etProductName.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();

        if (productCode.isEmpty() || productName.isEmpty() || priceText.isEmpty() || quantityText.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin sản phẩm.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantityToAdd;
        try {
            quantityToAdd = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra sản phẩm trong danh sách
        boolean productExists = false;
        for (int i = 0; i < productList.size(); i++) {
            String productDetails = productList.get(i);

            // Kiểm tra mã sản phẩm
            if (productDetails.contains("Mã: " + productCode)) {
                // Tăng số lượng
                String[] parts = productDetails.split(", ");
                int existingQuantity = Integer.parseInt(parts[3].split(": ")[1]);
                int updatedQuantity = existingQuantity + quantityToAdd;

                // Cập nhật chuỗi hiển thị
                String updatedDetails = "Mã: " + productCode + ", Tên: " + productName + ", Giá: " + priceText + ", SL: " + updatedQuantity;
                productList.set(i, updatedDetails);
                adapter.notifyDataSetChanged();
                productExists = true;
                break;
            }
        }

        // Nếu chưa tồn tại, thêm mới
        if (!productExists) {
            String productDetails = "Mã: " + productCode + ", Tên: " + productName + ", Giá: " + priceText + ", SL: " + quantityText;
            productList.add(productDetails);
            adapter.notifyDataSetChanged();
        }

        // Xóa dữ liệu trong các EditText
        etProductCode.setText("");
        etProductName.setText("");
        etPrice.setText("");
        etQuantity.setText("");

        // Lưu vào Firebase
        saveToFirebase(productCode, productName, priceText, quantityToAdd);
    }*/

    private void addProduct() {
        String productCode = etProductCode.getText().toString().trim();
        String productName = etProductName.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();

        if (productCode.isEmpty() || productName.isEmpty() || priceText.isEmpty() || quantityText.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin sản phẩm.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantityToAdd;
        try {
            quantityToAdd = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra sản phẩm trong danh sách
        boolean productExists = false;
        for (int i = 0; i < productList.size(); i++) {
            String productDetails = productList.get(i);

            if (productDetails.contains("Mã: " + productCode)) {
                // Tăng số lượng
                String[] parts = productDetails.split(", ");
                int existingQuantity = Integer.parseInt(parts[3].split(": ")[1]);
                int updatedQuantity = existingQuantity + quantityToAdd;

                // Cập nhật chuỗi hiển thị
                String updatedDetails = "Mã: " + productCode + ", Tên: " + productName + ", Giá: " + priceText + ", SL: " + updatedQuantity;
                productList.set(i, updatedDetails);
                adapter.notifyDataSetChanged();
                productExists = true;
                break;
            }
        }

        // Nếu chưa tồn tại, thêm mới
        if (!productExists) {
            String productDetails = "Mã: " + productCode + ", Tên: " + productName + ", Giá: " + priceText + ", SL: " + quantityText;
            productList.add(productDetails);
            adapter.notifyDataSetChanged();
        }

        // Xóa dữ liệu trong các EditText
        etProductCode.setText("");
        etProductName.setText("");
        etPrice.setText("");
        etQuantity.setText("");
    }

    private void saveToFirebase(String productCode, String productName, String priceText, int quantityToAdd) {
        DatabaseReference productRef = productsRepository.child(productCode);

        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer currentQuantity = snapshot.child("Quantity").getValue(Integer.class);
                    int updatedQuantity = (currentQuantity != null ? currentQuantity : 0) + quantityToAdd;

                    productRef.child("Quantity").setValue(updatedQuantity)
                            .addOnSuccessListener(aVoid -> Toast.makeText(BuyActivity.this, "Cập nhật số lượng thành công.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(BuyActivity.this, "Lỗi khi cập nhật số lượng.", Toast.LENGTH_SHORT).show());
                } else {
                    productRef.child("Name").setValue(productName);
                    productRef.child("Price").setValue(priceText);
                    productRef.child("Quantity").setValue(quantityToAdd)
                            .addOnSuccessListener(aVoid -> Toast.makeText(BuyActivity.this, "Thêm sản phẩm mới thành công.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(BuyActivity.this, "Lỗi khi thêm sản phẩm mới.", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BuyActivity.this, "Lỗi khi truy cập cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    // Nếu người dùng hủy quét
                    Toast.makeText(BuyActivity.this, "Quét mã bị hủy.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        // Parse JSON từ mã QR
                        JSONObject jsonObject = new JSONObject(result.getContents());

                        // Kiểm tra các trường bắt buộc trong JSON
                        if (jsonObject.has("Code") && jsonObject.has("Name") && jsonObject.has("Price") && jsonObject.has("Quantity")) {
                            String ProductCode = jsonObject.getString("Code");
                            String ProductName = jsonObject.getString("Name");
                            String Price = jsonObject.getString("Price");
                            String Quantity = jsonObject.getString("Quantity");

                            // Điền vào EditText
                            etProductCode.setText(ProductCode);
                            etProductName.setText(ProductName);
                            etPrice.setText(Price);
                            etQuantity.setText(Quantity);
                        } else {
                            Toast.makeText(BuyActivity.this, "Mã QR không đủ thông tin.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        // In lỗi JSON ra log
                        e.printStackTrace();
                        Toast.makeText(BuyActivity.this, "Dữ liệu QR không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
}