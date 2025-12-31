package com.uax.comandero.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Ticket;
import java.util.Calendar;
import java.util.List;

public class HistorialFragment extends Fragment {

    private RecyclerView recycler;
    private TextView tvFiltro;
    private ImageButton btnBorrar;
    private AppDatabase db;

    // Guardamos la referencia para poder quitar el observer anterior al cambiar de filtro
    private LiveData<List<Ticket>> datosActuales;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        recycler = view.findViewById(R.id.recyclerHistorial);
        tvFiltro = view.findViewById(R.id.tvFiltroActual);
        btnBorrar = view.findViewById(R.id.btnBorrarFiltro);
        ImageButton btnCalendario = view.findViewById(R.id.btnFiltrarFecha);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        db = AppDatabase.getDatabase(getContext());

        // 1. Cargar todo por defecto al entrar
        cargarDatos(null, null);

        // 2. Acción Calendario
        btnCalendario.setOnClickListener(v -> mostrarCalendario());

        // 3. Acción Borrar Filtro
        btnBorrar.setOnClickListener(v -> {
            cargarDatos(null, null); // Null significa "Traeme todo"
            btnBorrar.setVisibility(View.GONE);
            tvFiltro.setText("Mostrando: Todo");
        });

        return view;
    }

    private void mostrarCalendario() {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            // El usuario eligió una fecha.
            // Necesitamos calcular el inicio (00:00) y fin (23:59) de ese día en milisegundos.

            Calendar inicio = Calendar.getInstance();
            inicio.set(year, month, dayOfMonth, 0, 0, 0);
            inicio.set(Calendar.MILLISECOND, 0);

            Calendar fin = Calendar.getInstance();
            fin.set(year, month, dayOfMonth, 23, 59, 59);
            fin.set(Calendar.MILLISECOND, 999);

            // Actualizamos la UI
            String fechaTexto = dayOfMonth + "/" + (month + 1) + "/" + year;
            tvFiltro.setText("Día: " + fechaTexto);
            btnBorrar.setVisibility(View.VISIBLE);

            // Cargamos la BBDD con el rango
            cargarDatos(inicio.getTimeInMillis(), fin.getTimeInMillis());

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    // Método centralizado para cambiar qué datos observamos
    private void cargarDatos(Long desde, Long hasta) {
        // 1. Si ya estamos observando algo, dejamos de observarlo para no mezclar datos
        if (datosActuales != null) {
            datosActuales.removeObservers(getViewLifecycleOwner());
        }

        // 2. Decidimos qué consulta hacer
        if (desde == null || hasta == null) {
            datosActuales = db.dao().getHistorialAgrupado(); // Todos
        } else {
            datosActuales = db.dao().getHistorialPorFecha(desde, hasta); // Filtrado
        }

        // 3. Observamos la nueva fuente de datos
        datosActuales.observe(getViewLifecycleOwner(), tickets -> {
            recycler.setAdapter(new TicketAdapter(tickets));
        });
    }

    // --- ADAPTER ---
    class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
        List<Ticket> lista;
        public TicketAdapter(List<Ticket> l) { this.lista = l; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Ticket t = lista.get(pos);
            CharSequence fechaStr = DateFormat.format("dd/MM/yyyy HH:mm", t.fecha);

            h.t1.setText("Mesa " + t.numeroMesa + " - " + String.format("%.2f €", t.total));
            h.t2.setText(fechaStr);

            h.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("numeroMesa", t.numeroMesa);
                args.putLong("fechaTicket", t.fecha);
                Navigation.findNavController(v).navigate(R.id.action_historial_to_recibo, args);
            });
        }
        @Override public int getItemCount() { return lista == null ? 0 : lista.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            ViewHolder(View v) { super(v); t1 = v.findViewById(android.R.id.text1); t2 = v.findViewById(android.R.id.text2); }
        }
    }
}