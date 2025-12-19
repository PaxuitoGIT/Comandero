package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.uax.comandero.data.Plato;
import java.util.ArrayList;
import java.util.List;

public class DetalleMesaFragment extends Fragment {
    private int numeroMesa;
    private AppDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) numeroMesa = getArguments().getInt("numeroMesa");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detalle_mesa, container, false);
        db = AppDatabase.getDatabase(getContext());

        TextView tvTitulo = view.findViewById(R.id.tvTituloMesa);
        tvTitulo.setText("Mesa " + numeroMesa);

        RecyclerView recycler = view.findViewById(R.id.recyclerDetalle);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        db.dao().getComandaMesa(numeroMesa).observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ComandaAdapter(lineas));
        });

        view.findViewById(R.id.fabAddItem).setOnClickListener(v -> mostrarBuscador());

        view.findViewById(R.id.btnIrACaja).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("numeroMesa", numeroMesa);
            Navigation.findNavController(v).navigate(R.id.action_detalle_to_recibo, args);
        });

        return view;
    }

    private void mostrarBuscador() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_buscador, null);

        EditText etBuscar = view.findViewById(R.id.etBuscador);
        RecyclerView recyclerResultados = view.findViewById(R.id.recyclerResultados);
        recyclerResultados.setLayoutManager(new LinearLayoutManager(getContext()));

        AlertDialog dialog = builder.setView(view).create();

        // Listener de búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Plato> resultados = db.dao().buscarPlatos(s.toString());
                        getActivity().runOnUiThread(() -> {
                            recyclerResultados.setAdapter(new SearchAdapter(resultados, plato -> {
                                agregarPlatoConNota(plato);
                                dialog.dismiss();
                            }));
                        });
                    });
                }
            }
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void agregarPlatoConNota(Plato p) {
        // Dialogo para pedir nota (alergias)
        EditText input = new EditText(getContext());
        input.setHint("Notas (ej: Sin cebolla)");

        new AlertDialog.Builder(getContext())
                .setTitle("Añadir " + p.nombre)
                .setView(input)
                .setPositiveButton("Añadir", (d, w) -> {
                    String nota = input.getText().toString();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.dao().insertarLinea(new LineaComanda(numeroMesa, p.nombre, p.precio, nota, false, System.currentTimeMillis()));
                    });
                    Toast.makeText(getContext(), "Añadido", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ADAPTER COMANDA (Lista principal)
    class ComandaAdapter extends RecyclerView.Adapter<ComandaAdapter.ViewHolder> {
        List<LineaComanda> lineas;
        public ComandaAdapter(List<LineaComanda> l) { this.lineas = l; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int t) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_linea_comanda, parent, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            LineaComanda l = lineas.get(pos);
            h.tvNombre.setText(l.nombrePlato);
            h.tvPrecio.setText(l.precio + " €");
            h.tvNota.setText(l.notas != null && !l.notas.isEmpty() ? "Nota: " + l.notas : "");
        }
        @Override public int getItemCount() { return lineas == null ? 0 : lineas.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio, tvNota;
            ViewHolder(View v) { super(v); tvNombre=v.findViewById(R.id.tvNombreLinea); tvPrecio=v.findViewById(R.id.tvPrecioLinea); tvNota=v.findViewById(R.id.tvNotaLinea); }
        }
    }

    // ADAPTER BUSCADOR (Resultados de búsqueda)
    interface OnPlatoSelected { void onSelect(Plato p); }
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {
        List<Plato> platos; OnPlatoSelected listener;
        public SearchAdapter(List<Plato> p, OnPlatoSelected l) { this.platos=p; this.listener=l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Plato item = platos.get(pos);
            h.t1.setText("[" + item.codigo + "] " + item.nombre);
            h.t2.setText(item.precio + " €");
            h.itemView.setOnClickListener(v -> listener.onSelect(item));
        }
        @Override public int getItemCount() { return platos.size(); }
        class VH extends RecyclerView.ViewHolder { TextView t1,t2; VH(View v){super(v); t1=v.findViewById(android.R.id.text1); t2=v.findViewById(android.R.id.text2);} }
    }
}
