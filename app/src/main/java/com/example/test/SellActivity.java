package com.example.test;

import android.os.Bundle;
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


     //Phương thức tải dữ liệu sản phẩm từ Firebase và cập nhật vào ListView
    private void loadProductsFromFirebase() {
        productsRepository.addValueEventListener(new ValueEventListener() {
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

                        // Kiểm tra xem sản phẩm đã tồn tại trong 'products' chưa
                        productsRepository.child(ProductCode).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Sản phẩm đã tồn tại, cập nhật số lượng
                                    Integer currentQuantity = snapshot.child("Quantity").getValue(Integer.class);
                                    if (currentQuantity != null) {
                                        productsRepository.child(ProductCode).child("Quantity").setValue(currentQuantity + Quantity)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(SellActivity.this, "Cập nhật số lượng sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(SellActivity.this, "Cập nhật số lượng sản phẩm thất bại!", Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Toast.makeText(SellActivity.this, "Lỗi khi lấy số lượng hiện tại.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // Sản phẩm chưa tồn tại, thêm mới
                                    Product product = new Product(ProductCode, ProductName, Price, Quantity);
                                    productsRepository.child(ProductCode).setValue(product) //productsRepository
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(SellActivity.this, "Thêm mặt hàng thành công!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(SellActivity.this, "Thêm mặt hàng thất bại!", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SellActivity.this, "Lỗi khi thêm dữ liệu.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(SellActivity.this, "Dữ liệu QR không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /*//Phương thức tải dữ liệu sản phẩm từ Firebase và cập nhật vào ListView
    private void loadProductsFromFirebase() {
        databaseReference.child("bill").addValueEventListener(new ValueEventListener() {
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

    // chạy và xử lý kết quả từ việc quét QR code
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {  //Nếu quét bị hủy
                    Toast.makeText(SellActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result.getContents());
                        String ProductCode = jsonObject.getString("Code");
                        String ProductName = jsonObject.getString("Name");
                        String Price = jsonObject.getString("Price");
                        int Quantity = Integer.parseInt(jsonObject.getString("Quantity"));


                        Random random = new Random();
                        int number = random.nextInt(999999) + 1;

                        // Định dạng số để đảm bảo nó có 6 chữ số
                        String codeBill = String.format("%06d", number);

                        databaseReference.child("bill").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.hasChild(codeBill))
                                    Toast.makeText(SellActivity.this, "Mã đơn đã tồn tại", Toast.LENGTH_SHORT).show();
                                else{
                                    Product product = new Product(ProductName, Price, Quantity);

                                    //nếu mã mặt hàng đã tồn tại tăng số lượng sản phẩm
                                    if (snapshot.hasChild(ProductCode))
                                    {
                                        product.Quantity += 1;
                                    }
                                    databaseReference.child("bill").child("Code: " + codeBill).child("ProductCode: " +ProductCode).setValue(product).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SellActivity.this, "Thêm mặt hàng thành công!", Toast.LENGTH_SHORT).show();
                                    })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(SellActivity.this, "Thêm mặt hàng thất bại!", Toast.LENGTH_SHORT).show();
                                            });
                                    // Lấy dữ liệu từ Firebase và cập nhật vào ListView
                                    loadProductsFromFirebase();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SellActivity.this, "Lỗi khi thêm dữ liệu.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(SellActivity.this, "Dữ liệu QR không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                }
            });*/
}