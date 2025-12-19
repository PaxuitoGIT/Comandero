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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.LineaComanda;
import java.util.List;

public class ReciboFragment extends Fragment {
    private int numeroMesa;
    private AppDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) numeroMesa = getArguments().getInt("numeroMesa");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recibo, container, false);
        db = AppDatabase.getDatabase(getContext());

        RecyclerView recycler = view.findViewById(R.id.recyclerRecibo);
        TextView tvTotal = view.findViewById(R.id.tvTotalRecibo);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarPago);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        db.dao().getComandaMesa(numeroMesa).observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ReciboAdapter(lineas));

            // Calcular total
            double total = 0;
            if (lineas != null) {
                for (LineaComanda l : lineas) total += l.precio;
            }
            tvTotal.setText(String.format("%.2f €", total));
        });

        btnConfirmar.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.dao().cerrarMesa(numeroMesa); // UPDATE a pagado=1

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Venta cobrada y cerrada", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(v).navigate(R.id.action_recibo_to_mesas);
                });
            });
        });

        return view;
    }

    // Usamos el mismo layout de item que en comanda
    class ReciboAdapter extends RecyclerView.Adapter<ReciboAdapter.ViewHolder> {
        List<LineaComanda> list;
        public ReciboAdapter(List<LineaComanda> l) { this.list = l; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            LineaComanda l = list.get(pos);
            h.tvNombre.setText(l.nombrePlato);
            h.tvPrecio.setText(l.precio + " €");
            h.tvNota.setText(l.notas);
        }
        @Override public int getItemCount() { return list == null ? 0 : list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio, tvNota;
            ViewHolder(View v) { super(v); tvNombre=v.findViewById(R.id.tvNombreLinea); tvPrecio=v.findViewById(R.id.tvPrecioLinea); tvNota=v.findViewById(R.id.tvNotaLinea); }
        }
    }
}