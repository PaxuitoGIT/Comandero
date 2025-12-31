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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.LineaComanda;
import com.uax.comandero.utils.ImpresoraService; // Asegúrate de tener esta clase creada
import java.util.List;

public class ReciboFragment extends Fragment {
    private int numeroMesa;
    private long fechaTicket = -1; // -1 significa "Mesa Actual", otro valor es "Historial"

    // Variable para guardar los datos cargados y poder imprimirlos
    private List<LineaComanda> listaActual;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        if (getArguments() != null) {
            numeroMesa = getArguments().getInt("numeroMesa");
            fechaTicket = getArguments().getLong("fechaTicket", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup c, Bundle s) {
        View view = inflater.inflate(R.layout.fragment_recibo, c, false);
        AppDatabase db = AppDatabase.getDatabase(getContext());

        RecyclerView recycler = view.findViewById(R.id.recyclerRecibo);
        TextView tvTotal = view.findViewById(R.id.tvTotalRecibo);
        Button btnConf = view.findViewById(R.id.btnConfirmarPago);

        // Referencia al botón flotante de imprimir (asegúrate de haberlo puesto en el XML)
        View btnImprimir = view.findViewById(R.id.fabImprimir);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. DECIDIMOS QUÉ DATOS CARGAR
        LiveData<List<LineaComanda>> datos;

        if (fechaTicket == -1) {
            // MODO COBRO (Mesa activa)
            datos = db.dao().getComandaMesa(numeroMesa);
            btnConf.setVisibility(View.VISIBLE); // Botón visible para cobrar
        } else {
            // MODO HISTORIAL (Solo lectura)
            datos = db.dao().getReciboPasado(numeroMesa, fechaTicket);
            btnConf.setVisibility(View.GONE); // Ocultamos el botón porque ya está pagado
        }

        // 2. OBSERVAMOS DATOS Y ACTUALIZAMOS UI
        datos.observe(getViewLifecycleOwner(), lineas -> {
            this.listaActual = lineas; // Guardamos la lista para poder imprimirla luego

            recycler.setAdapter(new ReciboAdapter(lineas));

            double total = 0;
            if (lineas != null) {
                for (LineaComanda l : lineas) total += l.precio;
            }
            tvTotal.setText(String.format("%.2f €", total));
        });

        // 3. LÓGICA DEL BOTÓN DE COBRO (Cerrar mesa y guardar)
        btnConf.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                // Cobramos usando la fecha/hora actual
                db.dao().cobrarLineasMesa(numeroMesa, System.currentTimeMillis());

                // Cerramos la mesa para que desaparezca de la lista principal
                db.dao().cerrarEstadoMesa(numeroMesa);

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Cobrado y Guardado", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_recibo_to_mesas);
                });
            });
        });

        // 4. LÓGICA DEL BOTÓN DE IMPRIMIR (Nuevo)
        if (btnImprimir != null) {
            btnImprimir.setOnClickListener(v -> {
                if (listaActual != null && !listaActual.isEmpty()) {
                    // Calculamos el total al vuelo
                    double total = 0;
                    for (LineaComanda l : listaActual) total += l.precio;

                    // Llamamos al servicio de impresión
                    ImpresoraService impresora = new ImpresoraService(getContext());
                    impresora.imprimirTicket(numeroMesa, listaActual, total);
                } else {
                    Toast.makeText(getContext(), "No hay datos para imprimir", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    // 5. ADAPTER INTERNO
    class ReciboAdapter extends RecyclerView.Adapter<ReciboAdapter.VH> {
        List<LineaComanda> l;
        public ReciboAdapter(List<LineaComanda> l){this.l=l;}

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);

            // IMPORTANTE: Ocultamos los botones de edición porque esto es un recibo
            View btnEdit = v.findViewById(R.id.btnItemEdit);
            View btnPlus = v.findViewById(R.id.btnItemPlus);
            View btnDelete = v.findViewById(R.id.btnItemDelete);

            if(btnEdit != null) btnEdit.setVisibility(View.GONE);
            if(btnPlus != null) btnPlus.setVisibility(View.GONE);
            if(btnDelete != null) btnDelete.setVisibility(View.GONE);

            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            LineaComanda i = l.get(pos);
            TextView t1 = h.itemView.findViewById(R.id.tvNombreLinea);
            TextView t2 = h.itemView.findViewById(R.id.tvPrecioLinea);
            TextView tNota = h.itemView.findViewById(R.id.tvNotaLinea);

            t1.setText(i.nombrePlato);
            t2.setText(i.precio + " €");

            if (i.notas != null && !i.notas.isEmpty()) {
                tNota.setText(i.notas);
                tNota.setVisibility(View.VISIBLE);
            } else {
                tNota.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return l==null?0:l.size(); }

        class VH extends RecyclerView.ViewHolder{VH(View v){super(v);}}
    }
}