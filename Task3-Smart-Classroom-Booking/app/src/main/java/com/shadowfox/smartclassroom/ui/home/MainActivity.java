package com.shadowfox.smartclassroom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.shadowfox.smartclassroom.R;
import com.shadowfox.smartclassroom.databinding.ActivityMainBinding;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.ui.auth.LoginActivity;
import com.shadowfox.smartclassroom.ui.history.BookingHistoryFragment;
import com.shadowfox.smartclassroom.util.DataSeeder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthRepository auth = AuthRepository.getInstance();
        if (!auth.isLoggedIn() || auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ActivityMainBinding b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        DataSeeder.seedIfNeeded();

        b.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_bookings) f = new BookingHistoryFragment();
            else if (id == R.id.nav_profile) f = new ProfileFragment();
            else f = new ClassroomListFragment();
            getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fragmentContainer, f)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            b.bottomNav.setSelectedItemId(R.id.nav_classrooms);
        }
    }
}
