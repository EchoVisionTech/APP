package com.dam.echovisiontech;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dam.echovisiontech.ui.ullada.UlladaFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;


public class DialogRegister {
//    private static MainActivity mainActivity;
//
//    public DialogRegister(MainActivity mainActivity) {
//        this.mainActivity = mainActivity;
//    }
    static EditText fieldName;
    static EditText fieldPhone;
    static EditText fieldEmail;
    static EditText fieldCode;
    static String userPhone;

    // Method to create an error dialog
    public static void showErrorDialog(Context context, String errorMessage, String errorSender) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Error");
        builder.setMessage(errorMessage);
        builder.setPositiveButton("OK", null);
        AlertDialog errorDialog = builder.create();
        errorDialog.show();

        if (errorSender.equals("register")) {
            showRegisterDialog(context);
        } else if (errorSender.equals("sms")) {
            showSMSvalidationDialog(context);
        }
    }

    // Method to create and show the alert dialog showing the user is not registered
    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        // Set the dialog title and message
        builder.setTitle(title);
        builder.setMessage(message);
        // Set a positive button and its click listener
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
                //close app
                System.exit(0);
            }
        });

        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Call the register dialog
                dialog.dismiss();
                showRegisterDialog(context);
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Method to create and show the register dialog
    public static void showRegisterDialog(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_register, null);
        builder.setView(view);
        fieldName = view.findViewById(R.id.fieldName);
        fieldPhone = view.findViewById(R.id.fieldPhone);
        fieldEmail = view.findViewById(R.id.fieldEmail);

/*        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });*/
        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do something when the "OK" button is clicked
                if (!getUserFields(context)) {
                    // Show a message to the user indicating that fields are empty
                    Toast.makeText(context, "Complete all the fields", Toast.LENGTH_SHORT).show();
                    // call the dialog again
                    showRegisterDialog(context);
                } else {
                    dialog.dismiss();
                    String name = fieldName.getText().toString();
                    String phone = fieldPhone.getText().toString();
                    userPhone = phone;
                    String email = fieldEmail.getText().toString();
                    sendUserToServerAsync(context, name, phone, email);

                    //showSMSvalidationDialog(context);
                }
            }
        });
        AlertDialog registerDialog = builder.create();

        // Set the property to prevent dialog from dismissing when touched outside
        registerDialog.setCanceledOnTouchOutside(false);

        registerDialog.show();
    }

    // Method to create and show the SMS validation dialog
    public static void showSMSvalidationDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_validate_sms, null);
        builder.setView(view);
        fieldCode = view.findViewById(R.id.validationCode);

//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int i) {
//                dialog.dismiss();
//            }
//        });
        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do something when the "OK" button is clicked
                if (fieldCode.getText().toString().isEmpty()) {
                    // Show a message to the user indicating that fields are empty
                    Toast.makeText(context, "You have to enter the SMS code", Toast.LENGTH_SHORT).show();
                    // call the dialog again
                    showSMSvalidationDialog(context);
                } else {
                    dialog.dismiss();
                    sendCodeVerificationAsync(context, fieldCode.getText().toString(), userPhone);
                }
            }
        });
        AlertDialog smsDialog = builder.create();
        // Set the property to prevent dialog from dismissing when touched outside
        smsDialog.setCanceledOnTouchOutside(false);
        smsDialog.show();
    }

    // Method to connect and send user data to the server
    private static void sendUserToServer(Context context, String name, String phoneNumber, String email) {

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
            outputStreamWriter.write(json.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            // Leer la respuesta del servidor
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String response = reader.readLine();
            //Log.d("Respuesta", response);

            // Register status control
            if (response.equals("OK")) {
                Log.d("REGISTER", "User registered");
                // Show a toast message to the user to aknowledge the registration
                //Toast.makeText(context, "User registered", Toast.LENGTH_SHORT).show();
                // Go to next dialog
                showSMSvalidationDialog(context);
            } else {
                //Toast.makeText(context, "Error al registrar al usuario", Toast.LENGTH_SHORT).show();
                Log.e("REGISTER", "Error registering user");
                showRegisterDialog(context);
            }

            connection.disconnect();

        } catch (IOException | JSONException e) {
            //throw new RuntimeException(e);
            Log.d("ERROR", "Error registering user\n" + e);
        }
    }

    // Send user data to the server in a new thread
    private static void sendUserToServerAsync(Context context, String name, String phoneNumber, String email) {
        // Usa un Executor para ejecutar la tarea en otro hilo
        Executor sendExecutor = Executors.newSingleThreadExecutor();
        sendExecutor.execute(() -> {
            sendUserToServer(context, name, phoneNumber, email);
        });
    }

    private static void sendCodeVerification(Context context, String verificationCode, String phoneNumber) {

        String serverUrl = "https://ams22.ieti.site:443/api/usuaris/validar";

        // create JSON
        JSONObject json = new JSONObject();
        try {
            json.put("number", verificationCode);
            json.put("phone", phoneNumber);

            // send JSON
            URL url = new URL(serverUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Escribir el objeto JSON en el cuerpo de la solicitud
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            outputStreamWriter.write(json.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            // Leer la respuesta del servidor
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            // Read all the lines of the response
            String dataResponse = reader.readLine();
            String response = dataResponse;
            while (dataResponse != null) {
                dataResponse = reader.readLine();
                response += dataResponse;
                Log.d("RESPUESTA", response);
            }
            // Parse json response
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");
            Log.d("STATUS", status);

            if (status.equals("OK")) {
                JSONObject data = new JSONObject(jsonResponse.getString("data"));
                String token = data.getString("api_key");
                Log.d("REGISTRO", "User registered");
                Log.d("APIKEY", token);
                // Show a toast message to the user to aknowledge the registration

                //Toast.makeText(context, "User registered", Toast.LENGTH_SHORT).show();

                // Write token in private app folder
                OutputStreamWriter outputStreamWriterToken = new OutputStreamWriter(context.openFileOutput("token.txt", Context.MODE_PRIVATE));
                outputStreamWriterToken.write(token);
                outputStreamWriterToken.close();
                //mainActivity.setTokenValidated(true);
            } else {
                Toast.makeText(context, "Error registering user", Toast.LENGTH_SHORT).show();
                Log.d("ERROR", "Error sending verification code");
                showSMSvalidationDialog(context);
            }
            connection.disconnect();

        } catch (IOException | JSONException e) {
            //throw new RuntimeException(e);
            Log.d("ERROR", "Error registering user\n" + e);
        }
    }

    // Send the verification code to the server in a new thread
    private static void sendCodeVerificationAsync(Context context, String verificationCode, String phoneNumber) {
        // Usa un Executor para ejecutar la tarea en otro hilo
        Executor sendExecutor = Executors.newSingleThreadExecutor();
        sendExecutor.execute(() -> {
            sendCodeVerification(context, verificationCode, phoneNumber);
        });
    }

    // Get the user fields and check if they are empty. Logs for debugging
    private static boolean getUserFields(Context context) {
        if (fieldName.getText().toString().isEmpty() || fieldPhone.getText().toString().isEmpty() || fieldEmail.getText().toString().isEmpty()) {
            Log.d("fieldsFalse",fieldName.getText().toString());
            Log.d("fieldsFalse",fieldPhone.getText().toString());
            Log.d("fieldsFalse",fieldEmail.getText().toString());
            return false;
        } else {
            Log.d("fieldsTrue",fieldName.getText().toString());
            Log.d("fieldsTrue",fieldPhone.getText().toString());
            Log.d("fieldsTrue",fieldEmail.getText().toString());

            return true;
        }
    }
}


