package com.uax.comandero.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.LineaComanda;
import com.uax.comandero.data.Plato;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        RecyclerView recycler = view.findViewById(R.id.recyclerMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializar DB y poblar si está vacía (Solo para POC)
        AppDatabase db = AppDatabase.getDatabase(getContext());

        // Cargar datos
        db.dao().getMenu().observe(getViewLifecycleOwner(), platos -> {
            if (platos == null || platos.isEmpty()) {
                poblarDatosIniciales(db);
            } else {
                recycler.setAdapter(new PlatosAdapter(platos, db));
            }
        });

        return view;
    }

    private void poblarDatosIniciales(AppDatabase db) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.dao().insertarPlato(new Plato("Hamburguesa", 12.50, "Principal"));
            db.dao().insertarPlato(new Plato("Cerveza", 3.00, "Bebida"));
            db.dao().insertarPlato(new Plato("Patatas", 5.00, "Entrante"));
        });
    }

    // --- ADAPTER INTERNO ---
    class PlatosAdapter extends RecyclerView.Adapter<PlatosAdapter.ViewHolder> {
        private List<Plato> platos;
        private AppDatabase db;

        public PlatosAdapter(List<Plato> platos, AppDatabase db) {
            this.platos = platos;
            this.db = db;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plato, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Plato p = platos.get(position);
            holder.tvNombre.setText(p.nombre);
            holder.tvPrecio.setText(p.precio + " €");

            holder.btnAdd.setOnClickListener(v -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    db.dao().insertarLinea(new LineaComanda(p.nombre, p.precio, false, System.currentTimeMillis()));
                });
                Toast.makeText(getContext(), "Añadido: " + p.nombre, Toast.LENGTH_SHORT).show();
            });
        }

        @Override public int getItemCount() { return platos.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio;
            Button btnAdd;
            ViewHolder(View v) {
                super(v);
                tvNombre = v.findViewById(R.id.tvNombre);
                tvPrecio = v.findViewById(R.id.tvPrecio);
                btnAdd = v.findViewById(R.id.btnAdd);
            }
        }
    }
}