package com.dam.echovisiontech.ui.ullada;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.dam.echovisiontech.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class UlladaFragment extends Fragment {

    private int cont_toast = 0;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Executor executor = Executors.newSingleThreadExecutor(); // You need to initialize an executor
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorListener;
    private long lastTapTime = 0;
    private TextToSpeech tts;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ullada, container, false);

        previewView = root.findViewById(R.id.previewView);

        Button captureImage = root.findViewById(R.id.captureImage);
        captureImage.setOnClickListener(v -> captureImage());

        // Detectar Doble Tap
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float zAcc = sensorEvent.values[2];
                // Check for a double tap based on z-axis movement
                detectDoubleTap(zAcc);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                // Ignore for now
            }
        };

        // Text to speech
        Locale locSpanish = new Locale("spa", "ESP");
        tts = new TextToSpeech(requireContext().getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(locSpanish);
                }
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        requestCameraPermission(); // Request camera permission here


        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            initializeCamera();
        }
    }

    private void initializeCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                preview, imageCapture);
    }

    private void captureImage() {
        if (!tts.isSpeaking()){
            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            File imageFile = new File(requireContext().getFilesDir(), mDateFormat.format(new Date()) + ".jpg");

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();
            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    String imagePath = outputFileResults.getSavedUri().toString(); // Get the image path
                    showImagePath(imagePath);
                    sendImageToServerAsync(imageFile); // Llama a la función que envía la imagen al servidor en otro hilo
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    error.printStackTrace();
                }
            });
        } else {
            Toast.makeText(requireContext(), "Espere a que termine la descripción de la imagen actual", Toast.LENGTH_LONG).show();
        }
    }

    private void sendImageToServerAsync(File imageFile) {
        // Usa un Executor para ejecutar la tarea en otro hilo
        Executor sendExecutor = Executors.newSingleThreadExecutor();
        sendExecutor.execute(() -> {
            sendImageToServer(imageFile);
        });
    }

    private void showImagePath(final String imagePath) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (cont_toast == 0) {
                    Toast.makeText(requireContext(), "Imagen enviada", Toast.LENGTH_LONG).show();
                    cont_toast = 1;
                }
            }
        });
    }
    private void sendImageToServer(File imageFile) {
        String imageData = convertImageToBase64(imageFile);
        String serverUrl = "https://ams22.ieti.site:443/api/maria/image";
        // Read token from file
        String token = "";
        File tokenFile = new File(requireContext().getFilesDir(), "token.txt");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(tokenFile.toPath())));
            token = reader.readLine();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            Log.e("ERROR", "Error al leer el token\n" + e);
        }

        Log.d("TOKEN", token);
        Log.d("IMAGEN", imageData);

        // create JSON
        JSONObject json = new JSONObject();
        try {
            json.put("prompt", "Describe esta imagen en castellano");
            json.put("token", token);
            json.put("image", imageData);

            // send JSON
            URL url = new URL(serverUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Escribir el objeto JSON en el cuerpo de la solicitud
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            outputStreamWriter.write(String.valueOf(json.toString()));
            outputStreamWriter.flush();
            outputStreamWriter.close();

            // Leer la respuesta del servidor
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read all the lines of the response
            String dataResponse = reader.readLine();
            String response = dataResponse;
            while (dataResponse != null) {
                Log.d("RESPONSE", dataResponse);
                dataResponse = reader.readLine();
                response += dataResponse;
            }
            // Parse json response
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");
            Log.d("STATUS", status);

            if (status.equals("OK")) {
                JSONObject data = new JSONObject(jsonResponse.getString("data"));
                String imageDescription = data.getString("response");
                Log.d("Descripcion imagen", imageDescription);
                tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, "1");
            } else {
                Log.e("ERROR", "Error en la respuesta del servidor." + jsonResponse.getString("message"));
                if (jsonResponse.getString("message").equals("Quota superada")) {
                    tts.speak("Ha superado el límite de imágenes que puede enviar al servidor. Por favor, espere a que se reinicie el contador.", TextToSpeech.QUEUE_FLUSH, null, "1");
                    Toast.makeText(requireContext(), "Has superado la cuota de imagenes diarias", Toast.LENGTH_LONG).show();
                }
            }

            connection.disconnect();

        } catch (IOException | JSONException e) {
            //throw new RuntimeException(e);
            Log.e("ERROR", "Error al enviar la imagen al servidor\n" + e);
        }
    }

    private static String convertImageToBase64(File imageFile) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(imageFile);
        } catch (FileNotFoundException e) {
            //throw new RuntimeException(e);
            Log.e("ERROR", "Error al convertir la imagen a base64\n" + e);
        }
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while (true) {
            try {
                if ((bytesRead = inputStream.read(buffer)) == -1) break;
            } catch (IOException e) {
                //throw new RuntimeException(e);
                Log.e("ERROR", "Error al convertir la imagen a base64\n" + e);
            }
            outputStream.write(buffer, 0, bytesRead);
        }
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    public void checkImageQuota() {

    }
    private void detectDoubleTap(float zAcc) {
        long now = SystemClock.uptimeMillis();
        long tapInterval = now - lastTapTime;

        if (tapInterval < 1000 && zAcc < -9.0) { // Check if tap interval is less than 1 second and zAcc indicates a tap
            //showToast("Double Tap Detected!");
            cont_toast = 0;
            captureImage();
            lastTapTime = 0; // Reset last tap time after detecting double tap
        } else if (zAcc < -9.0) { // If a tap is detected, update the last tap time
            lastTapTime = now;
        }
    }
}
