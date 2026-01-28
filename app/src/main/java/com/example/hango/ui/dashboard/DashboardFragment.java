package com.example.hango.ui.dashboard;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hango.MainActivity;
import com.example.hango.R;

public class DashboardFragment extends Fragment {

    private EditText searchEditText;
    private ImageView openCameraButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        searchEditText   = view.findViewById(R.id.searchEditText);
        openCameraButton = view.findViewById(R.id.openCameraButton);

        // --- Khi bấm Enter (IME_ACTION_SEARCH) trên bàn phím ---
        searchEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = searchEditText.getText().toString().trim();
                if (query.isEmpty()) {
                    Toast.makeText(getContext(), "Vui lòng nhập từ khoá", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).sendTextToApi(query, 20);
                } else {
                    Toast.makeText(getContext(), "Không thể tìm kiếm", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // --- Nút mở camera ---
        openCameraButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(bitmap ->
                        ((MainActivity) getActivity()).sendImageToApi(bitmap)
                );
            } else {
                Toast.makeText(getContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
