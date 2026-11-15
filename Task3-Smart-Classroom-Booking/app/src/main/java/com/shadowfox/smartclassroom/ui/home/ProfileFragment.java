package com.shadowfox.smartclassroom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.shadowfox.smartclassroom.databinding.FragmentProfileBinding;
import com.shadowfox.smartclassroom.model.User;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.ui.admin.AdminDashboardActivity;
import com.shadowfox.smartclassroom.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AuthRepository auth = AuthRepository.getInstance();
        User u = auth.getCurrentUser();
        if (u != null) {
            String name = u.name == null ? "" : u.name;
            b.avatarText.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
            b.nameText.setText(name);
            b.emailText.setText(u.email);
            b.roleChip.setText(u.isAdmin() ? "Admin / Staff" : "Student");
            b.adminButton.setVisibility(u.isAdmin() ? View.VISIBLE : View.GONE);
        }
        b.adminButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminDashboardActivity.class)));
        b.logoutButton.setOnClickListener(v -> {
            auth.logout();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
