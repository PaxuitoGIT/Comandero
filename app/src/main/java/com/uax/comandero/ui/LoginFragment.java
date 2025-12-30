package com.uax.comandero.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.uax.comandero.R;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el diseño
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Vincular las vistas con los IDs del XML
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        // 3. Configurar el botón
        btnLogin.setOnClickListener(v -> loginUser(view));

        // Si el usuario ya estaba logueado, pasar directo (comentar si no se desea)
        if (mAuth.getCurrentUser() != null) {
             Navigation.findNavController(view).navigate(R.id.action_login_to_menu);
         }
    }

    private void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones básicas
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("El email es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("La contraseña es obligatoria");
            return;
        }

        // Llamada a Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Login correcto: Ejecutar la navegación
                        Toast.makeText(getContext(), "Bienvenido", Toast.LENGTH_SHORT).show();

                        // AQUÍ ESTÁ LA ACCIÓN QUE PEDISTE
                        Navigation.findNavController(view).navigate(R.id.action_login_to_menu);

                    } else {
                        // Error en Login
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}