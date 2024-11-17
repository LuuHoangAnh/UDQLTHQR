package com.example.test;

public class Customer {
    public String fullname;
    public String phone;

    public Customer() {
    }

    public Customer(String fullname, String phone) {
        this.fullname = fullname;
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "Họ và tên: " + fullname + ", SDT: " + phone;
    }
}
