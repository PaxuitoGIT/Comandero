package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Plato;
import java.util.List;

public class MenuFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        RecyclerView recycler = view.findViewById(R.id.recyclerMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        AppDatabase db = AppDatabase.getDatabase(getContext());

        // Cargar lista
        db.dao().getMenu().observe(getViewLifecycleOwner(), platos -> {
            recycler.setAdapter(new MenuAdapter(platos));
        });

        // FAB Añadir
        view.findViewById(R.id.fabAddPlato).setOnClickListener(v -> mostrarDialogoPlato(null));
        return view;
    }

    private void mostrarDialogoPlato(Plato platoExistente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_plato, null);

        EditText etId = view.findViewById(R.id.etIdPlato);
        EditText etNombre = view.findViewById(R.id.etNombrePlato);
        EditText etPrecio = view.findViewById(R.id.etPrecioPlato);

        if (platoExistente != null) {
            etId.setText(platoExistente.codigo);
            etId.setEnabled(false); // ID no editable
            etNombre.setText(platoExistente.nombre);
            etPrecio.setText(String.valueOf(platoExistente.precio));
        }

        builder.setView(view)
                .setTitle(platoExistente == null ? "Nuevo Plato" : "Editar Plato")
                .setPositiveButton("Guardar", null) // Se sobrescribe abajo
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String codigo = etId.getText().toString().trim();
            String nombre = etNombre.getText().toString().trim();
            String precioStr = etPrecio.getText().toString().trim();

            if (codigo.isEmpty() || nombre.isEmpty() || precioStr.isEmpty()) {
                Toast.makeText(getContext(), "Faltan datos", Toast.LENGTH_SHORT).show();
                return;
            }

            double precio = Double.parseDouble(precioStr);
            AppDatabase db = AppDatabase.getDatabase(getContext());

            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (platoExistente == null) {
                    // Validar ID único
                    if (db.dao().contarPlatosPorCodigo(codigo) > 0) {
                        getActivity().runOnUiThread(() -> etId.setError("ID ya existe"));
                    } else {
                        db.dao().insertarPlato(new Plato(codigo, nombre, precio, "General"));
                        getActivity().runOnUiThread(dialog::dismiss);
                    }
                } else {
                    platoExistente.nombre = nombre;
                    platoExistente.precio = precio;
                    db.dao().actualizarPlato(platoExistente);
                    getActivity().runOnUiThread(dialog::dismiss);
                }
            });
        });
    }

    // Adapter Interno
    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        private List<Plato> list;
        public MenuAdapter(List<Plato> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reutilizo item_linea_comanda para ahorrar XML, pero idealmente sería item_plato
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Plato p = list.get(position);
            holder.t1.setText("[" + p.codigo + "] " + p.nombre);
            holder.t2.setText(p.precio + " €");
            holder.itemView.setOnClickListener(v -> mostrarDialogoPlato(p));
        }
        @Override public int getItemCount() { return list == null ? 0 : list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            ViewHolder(View v) { super(v); t1 = v.findViewById(android.R.id.text1); t2 = v.findViewById(android.R.id.text2); }
        }
    }
}