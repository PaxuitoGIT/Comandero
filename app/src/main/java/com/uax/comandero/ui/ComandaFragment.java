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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.LineaComanda;
import java.util.List;

public class ComandaFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comanda, container, false);
        RecyclerView recycler = view.findViewById(R.id.recyclerComanda);
        Button btnCerrar = view.findViewById(R.id.btnCerrarMesa);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        AppDatabase db = AppDatabase.getDatabase(getContext());

        db.dao().getComandaActual().observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ComandaAdapter(lineas));
            // Calcular total opcionalmente aquí
        });

        btnCerrar.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.dao().cerrarMesa();
            });
            Toast.makeText(getContext(), "Mesa cerrada y cobrada", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // Reutilizamos estructura simple para adapter
    class ComandaAdapter extends RecyclerView.Adapter<ComandaAdapter.ViewHolder> {
        private List<LineaComanda> lineas;

        public ComandaAdapter(List<LineaComanda> lineas) { this.lineas = lineas; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usamos un layout simple de Android para ahorrar crear otro XML
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LineaComanda l = lineas.get(position);
            holder.text1.setText(l.nombrePlato);
            holder.text2.setText(l.precio + " €");
        }

        @Override public int getItemCount() { return lineas == null ? 0 : lineas.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}