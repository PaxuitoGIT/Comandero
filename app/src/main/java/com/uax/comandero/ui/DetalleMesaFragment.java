package com.uax.comandero.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.ItemAgrupado;
import com.uax.comandero.data.LineaComanda;
import com.uax.comandero.data.Plato;
import com.uax.comandero.utils.ComandaUtils;
import com.uax.comandero.utils.ImpresoraService;
import java.util.ArrayList;
import java.util.List;

public class DetalleMesaFragment extends Fragment {
    private int numeroMesa;
    private AppDatabase db;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean bluetoothConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                if (bluetoothConnectGranted != null && bluetoothConnectGranted) mandarACocina();
            });

    @Override public void onCreate(Bundle s) { super.onCreate(s); if (getArguments() != null) numeroMesa = getArguments().getInt("numeroMesa"); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detalle_mesa, container, false);
        db = AppDatabase.getDatabase(getContext());

        TextView tvTitulo = view.findViewById(R.id.tvTituloMesa);
        tvTitulo.setText("Mesa " + numeroMesa);

        RecyclerView recycler = view.findViewById(R.id.recyclerDetalle);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        db.dao().getComandaMesa(numeroMesa).observe(getViewLifecycleOwner(), lineas -> {
            List<ItemAgrupado> listaAgrupada = ComandaUtils.agruparLineas(lineas);
            recycler.setAdapter(new ComandaAgrupadaAdapter(listaAgrupada, new OnAgrupadoAccion() {
                @Override public void onBorrarUno(ItemAgrupado item) { confirmarBorradoUno(item); }
                @Override public void onEditar(ItemAgrupado item) { editarGrupo(item); } // NUEVO
            }));
        });

        view.findViewById(R.id.fabAddItem).setOnClickListener(v -> mostrarBuscador());
        view.findViewById(R.id.btnIrACaja).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("numeroMesa", numeroMesa);
            Navigation.findNavController(v).navigate(R.id.action_detalle_to_recibo, args);
        });
        view.findViewById(R.id.btnMandarCocina).setOnClickListener(v -> mandarACocina());
        view.findViewById(R.id.fabSimularCocina).setOnClickListener(v -> simularTicketCocina());

        return view;
    }

    private void editarGrupo(ItemAgrupado item) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputPrecio = new EditText(getContext());
        inputPrecio.setHint("Precio Unitario");
        inputPrecio.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputPrecio.setText(String.valueOf(item.precioUnitario));
        layout.addView(inputPrecio);

        final EditText inputNota = new EditText(getContext());
        inputNota.setHint("Nota (ej: Muy hecho)");
        inputNota.setText(item.notas);
        layout.addView(inputNota);

        new AlertDialog.Builder(getContext())
                .setTitle("Editar " + item.nombre)
                .setMessage("Se actualizarán " + item.cantidad + " unidades.")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    String precioStr = inputPrecio.getText().toString();
                    String nuevaNota = inputNota.getText().toString();
                    if (!precioStr.isEmpty()) {
                        double nuevoPrecio = Double.parseDouble(precioStr);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            // Actualizamos TODOS los items de este grupo
                            db.dao().actualizarLineasEnMasa(item.idsOriginales, nuevoPrecio, nuevaNota);
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarBorradoUno(ItemAgrupado item) {
        int idABorrar = item.idsOriginales.get(item.idsOriginales.size() - 1);
        new AlertDialog.Builder(getContext())
                .setTitle("¿Restar 1 " + item.nombre + "?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        LineaComanda dummy = new LineaComanda(0, "", 0, "", false, 0, false, false);
                        dummy.id = idABorrar;
                        db.dao().borrarLinea(dummy);
                    });
                }).setNegativeButton("Cancelar", null).show();
    }

    private void mandarACocina() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                return;
            }
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LineaComanda> pendientes = db.dao().getPendientesCocina(numeroMesa);
            if (pendientes.isEmpty()) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Todo enviado", Toast.LENGTH_SHORT).show());
                return;
            }
            ImpresoraService impresora = new ImpresoraService(getContext());
            impresora.imprimirTicketCocina(numeroMesa, pendientes);
            List<Integer> ids = new ArrayList<>();
            for (LineaComanda l : pendientes) ids.add(l.id);
            db.dao().marcarComoEnviados(ids);
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Enviado a Cocina", Toast.LENGTH_SHORT).show());
        });
    }

    private void simularTicketCocina() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LineaComanda> pendientes = db.dao().getPendientesCocina(numeroMesa);
            if (pendientes.isEmpty()) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Nada pendiente", Toast.LENGTH_SHORT).show());
                return;
            }
            ImpresoraService servicio = new ImpresoraService(getContext());
            String ticketRaw = servicio.getTicketCocinaBuilder(numeroMesa, pendientes);
            String textoVisual = ticketRaw.replace("[C]", "").replace("[L]", "").replace("[R]", "   ").replace("<font size='big'>", "").replace("</font>", "").replace("\n", "<br>");
            getActivity().runOnUiThread(() -> mostrarDialogoSimulacion(textoVisual));
        });
    }

    private void mostrarDialogoSimulacion(String textoHTML) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        TextView t = new TextView(getContext());
        t.setText(Html.fromHtml(textoHTML, Html.FROM_HTML_MODE_COMPACT));
        t.setPadding(50, 40, 50, 40);
        t.setTypeface(Typeface.MONOSPACE);
        t.setBackgroundColor(0xFFFFFDE7);
        t.setTextColor(0xFF000000);
        builder.setView(t).setPositiveButton("Cerrar", null).show();
    }

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
                        getActivity().runOnUiThread(() -> recyclerRes.setAdapter(new SearchAdapter(res, plato -> {
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
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        final EditText inputNota = new EditText(getContext());
        inputNota.setHint("Notas (ej: Sin cebolla)");
        layout.addView(inputNota);

        new AlertDialog.Builder(getContext())
                .setTitle(p.nombre)
                .setView(layout)
                .setPositiveButton("Añadir", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.dao().insertarLinea(new LineaComanda(
                                numeroMesa, p.nombre, p.precio,
                                inputNota.getText().toString(), false,
                                System.currentTimeMillis(),
                                p.esCocina,
                                false
                        ));
                    });
                }).show();
    }

    interface OnAgrupadoAccion { void onBorrarUno(ItemAgrupado item); void onEditar(ItemAgrupado item); }

    class ComandaAgrupadaAdapter extends RecyclerView.Adapter<ComandaAgrupadaAdapter.VH> {
        List<ItemAgrupado> list; OnAgrupadoAccion listener;
        public ComandaAgrupadaAdapter(List<ItemAgrupado> l, OnAgrupadoAccion li){this.list=l;this.listener=li;}

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            ItemAgrupado item = list.get(pos);
            if(item.cantidad > 1) h.tvNombre.setText(item.cantidad + "x " + item.nombre);
            else h.tvNombre.setText(item.nombre);
            h.tvPrecio.setText(String.format("%.2f €", item.precioTotal));
            if(item.notas != null && !item.notas.isEmpty()) {
                h.tvNota.setText(item.notas);
                h.tvNota.setVisibility(View.VISIBLE);
            } else h.tvNota.setVisibility(View.GONE);

            h.btnDelete.setOnClickListener(v -> listener.onBorrarUno(item));

            // HABILITAMOS EL BOTÓN EDITAR
            h.btnEdit.setVisibility(View.VISIBLE);
            h.btnEdit.setOnClickListener(v -> listener.onEditar(item));

            h.btnPlus.setVisibility(View.GONE);
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio, tvNota; View btnDelete, btnEdit, btnPlus;
            VH(View v){super(v);
                tvNombre=v.findViewById(R.id.tvNombreLinea);
                tvPrecio=v.findViewById(R.id.tvPrecioLinea);
                tvNota=v.findViewById(R.id.tvNotaLinea);
                btnDelete=v.findViewById(R.id.btnItemDelete);
                btnEdit=v.findViewById(R.id.btnItemEdit);
                btnPlus=v.findViewById(R.id.btnItemPlus);
            }
        }
    }

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