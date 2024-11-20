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
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    List<String> productKeys = new ArrayList<>();
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

        registerForContextMenu(lvProducts);

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
                if (productList.isEmpty()) {
                    Toast.makeText(SellActivity.this, "Không có sản phẩm nào trong danh sách!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tạo mã hóa đơn mới mỗi lần nhấn "Hoàn tất"
                String billCode = String.format("%06d", new Random().nextInt(999999) + 1);

                for (Product product : tempProductList) {
                    String productCode = product.ProductCode;
                    int newQuantity = product.Quantity;

                    if (!productCode.isEmpty() && newQuantity > 0) {
                        // Lấy thông tin sản phẩm từ Firebase
                        productsRepository.child(productCode).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String productName = snapshot.child("ProductName").getValue(String.class);
                                String price = snapshot.child("Price").getValue(String.class);
                                Integer stockQuantity = snapshot.child("Quantity").getValue(Integer.class);

                                if (stockQuantity != null && stockQuantity >= newQuantity) {
                                    // Trừ số lượng từ kho
                                    productsRepository.child(productCode).child("Quantity").setValue(stockQuantity - newQuantity)
                                            .addOnSuccessListener(aVoid -> {
                                                // Lưu thông tin sản phẩm vào hóa đơn
                                                billRepository.child(billCode).child(productCode)
                                                        .setValue(product)
                                                        .addOnSuccessListener(aVoid1 -> Log.d("Bill", "Lưu hóa đơn thành công"))
                                                        .addOnFailureListener(e -> Log.e("Bill", "Lỗi khi lưu hóa đơn", e));
                                            })
                                            .addOnFailureListener(e -> Log.e("Stock", "Lỗi khi trừ số lượng kho", e));
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

                // Dọn dẹp danh sách sản phẩm sau khi hoàn tất
                tempProductList.clear();
                productList.clear();
                adapter.notifyDataSetChanged();
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
            // Xóa sản phẩm chỉ trên ListView
            tempProductList.remove(pos);
            updateProductListView();

            Toast.makeText(this, "Đã xóa sản phẩm khỏi danh sách hiển thị.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
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
                    String barcode = result.getContents(); // Lấy mã vạch
                    // Kiểm tra mã vạch trong Firebase
                    productsRepository.child(barcode).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Lấy thông tin sản phẩm từ Firebase
                                String productCode = snapshot.child("ProductCode").getValue(String.class);
                                String productName = snapshot.child("Name").getValue(String.class);
                                String price = snapshot.child("Price").getValue(String.class);
                                Integer stockQuantity = snapshot.child("Quantity").getValue(Integer.class);

                                if (productCode != null && productName != null && price != null && stockQuantity != null) {
                                    int quantity = 1; // Mặc định số lượng quét là 1, bạn có thể thay đổi logic này

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
                                    Toast.makeText(SellActivity.this, "Dữ liệu sản phẩm không hợp lệ.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(SellActivity.this, "Không tìm thấy sản phẩm với mã vạch này.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SellActivity.this, "Lỗi khi kiểm tra dữ liệu Firebase!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
//    QR Code
    /*private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
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
            });*/

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

    public void editProduct(int pos) {
        // Lấy dữ liệu từ danh sách tạm thời
        Product currentProduct = tempProductList.get(pos);

        View v = LayoutInflater.from(this).inflate(R.layout.buyandselldialog, null);
        EditText code = v.findViewById(R.id.edtextmenu_code);
        EditText name = v.findViewById(R.id.edtextmenu_name);
        EditText price = v.findViewById(R.id.edtextmenu_price);
        EditText quantity = v.findViewById(R.id.edtextmenu_quantity);

        // Gán dữ liệu vào các ô nhập
        code.setText(currentProduct.ProductCode);
        name.setText(currentProduct.ProductName);
        price.setText(currentProduct.Price);
        quantity.setText(String.valueOf(currentProduct.Quantity));

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setView(v);
        b.setPositiveButton("Cập nhật", (dialogInterface, i) -> {
            String newCode = code.getText().toString();
            String newName = name.getText().toString();
            String newPrice = price.getText().toString();
            int newQuantity;

            try {
                newQuantity = Integer.parseInt(quantity.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số lượng phải là số nguyên.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newName.isEmpty() && !newPrice.isEmpty() && !newCode.isEmpty()) {
                // Cập nhật dữ liệu trong danh sách tạm thời
                currentProduct.ProductCode = newCode;
                currentProduct.ProductName = newName;
                currentProduct.Price = newPrice;
                currentProduct.Quantity = newQuantity;

                // Cập nhật lại ListView
                updateProductListView();
                Toast.makeText(this, "Cập nhật thông tin sản phẩm thành công.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Hủy", null).show();
    }
}