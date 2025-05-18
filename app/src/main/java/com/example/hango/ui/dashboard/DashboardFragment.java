package com.example.hango.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hango.MainActivity;
import com.example.hango.R;

public class DashboardFragment extends Fragment {

    private ImageView openCameraButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        openCameraButton = view.findViewById(R.id.openCameraButton);

        openCameraButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(bitmap -> {
                    // Gọi MainActivity xử lý API và chuyển Fragment
                    ((MainActivity) getActivity()).sendImageToApi(bitmap);
                });
            } else {
                Toast.makeText(getContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
