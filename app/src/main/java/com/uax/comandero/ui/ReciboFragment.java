package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
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
import com.uax.comandero.utils.ImpresoraService;
import java.util.List;

public class ReciboFragment extends Fragment {
    private int numeroMesa;
    private long fechaTicket = -1;
    private List<LineaComanda> listaActual;

    // VARIABLES PARA DESCUENTO
    private boolean descuentoAplicado = false;
    private double totalFinal = 0.0;

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
        Button btnDesc = view.findViewById(R.id.btnDescuento); // NUEVO BOTÓN

        View btnImprimir = view.findViewById(R.id.fabImprimir);
        View btnSimular = view.findViewById(R.id.fabSimular);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Cargar datos
        LiveData<List<LineaComanda>> datos;
        if (fechaTicket == -1) {
            datos = db.dao().getComandaMesa(numeroMesa);
            btnConf.setVisibility(View.VISIBLE);
            btnDesc.setVisibility(View.VISIBLE); // Solo permitimos descuento antes de pagar
        } else {
            datos = db.dao().getReciboPasado(numeroMesa, fechaTicket);
            btnConf.setVisibility(View.GONE);
            btnDesc.setVisibility(View.GONE);
        }

        datos.observe(getViewLifecycleOwner(), lineas -> {
            this.listaActual = lineas;
            recycler.setAdapter(new ReciboAdapter(lineas));
            recalcularTotal(tvTotal);
        });

        // --- LÓGICA DESCUENTO MANUAL ---
        btnDesc.setOnClickListener(v -> {
            descuentoAplicado = !descuentoAplicado; // Alternar estado
            if(descuentoAplicado){
                btnDesc.setText("Quitar Descuento");
                btnDesc.setBackgroundColor(0xFFE91E63); // Rojo para indicar activo
            } else {
                btnDesc.setText("Aplicar Descuento 10%");
                btnDesc.setBackgroundColor(0xFF2196F3); // Azul normal
            }
            recalcularTotal(tvTotal);
        });

        // Cobrar
        btnConf.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.dao().cobrarLineasMesa(numeroMesa, System.currentTimeMillis());
                db.dao().cerrarEstadoMesa(numeroMesa);
                getActivity().runOnUiThread(() -> {
                    Navigation.findNavController(v).navigate(R.id.action_recibo_to_mesas);
                });
            });
        });

        // Imprimir Real
        if (btnImprimir != null) {
            btnImprimir.setOnClickListener(v -> {
                if (listaActual != null && !listaActual.isEmpty()) {
                    ImpresoraService impresora = new ImpresoraService(getContext());
                    // Pasamos el totalFinal que ya incluye el descuento si aplica
                    impresora.imprimirTicket(numeroMesa, listaActual, totalFinal);
                }
            });
        }

        // Simular
        if (btnSimular != null) {
            btnSimular.setOnClickListener(v -> mostrarSimulacion());
        }

        return view;
    }

    private void recalcularTotal(TextView tv) {
        double subtotal = 0;
        if (listaActual != null) {
            for (LineaComanda l : listaActual) subtotal += l.precio;
        }

        if (descuentoAplicado) {
            totalFinal = subtotal * 0.90; // 10% descuento
            tv.setText(String.format("%.2f € (DTO -10%%)", totalFinal));
        } else {
            totalFinal = subtotal;
            tv.setText(String.format("%.2f €", totalFinal));
        }
    }

    private void mostrarSimulacion() {
        ImpresoraService servicio = new ImpresoraService(getContext());
        // Usamos totalFinal para que en la simulación salga el precio con descuento
        String ticketRaw = servicio.getTicketBuilder(numeroMesa, listaActual, totalFinal);

        String textoVisual = ticketRaw
                .replace("[C]", "")
                .replace("[L]", "")
                .replace("[R]", "   ")
                .replace("<font size='big'>", "")
                .replace("</font>", "")
                .replace("\n", "<br>");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        TextView t = new TextView(getContext());
        t.setText(Html.fromHtml(textoVisual, Html.FROM_HTML_MODE_COMPACT));
        t.setPadding(50, 40, 50, 40);
        t.setTypeface(Typeface.MONOSPACE);
        t.setBackgroundColor(0xFFFFFDE7);
        t.setTextColor(0xFF000000);
        builder.setView(t).setPositiveButton("Cerrar", null).show();
    }

    class ReciboAdapter extends RecyclerView.Adapter<ReciboAdapter.VH> {
        List<LineaComanda> l; ReciboAdapter(List<LineaComanda> l){this.l=l;}
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);
            v.findViewById(R.id.btnItemEdit).setVisibility(View.GONE);
            v.findViewById(R.id.btnItemPlus).setVisibility(View.GONE);
            v.findViewById(R.id.btnItemDelete).setVisibility(View.GONE);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            LineaComanda i = l.get(pos);
            TextView t1 = h.itemView.findViewById(R.id.tvNombreLinea);
            TextView t2 = h.itemView.findViewById(R.id.tvPrecioLinea);
            t1.setText(i.nombrePlato);
            t2.setText(i.precio + " €");
        }
        @Override public int getItemCount() { return l==null?0:l.size(); }
        class VH extends RecyclerView.ViewHolder{VH(View v){super(v);}}
    }
}