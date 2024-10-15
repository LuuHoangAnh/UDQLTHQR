package com.example.test;

public class Product {
    public String ProductCode;
    public String ProductName;
    public String Price;
    public int Quantity;

    // Constructor không tham số cần thiết cho Firebase
    public Product() {
    }

    // Constructor có tham số để tạo đối tượng Product
    public Product(String productCode, String productName, String price, int quantity) {
        ProductCode = productCode;
        ProductName = productName;
        Price = price;
        Quantity = quantity;
    }
}