package com.dam.echovisiontech.ui.ullada;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dam.echovisiontech.databinding.FragmentUlladaBinding;

public class UlladaFragment extends Fragment {

private FragmentUlladaBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        UlladaViewModel homeViewModel =
                new ViewModelProvider(this).get(UlladaViewModel.class);

    binding = FragmentUlladaBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}