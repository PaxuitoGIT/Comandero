package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Plato;
import java.util.List;

public class MenuFragment extends Fragment {

    private AppDatabase db;
    private boolean esAdmin = false;

    // Categorías fijas para el Spinner
    private final String[] CATEGORIAS = {"Entrantes", "Principales", "Postres", "Bebidas", "Otros"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        db = AppDatabase.getDatabase(getContext());

        // --- 1. LÓGICA DE ROLES (SEGURIDAD) ---
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FloatingActionButton fab = view.findViewById(R.id.fabAddPlato);

        // Simulamos que el admin es este correo (o cualquiera que empiece por 'admin')
        if (user != null && (user.getEmail().equals("admin@uax.com") || user.getEmail().startsWith("admin"))) {
            esAdmin = true;
            fab.setVisibility(View.VISIBLE); // El jefe puede añadir
        } else {
            esAdmin = false;
            fab.setVisibility(View.GONE); // Los camareros SOLO ven la lista
        }

        // --- 2. LISTADO ---
        RecyclerView recycler = view.findViewById(R.id.recyclerMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        db.dao().getMenu().observe(getViewLifecycleOwner(), platos -> {
            recycler.setAdapter(new MenuAdapter(platos));
        });

        fab.setOnClickListener(v -> mostrarDialogoPlato(null));

        return view;
    }

    private void mostrarDialogoPlato(Plato platoEditar) {
        if (!esAdmin) return; // Doble seguridad

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_plato, null);

        EditText etCodigo = view.findViewById(R.id.etCodigoPlato);
        EditText etNombre = view.findViewById(R.id.etNombrePlato);
        EditText etPrecio = view.findViewById(R.id.etPrecioPlato);
        CheckBox chkCocina = view.findViewById(R.id.chkEsCocina);
        Spinner spCategoria = view.findViewById(R.id.spCategoria);

        // Configurar Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, CATEGORIAS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(adapter);

        if (platoEditar != null) {
            etCodigo.setText(platoEditar.codigo);
            etCodigo.setEnabled(false);
            etNombre.setText(platoEditar.nombre);
            etPrecio.setText(String.valueOf(platoEditar.precio));
            chkCocina.setChecked(platoEditar.esCocina);

            // Seleccionar categoría actual en el spinner
            for (int i = 0; i < CATEGORIAS.length; i++) {
                if (CATEGORIAS[i].equals(platoEditar.categoria)) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
        }

        builder.setView(view)
                .setTitle(platoEditar == null ? "Nuevo Plato" : "Editar Plato")
                .setPositiveButton("Guardar", (d, w) -> {
                    String codigo = etCodigo.getText().toString();
                    String nombre = etNombre.getText().toString();
                    String precioStr = etPrecio.getText().toString();
                    boolean esCocina = chkCocina.isChecked();
                    String catSeleccionada = spCategoria.getSelectedItem().toString();

                    if (codigo.isEmpty() || nombre.isEmpty() || precioStr.isEmpty()) return;

                    double precio = Double.parseDouble(precioStr);

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (platoEditar == null) {
                            if (db.dao().contarPlatosPorCodigo(codigo) > 0) {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Código ya existe", Toast.LENGTH_SHORT).show());
                            } else {
                                // GUARDAR CON CATEGORÍA
                                db.dao().insertarPlato(new Plato(codigo, nombre, precio, esCocina, catSeleccionada));
                            }
                        } else {
                            platoEditar.nombre = nombre;
                            platoEditar.precio = precio;
                            platoEditar.esCocina = esCocina;
                            platoEditar.categoria = catSeleccionada;
                            db.dao().actualizarPlato(platoEditar);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        List<Plato> l; MenuAdapter(List<Plato> l){this.l=l;}
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_plato, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Plato p = l.get(pos);
            h.t1.setText(p.nombre);
            // Mostramos Categoría y si es Cocina
            String info = p.precio + "€ • " + (p.categoria != null ? p.categoria : "General");
            if(p.esCocina) info += " 🍳";

            h.t2.setText(info);

            // Si no es admin, ocultamos el botón de editar
            h.btnEdit.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
            h.btnEdit.setOnClickListener(v-> mostrarDialogoPlato(p));
        }
        @Override public int getItemCount() { return l.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2; ImageButton btnEdit;
            ViewHolder(View v){super(v); t1=v.findViewById(R.id.tvNombrePlato); t2=v.findViewById(R.id.tvPrecioPlato); btnEdit=v.findViewById(R.id.btnEditPlato);}
        }
    }
}