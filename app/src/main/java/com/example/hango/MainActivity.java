package com.example.hango;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.hango.R;

import androidx.fragment.app.Fragment;
import com.example.hango.ui.dashboard.DashboardFragment;
import com.example.hango.ui.cart.CartFragment;
import com.example.hango.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadFragment(new DashboardFragment());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getTitle().toString()) {
                case "Trang chủ":
                    loadFragment(new DashboardFragment());
                    return true;
                case "Trang chính":
                    loadFragment(new HomeFragment());
                    return true;
                case "Cửa hàng":
                    loadFragment(new CartFragment());
                    return true;
                // Thêm các case khác nếu cần
            }
            return false;
        });
    }


    private void loadHomeFragment() {
        // Tạo instance của HomeFragment
        HomeFragment homeFragment = new HomeFragment();

        // Lấy FragmentTransaction và thay thế Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, homeFragment);  // Đảm bảo bạn truyền đúng container ID
        transaction.addToBackStack(null);  // Nếu muốn cho phép quay lại fragment cũ
        transaction.commit();

    }

    private void cartHomeFragment() {
        // Tạo instance của HomeFragment
        CartFragment cartFragment = new CartFragment();

        // Lấy FragmentTransaction và thay thế Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, cartFragment);  // Đảm bảo bạn truyền đúng container ID
        transaction.addToBackStack(null);  // Nếu muốn cho phép quay lại fragment cũ
        transaction.commit();

    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
