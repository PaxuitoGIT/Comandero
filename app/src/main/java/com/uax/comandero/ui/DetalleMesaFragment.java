package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        // --- CAMBIO PRINCIPAL AQUÍ ---
        // Pasamos la interfaz OnComandaAccion con los 3 comportamientos
        db.dao().getComandaMesa(numeroMesa).observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ComandaAdapter(lineas, new OnComandaAccion() {
                @Override
                public void onEditar(LineaComanda linea) {
                    editarLinea(linea);
                }

                @Override
                public void onRepetir(LineaComanda linea) {
                    repetirLinea(linea);
                }

                @Override
                public void onEliminar(LineaComanda linea) {
                    confirmarBorrado(linea);
                }
            }));
        });

        view.findViewById(R.id.fabAddItem).setOnClickListener(v -> mostrarBuscador());

        view.findViewById(R.id.btnIrACaja).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("numeroMesa", numeroMesa);
            Navigation.findNavController(v).navigate(R.id.action_detalle_to_recibo, args);
        });

        return view;
    }

    // --- LÓGICA DE LAS ACCIONES (Métodos llamados por los botones) ---

    private void editarLinea(LineaComanda linea) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputPrecio = new EditText(getContext());
        inputPrecio.setHint("Precio");
        inputPrecio.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputPrecio.setText(String.valueOf(linea.precio));
        layout.addView(inputPrecio);

        final EditText inputNota = new EditText(getContext());
        inputNota.setHint("Nota (ej: Sin hielo)");
        inputNota.setText(linea.notas);
        layout.addView(inputNota);

        new AlertDialog.Builder(getContext())
                .setTitle("Editar " + linea.nombrePlato)
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nuevoPrecioStr = inputPrecio.getText().toString();
                    String nuevaNota = inputNota.getText().toString();

                    if (!nuevoPrecioStr.isEmpty()) {
                        linea.precio = Double.parseDouble(nuevoPrecioStr);
                        linea.notas = nuevaNota;

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            db.dao().actualizarLinea(linea);
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void repetirLinea(LineaComanda original) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.dao().insertarLinea(new LineaComanda(
                    original.numeroMesa,
                    original.nombrePlato,
                    original.precio,
                    original.notas,
                    false,
                    System.currentTimeMillis()
            ));
        });
        Toast.makeText(getContext(), "Añadido +1 " + original.nombrePlato, Toast.LENGTH_SHORT).show();
    }

    private void confirmarBorrado(LineaComanda linea) {
        new AlertDialog.Builder(getContext())
                .setTitle("¿Eliminar?")
                .setMessage("Vas a borrar: " + linea.nombrePlato)
                .setPositiveButton("Eliminar", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.dao().borrarLinea(linea);
                    });
                    Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- LÓGICA DE AÑADIR NUEVOS (BUSCADOR) ---

    private void mostrarBuscador() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_buscador, null);
        EditText etBuscar = view.findViewById(R.id.etBuscador);
        RecyclerView recyclerRes = view.findViewById(R.id.recyclerResultados);
        recyclerRes.setLayoutManager(new LinearLayoutManager(getContext()));

        AlertDialog dialog = builder.setView(view).create();

        etBuscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int c, int a) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Plato> res = db.dao().buscarPlatos(s.toString());
                        getActivity().runOnUiThread(() ->
                                recyclerRes.setAdapter(new SearchAdapter(res, plato -> {
                                    agregarPlato(plato);
                                    dialog.dismiss();
                                })));
                    });
                }
            }
            public void afterTextChanged(Editable s) {}
        });
        dialog.show();
    }

    private void agregarPlato(Plato p) {
        EditText input = new EditText(getContext());
        input.setHint("Notas (ej: Sin cebolla)");
        new AlertDialog.Builder(getContext())
                .setTitle(p.nombre)
                .setView(input)
                .setPositiveButton("Añadir", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.dao().insertarLinea(new LineaComanda(numeroMesa, p.nombre, p.precio, input.getText().toString(), false, System.currentTimeMillis()));
                    });
                })
                .show();
    }

    // ==========================================
    // ADAPTERS Y VIEW HOLDERS
    // ==========================================

    // 1. Interfaz para los 3 botones de acción
    interface OnComandaAccion {
        void onEditar(LineaComanda linea);
        void onRepetir(LineaComanda linea);
        void onEliminar(LineaComanda linea);
    }

    // 2. Adapter de la Comanda (Lista principal)
    class ComandaAdapter extends RecyclerView.Adapter<ComandaAdapter.ViewHolder> {
        List<LineaComanda> lineas;
        OnComandaAccion listener;

        public ComandaAdapter(List<LineaComanda> l, OnComandaAccion listener) {
            this.lineas = l;
            this.listener = listener;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            // Asegúrate de que este layout (item_linea_comanda) tiene los IDs: btnItemEdit, btnItemPlus, btnItemDelete
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            LineaComanda item = lineas.get(pos);
            h.tvNombre.setText(item.nombrePlato);
            h.tvPrecio.setText(item.precio + " €");

            if (item.notas != null && !item.notas.isEmpty()) {
                h.tvNota.setText(item.notas);
                h.tvNota.setVisibility(View.VISIBLE);
            } else {
                h.tvNota.setVisibility(View.GONE);
            }

            // Asignamos los clicks a los botones específicos
            h.btnEdit.setOnClickListener(v -> listener.onEditar(item));
            h.btnPlus.setOnClickListener(v -> listener.onRepetir(item));
            h.btnDelete.setOnClickListener(v -> listener.onEliminar(item));
        }

        @Override public int getItemCount() { return lineas == null ? 0 : lineas.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio, tvNota;
            View btnEdit, btnPlus, btnDelete; // Usamos View genérico para aceptar ImageButton o Button

            ViewHolder(View v) {
                super(v);
                tvNombre = v.findViewById(R.id.tvNombreLinea);
                tvPrecio = v.findViewById(R.id.tvPrecioLinea);
                tvNota = v.findViewById(R.id.tvNotaLinea);

                // Referencias a los botones del XML item_linea_comanda.xml
                btnEdit = v.findViewById(R.id.btnItemEdit);
                btnPlus = v.findViewById(R.id.btnItemPlus);
                btnDelete = v.findViewById(R.id.btnItemDelete);
            }
        }
    }

    // 3. Adapter del Buscador
    interface OnPlatoSel { void onSelect(Plato p); }
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {
        List<Plato> p; OnPlatoSel l;
        public SearchAdapter(List<Plato> p, OnPlatoSel l) { this.p=p; this.l=l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup pa, int t) {
            View v = LayoutInflater.from(pa.getContext()).inflate(android.R.layout.simple_list_item_2, pa, false); return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Plato i = p.get(pos); h.t1.setText(i.codigo + " - " + i.nombre); h.t2.setText(i.precio + "€");
            h.itemView.setOnClickListener(v -> l.onSelect(i));
        }
        @Override public int getItemCount() { return p.size(); }
        class VH extends RecyclerView.ViewHolder { TextView t1,t2; VH(View v){super(v); t1=v.findViewById(android.R.id.text1); t2=v.findViewById(android.R.id.text2);} }
    }
}