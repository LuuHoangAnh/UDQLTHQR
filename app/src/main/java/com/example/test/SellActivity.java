package com.example.test;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class SellActivity extends AppCompatActivity {
//    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");
    DatabaseReference productsRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/products");
    DatabaseReference billRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/bill");

    ImageButton btnScanQR;
    EditText etProductCode, etProductName, etPrice, etQuantity;
    Button btnInfoCustomer, btnDone;

    ListView lvProducts;

    List<String> productList = new ArrayList<>();
    ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sell);

        btnScanQR = (ImageButton) findViewById(R.id.btnScanQR);
        btnInfoCustomer = (Button) findViewById(R.id.btnInfoCustomer);
        btnDone = (Button) findViewById(R.id.btnDone);
        lvProducts = (ListView) findViewById(R.id.lvProducts);

        adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.tvProductInfo, productList);
        lvProducts.setAdapter(adapter);

        // Tải dữ liệu từ Firebase
        loadProductsFromFirebase();

        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcodeLauncher.launch(new ScanOptions());
            }
        });


    }

    Random random = new Random();
    int number = random.nextInt(999999) + 1;

    // Định dạng số để đảm bảo nó có 6 chữ số
    String billCode = String.format("%06d", number);


    //Phương thức tải dữ liệu sản phẩm từ Firebase và cập nhật vào ListView
    private void loadProductsFromFirebase() {
        billRepository.child(billCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear(); // Xóa dữ liệu cũ
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // Lấy dữ liệu từng sản phẩm
                    String productCode = dataSnapshot.child("ProductCode").getValue(String.class);
                    String productName = dataSnapshot.child("ProductName").getValue(String.class);
                    String price = dataSnapshot.child("Price").getValue(String.class);
                    Integer quantity = dataSnapshot.child("Quantity").getValue(Integer.class);

                    // Kiểm tra dữ liệu không null
                    if (productCode != null && productName != null && price != null && quantity != null) {
                        String productInfo = "Mã sản phẩm: " + productCode + ", Tên: " + productName + ", Giá: " + price + ", Số lượng: " + quantity;
                        productList.add(productInfo); // Thêm sản phẩm vào danh sách
                    } else {
                        Toast.makeText(SellActivity.this, "Dữ liệu không hợp lệ hoặc thiếu thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.notifyDataSetChanged(); // Cập nhật ListView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SellActivity.this, "Không thể tải dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Đăng ký launcher và xử lý kết quả từ việc quét QR code
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {  // Nếu quét bị hủy
                    Toast.makeText(SellActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result.getContents());
                        String ProductCode = jsonObject.getString("Code");
                        String ProductName = jsonObject.getString("Name");
                        String Price = jsonObject.getString("Price");
                        int Quantity = Integer.parseInt(jsonObject.getString("Quantity"));

                        billRepository.child(billCode).child(ProductCode).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Kiểm tra xem sản phẩm đã tồn tại trong bill chưa
                                    Integer currentBillQuantity = snapshot.child("Quantity").getValue(Integer.class);
                                    if (currentBillQuantity != null) {
                                        productsRepository.child(ProductCode).child("Quantity").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot productSnapshot) {
                                                Integer stockQuantity = productSnapshot.getValue(Integer.class);

                                                if (stockQuantity != null && stockQuantity >= Quantity) {
                                                    //Cập nhật số lượng nếu đã tồn tại
                                                    billRepository.child(billCode).child(ProductCode).child("Quantity").setValue(currentBillQuantity + Quantity)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(SellActivity.this, "Cập nhật số lượng sản phẩm thành công!", Toast.LENGTH_SHORT).show();

                                                                //Cập nhật lại kho
                                                                productsRepository.child(ProductCode).child("Quantity").setValue(stockQuantity - Quantity)
                                                                        .addOnSuccessListener(aVoid1 -> {
                                                                            Toast.makeText(SellActivity.this, "Cập nhật kho thành công!", Toast.LENGTH_SHORT).show();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Toast.makeText(SellActivity.this, "Cập nhật kho thất bại!", Toast.LENGTH_SHORT).show();
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(SellActivity.this, "Cập nhật số lượng sản phẩm thất bại!", Toast.LENGTH_SHORT).show();
                                                            });
                                                } else {
                                                    Toast.makeText(SellActivity.this, "Số lượng sản phẩm trong kho không đủ!", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Toast.makeText(SellActivity.this, "Lỗi khi kiểm tra kho dữ liệu!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(SellActivity.this, "Lỗi khi lấy số lượng hiện tại.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    //Nếu sản phẩm chưa tồn tại, kiểm tra kho và thêm
                                    productsRepository.child(ProductCode).child("Quantity").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot productSnapshot) {
                                            Integer stockQuantity = productSnapshot.getValue(Integer.class);

                                            if (stockQuantity != null && stockQuantity >= Quantity) {
                                                Product product = new Product(ProductCode, ProductName, Price, Quantity);

                                                //Thêm sản phẩm vào bill
                                                billRepository.child(billCode).child(ProductCode).setValue(product)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(SellActivity.this, "Thêm mặt hàng thành công!", Toast.LENGTH_SHORT).show();

                                                            //Cập nhật lại kho
                                                            productsRepository.child(ProductCode).child("Quantity").setValue(stockQuantity - Quantity)
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        Toast.makeText(SellActivity.this, "Cập nhật kho thành công!", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(SellActivity.this, "Cập nhật kho thất bại!", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(SellActivity.this, "Thêm mặt hàng thất bại!", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Toast.makeText(SellActivity.this, "Số lượng trong kho không đủ!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(SellActivity.this, "Lỗi khi kiểm tra kho dữ liệu!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SellActivity.this, "Lỗi khi truy cập dữ liệu hóa đơn.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(SellActivity.this, "Dữ liệu QR không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
}