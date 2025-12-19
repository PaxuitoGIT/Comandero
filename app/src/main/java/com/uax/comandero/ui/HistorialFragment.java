package com.uax.comandero.ui;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.LineaComanda;
import java.util.List;

public class HistorialFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        RecyclerView recycler = view.findViewById(R.id.recyclerHistorial);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        AppDatabase db = AppDatabase.getDatabase(getContext());

        // Observamos la query que filtra por 'esPagado = 1'
        db.dao().getHistorial().observe(getViewLifecycleOwner(), lista -> {
            recycler.setAdapter(new HistorialAdapter(lista));
        });

        return view;
    }

    // Adapter interno para el historial
    class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private List<LineaComanda> lista;

        public HistorialAdapter(List<LineaComanda> lista) { this.lista = lista; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usamos un layout nativo de Android de dos líneas para ahorrar crear otro XML
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LineaComanda item = lista.get(position);

            // Mostramos Mesa y Nombre del plato
            holder.textTitulo.setText("Mesa " + item.numeroMesa + ": " + item.nombrePlato);

            // Formateamos fecha y precio
            CharSequence fechaTexto = android.text.format.DateFormat.format("dd/MM HH:mm", item.fecha);
            String detalle = String.format("%.2f € - %s", item.precio, fechaTexto);

            // Si tenía nota, la mostramos
            if (item.notas != null && !item.notas.isEmpty()) {
                detalle += " (" + item.notas + ")";
            }

            holder.textSubtitulo.setText(detalle);
        }

        @Override public int getItemCount() { return lista == null ? 0 : lista.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTitulo, textSubtitulo;
            ViewHolder(View v) {
                super(v);
                textTitulo = v.findViewById(android.R.id.text1);
                textSubtitulo = v.findViewById(android.R.id.text2);
            }
        }
    }
}
