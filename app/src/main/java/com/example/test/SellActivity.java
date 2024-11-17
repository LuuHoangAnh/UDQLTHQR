package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class SellActivity extends AppCompatActivity {
//    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/");
    DatabaseReference productsRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/products");
    DatabaseReference billRepository = FirebaseDatabase.getInstance().getReferenceFromUrl("https://udqlthqr-default-rtdb.firebaseio.com/bill");

    ImageButton btnScanQR;
    EditText etProductCode, etProductName, etPrice, etQuantity;
    Button btnInfoCustomer, btnDone, btnBack;

    ListView lvProducts;

    List<String> productList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    // Danh sách lưu trữ tạm thời sản phẩm quét
    List<Product> tempProductList = new ArrayList<>();

    private int defaultListViewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sell);

        btnScanQR = (ImageButton) findViewById(R.id.btnScanQR);
        btnInfoCustomer = (Button) findViewById(R.id.btnInfoCustomer);
        btnDone = (Button) findViewById(R.id.btnDone);
        btnBack = (Button) findViewById(R.id.btnBack);
        lvProducts = (ListView) findViewById(R.id.lvProducts);

        adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.tvProductInfo, productList);
        lvProducts.setAdapter(adapter);

        lvProducts.post(() -> defaultListViewHeight = lvProducts.getHeight());  //lưu chiều cao mặc định

        // Tải dữ liệu từ Firebase
        loadProductsFromFirebase();

        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { barcodeLauncher.launch(new ScanOptions());}
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SellActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });

        btnInfoCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SellActivity.this, GetCustomerInfoActivity.class);
                startActivity(i);
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tempProductList.isEmpty()) {
                    Toast.makeText(SellActivity.this, "Không có sản phẩm nào trong danh sách!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Product product : tempProductList) {
                    String productCode = product.ProductCode;
                    int quantityToSubtract = product.Quantity;

                    // Trừ số lượng từ kho
                    productsRepository.child(productCode).child("Quantity").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer stockQuantity = snapshot.getValue(Integer.class);
                            if (stockQuantity != null && stockQuantity >= quantityToSubtract) {
                                productsRepository.child(productCode).child("Quantity").setValue(stockQuantity - quantityToSubtract)
                                        .addOnSuccessListener(aVoid -> {
                                            // Lưu sản phẩm vào hóa đơn
                                            billRepository.child(billCode).child(productCode).setValue(product)
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Toast.makeText(SellActivity.this, "Lưu hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(SellActivity.this, "Lỗi khi lưu hóa đơn!", Toast.LENGTH_SHORT).show();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SellActivity.this, "Lỗi khi trừ số lượng kho!", Toast.LENGTH_SHORT).show();
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

                // Xóa danh sách tạm thời và cập nhật giao diện
                tempProductList.clear();
                productList.clear();
                adapter.notifyDataSetChanged();
                setListViewHeightBasedOnItems(lvProducts);
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
                setListViewHeightBasedOnItems(lvProducts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SellActivity.this, "Không thể tải dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(SellActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result.getContents());
                        String productCode = jsonObject.getString("Code");
                        String productName = jsonObject.getString("Name");
                        String price = jsonObject.getString("Price");
                        int quantity = Integer.parseInt(jsonObject.getString("Quantity"));

                        // Kiểm tra số lượng trong kho từ Firebase
                        productsRepository.child(productCode).child("Quantity").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Integer stockQuantity = snapshot.getValue(Integer.class);

                                if (stockQuantity != null && stockQuantity >= quantity) {
                                    // Kiểm tra sản phẩm đã tồn tại trong danh sách tạm thời chưa
                                    boolean productExists = false;
                                    for (Product product : tempProductList) {
                                        if (product.ProductCode.equals(productCode)) {
                                            // Nếu tồn tại, tăng số lượng
                                            product.Quantity += quantity;
                                            productExists = true;
                                            break;
                                        }
                                    }

                                    if (!productExists) {
                                        // Nếu chưa tồn tại, thêm mới
                                        tempProductList.add(new Product(productCode, productName, price, quantity));
                                    }

                                    // Cập nhật ListView
                                    updateProductListView();

                                    Toast.makeText(SellActivity.this, "Đã thêm hoặc cập nhật sản phẩm trong danh sách.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SellActivity.this, "Số lượng sản phẩm trong kho không đủ!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SellActivity.this, "Lỗi khi kiểm tra kho dữ liệu!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(SellActivity.this, "Dữ liệu QR không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void updateProductListView() {
        productList.clear(); // Xóa danh sách hiển thị cũ
        for (Product product : tempProductList) {
            String productInfo = "Mã sản phẩm: " + product.ProductCode +
                    ", Tên: " + product.ProductName +
                    ", Giá: " + product.Price +
                    ", Số lượng: " + product.Quantity;
            productList.add(productInfo);
        }
        adapter.notifyDataSetChanged(); // Cập nhật lại ListView
        setListViewHeightBasedOnItems(lvProducts); // Cập nhật chiều cao ListView
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
}