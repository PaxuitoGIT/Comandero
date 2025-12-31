package com.uax.comandero.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface; // Necesario para la fuente del ticket
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html; // Necesario para el formato HTML del ticket
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import com.uax.comandero.data.LineaComanda;
import com.uax.comandero.data.Plato;
import com.uax.comandero.utils.ImpresoraService;

import java.util.ArrayList;
import java.util.List;

public class DetalleMesaFragment extends Fragment {
    private int numeroMesa;
    private AppDatabase db;

    // --- 1. GESTOR DE RESULTADO DE PERMISOS ---
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean bluetoothConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);

                if (bluetoothConnectGranted != null && bluetoothConnectGranted) {
                    // Si aceptó, reintentamos mandar a cocina
                    mandarACocina();
                } else {
                    Toast.makeText(getContext(), "Se necesitan permisos para imprimir", Toast.LENGTH_SHORT).show();
                }
            });

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

        // Configuración del Adapter
        db.dao().getComandaMesa(numeroMesa).observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ComandaAdapter(lineas, new OnComandaAccion() {
                @Override public void onEditar(LineaComanda linea) { editarLinea(linea); }
                @Override public void onRepetir(LineaComanda linea) { repetirLinea(linea); }
                @Override public void onEliminar(LineaComanda linea) { confirmarBorrado(linea); }
            }));
        });

        // Botón Buscar (+)
        view.findViewById(R.id.fabAddItem).setOnClickListener(v -> mostrarBuscador());

        // Botón Ir a Caja
        view.findViewById(R.id.btnIrACaja).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("numeroMesa", numeroMesa);
            Navigation.findNavController(v).navigate(R.id.action_detalle_to_recibo, args);
        });

        // Botón Mandar a Cocina (Impresión Real)
        View btnCocina = view.findViewById(R.id.btnMandarCocina);
        if (btnCocina != null) {
            btnCocina.setOnClickListener(v -> mandarACocina());
        }

        // NUEVO: Botón Ojo (Simular Cocina)
        View btnSimular = view.findViewById(R.id.fabSimularCocina);
        if (btnSimular != null) {
            btnSimular.setOnClickListener(v -> simularTicketCocina());
        }

        return view;
    }

    // ==========================================
    // LÓGICA DE COCINA (IMPRESIÓN REAL)
    // ==========================================

    private void mandarACocina() {
        // Chequeo de permisos Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                requestPermissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                });
                return;
            }
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<LineaComanda> pendientes = db.dao().getPendientesCocina(numeroMesa);

            if (pendientes.isEmpty()) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Todo está enviado a cocina", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // Imprimir
            ImpresoraService impresora = new ImpresoraService(getContext());
            impresora.imprimirTicketCocina(numeroMesa, pendientes);

            // Marcar como enviados
            List<Integer> idsActualizados = new ArrayList<>();
            for (LineaComanda l : pendientes) idsActualizados.add(l.id);
            db.dao().marcarComoEnviados(idsActualizados);

            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Enviado a Cocina (" + pendientes.size() + " platos)", Toast.LENGTH_SHORT).show()
            );
        });
    }

    // ==========================================
    // LÓGICA DE SIMULACIÓN (VISTA PREVIA)
    // ==========================================

    private void simularTicketCocina() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Buscamos lo que ESTÁ pendiente (sin marcarlo como enviado)
            List<LineaComanda> pendientes = db.dao().getPendientesCocina(numeroMesa);

            if (pendientes.isEmpty()) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "No hay nada nuevo para Cocina", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // Generamos el texto del ticket
            ImpresoraService servicio = new ImpresoraService(getContext());
            String ticketRaw = servicio.getTicketCocinaBuilder(numeroMesa, pendientes);

            // Limpieza visual para mostrar en el móvil
            String textoVisual = ticketRaw
                    .replace("[C]", "")
                    .replace("[L]", "")
                    .replace("[R]", "   ")
                    .replace("<font size='big'>", "")
                    .replace("</font>", "")
                    .replace("\n", "<br>");

            getActivity().runOnUiThread(() -> mostrarDialogoSimulacion(textoVisual));
        });
    }

    private void mostrarDialogoSimulacion(String textoHTML) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Vista Previa COCINA");

        TextView textoView = new TextView(getContext());
        textoView.setText(Html.fromHtml(textoHTML, Html.FROM_HTML_MODE_COMPACT));
        textoView.setPadding(50, 40, 50, 40);

        // Estilo visual de ticket
        textoView.setTypeface(Typeface.MONOSPACE);
        textoView.setTextSize(16);
        textoView.setBackgroundColor(0xFFFFFDE7); // Amarillo claro
        textoView.setTextColor(0xFF000000);

        builder.setView(textoView);
        builder.setPositiveButton("Cerrar (No enviado)", null);
        builder.show();
    }

    // ==========================================
    // ACCIONES DE ITEM
    // ==========================================

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
                        AppDatabase.databaseWriteExecutor.execute(() -> db.dao().actualizarLinea(linea));
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
                    System.currentTimeMillis(),
                    original.esCocina, // Hereda si es cocina
                    false // Nuevo item, NO enviado
            ));
        });
        Toast.makeText(getContext(), "Añadido +1 " + original.nombrePlato, Toast.LENGTH_SHORT).show();
    }

    private void confirmarBorrado(LineaComanda linea) {
        new AlertDialog.Builder(getContext())
                .setTitle("¿Eliminar?")
                .setMessage("Vas a borrar: " + linea.nombrePlato)
                .setPositiveButton("Eliminar", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> db.dao().borrarLinea(linea));
                    Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================
    // BUSCADOR Y AÑADIR
    // ==========================================

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
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputNota = new EditText(getContext());
        inputNota.setHint("Notas (ej: Sin cebolla)");
        layout.addView(inputNota);

        final CheckBox checkCocina = new CheckBox(getContext());
        checkCocina.setText("Enviar a Cocina");
        checkCocina.setChecked(true); // Por defecto activado
        layout.addView(checkCocina);

        new AlertDialog.Builder(getContext())
                .setTitle(p.nombre)
                .setView(layout)
                .setPositiveButton("Añadir", (d, w) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.dao().insertarLinea(new LineaComanda(
                                numeroMesa, p.nombre, p.precio,
                                inputNota.getText().toString(), false,
                                System.currentTimeMillis(),
                                checkCocina.isChecked(),
                                false
                        ));
                    });
                })
                .show();
    }

    // ==========================================
    // ADAPTERS
    // ==========================================

    interface OnComandaAccion {
        void onEditar(LineaComanda linea);
        void onRepetir(LineaComanda linea);
        void onEliminar(LineaComanda linea);
    }

    class ComandaAdapter extends RecyclerView.Adapter<ComandaAdapter.ViewHolder> {
        List<LineaComanda> lineas;
        OnComandaAccion listener;

        public ComandaAdapter(List<LineaComanda> l, OnComandaAccion listener) { this.lineas = l; this.listener = listener; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            LineaComanda item = lineas.get(pos);
            h.tvNombre.setText(item.nombrePlato);
            h.tvPrecio.setText(item.precio + " €");
            h.tvNota.setText(item.notas != null ? item.notas : "");
            h.tvNota.setVisibility(item.notas != null && !item.notas.isEmpty() ? View.VISIBLE : View.GONE);

            h.btnEdit.setOnClickListener(v -> listener.onEditar(item));
            h.btnPlus.setOnClickListener(v -> listener.onRepetir(item));
            h.btnDelete.setOnClickListener(v -> listener.onEliminar(item));
        }

        @Override public int getItemCount() { return lineas == null ? 0 : lineas.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre, tvPrecio, tvNota;
            View btnEdit, btnPlus, btnDelete;
            ViewHolder(View v) {
                super(v);
                tvNombre = v.findViewById(R.id.tvNombreLinea);
                tvPrecio = v.findViewById(R.id.tvPrecioLinea);
                tvNota = v.findViewById(R.id.tvNotaLinea);
                btnEdit = v.findViewById(R.id.btnItemEdit);
                btnPlus = v.findViewById(R.id.btnItemPlus);
                btnDelete = v.findViewById(R.id.btnItemDelete);
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