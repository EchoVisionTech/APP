package com.dam.echovisiontech.ui.compte;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dam.echovisiontech.databinding.FragmentCompteBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class CompteFragment extends Fragment {

    EditText fieldName;
    EditText fieldPhone;
    EditText fieldEmail;

    private FragmentCompteBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        CompteViewModel notificationsViewModel =
                new ViewModelProvider(this).get(CompteViewModel.class);

    binding = FragmentCompteBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

        fieldName = binding.fieldName;
        fieldPhone = binding.fieldPhone;
        fieldEmail = binding.fieldEmail;

        Button buttonRegister = binding.buttonRegister;

        buttonRegister.setOnClickListener(v -> getUserFields());

        //notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void getUserFields() {
        if (fieldName.getText().toString().isEmpty() || fieldPhone.getText().toString().isEmpty() || fieldEmail.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Complete all the fields", Toast.LENGTH_SHORT).show();;
        } else {
            String name = fieldName.getText().toString();
            String phone = fieldPhone.getText().toString();
            String email = fieldEmail.getText().toString();
            sendUserToServerAsync(name, phone, email);
        }
    }
    private void sendUserToServerAsync(String name, String phoneNumber, String email) {
        // Usa un Executor para ejecutar la tarea en otro hilo
        Executor sendExecutor = Executors.newSingleThreadExecutor();
        sendExecutor.execute(() -> {
            sendUserToServer(name, phoneNumber, email);
        });
    }
    private void sendUserToServer(String name, String phoneNumber, String email) {

        String serverUrl = "https://ams22.ieti.site:443/api/user/register";

        Log.d("USER", name + " - " + phoneNumber + " - " + email);

        // create JSON
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("phone", phoneNumber);
            json.put("email", email);


            // send JSON
            URL url = new URL(serverUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Escribir el objeto JSON en el cuerpo de la solicitud
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            outputStreamWriter.write(String.valueOf(new JSONObject().put("data",json.toString())));
            outputStreamWriter.flush();
            outputStreamWriter.close();

            // Leer la respuesta del servidor
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String response = reader.readLine();
            //Log.d("Respuesta", response);

            connection.disconnect();

        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

}