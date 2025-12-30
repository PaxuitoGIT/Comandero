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
import java.util.List;

public class ReciboFragment extends Fragment {
    private int numeroMesa;
    private long fechaTicket = -1; // -1 significa "Mesa Actual", otro valor es "Historial"

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

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // DECIDIMOS QUÉ DATOS CARGAR
        LiveData<List<LineaComanda>> datos;

        if (fechaTicket == -1) {
            // MODO COBRO (Mesa activa)
            datos = db.dao().getComandaMesa(numeroMesa);
            btnConf.setVisibility(View.VISIBLE); // Botón visible para cobrar
        } else {
            // MODO HISTORIAL (Solo lectura)
            datos = db.dao().getReciboPasado(numeroMesa, fechaTicket);
            btnConf.setVisibility(View.GONE); // Ocultamos el botón porque ya está pagado

            // Opcional: Cambiar título
            // tvTitulo.setText("Historial Mesa " + numeroMesa);
        }

        // Observamos los datos (el adapter es el mismo para ambos casos)
        datos.observe(getViewLifecycleOwner(), lineas -> {
            recycler.setAdapter(new ReciboAdapter(lineas));
            double total = 0;
            for (LineaComanda l : lineas) total += l.precio;
            tvTotal.setText(String.format("%.2f €", total));
        });

        // LÓGICA DEL BOTÓN DE COBRO (Solo funciona si es mesa activa)
        btnConf.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                // ACTUALIZAMOS EL DAO AQUÍ: Pasamos System.currentTimeMillis()
                db.dao().cobrarLineasMesa(numeroMesa, System.currentTimeMillis());

                db.dao().cerrarEstadoMesa(numeroMesa); // Cerramos mesa en tabla 'mesas'

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Cobrado y Guardado", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).navigate(R.id.action_recibo_to_mesas);
                });
            });
        });

        return view;
    }

    // Adapter simple interno (puedes reusar el de DetalleMesa si quieres)
    class ReciboAdapter extends RecyclerView.Adapter<ReciboAdapter.VH> {
        List<LineaComanda> l; ReciboAdapter(List<LineaComanda> l){this.l=l;}
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_linea_comanda, p, false);
            // Ocultamos los botones de editar/borrar porque en el recibo es solo lectura
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