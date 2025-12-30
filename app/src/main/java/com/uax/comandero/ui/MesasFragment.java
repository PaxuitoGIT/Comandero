package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Mesa;
import java.util.List;

public class MesasFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mesas, container, false);
        RecyclerView recycler = view.findViewById(R.id.recyclerMesas);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        AppDatabase db = AppDatabase.getDatabase(getContext());

        // AHORA OBSERVAMOS LA TABLA 'MESAS'
        db.dao().getMesasActivas().observe(getViewLifecycleOwner(), mesas -> {
            recycler.setAdapter(new MesasAdapter(mesas));
        });

        // BOTÓN CREAR MESA
        view.findViewById(R.id.fabAddMesa).setOnClickListener(v -> {
            EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Número de mesa (ej: 5)");

            new AlertDialog.Builder(getContext())
                    .setTitle("Abrir Nueva Mesa")
                    .setView(input)
                    .setPositiveButton("Abrir", (d, w) -> {
                        String text = input.getText().toString();
                        if (!text.isEmpty()) {
                            int numMesa = Integer.parseInt(text);
                            // 1. Guardamos la mesa en BD
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.dao().abrirMesa(new Mesa(numMesa, true, System.currentTimeMillis()));

                                // 2. Navegamos inmediatamente
                                getActivity().runOnUiThread(() -> irAMesa(numMesa));
                            });
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        return view;
    }

    // MÉTODO PARA NAVEGAR LLEVANDO EL ID (EL MENÚ SE CARGARÁ SOLO)
    private void irAMesa(int numeroMesa) {
        Bundle args = new Bundle();
        args.putInt("numeroMesa", numeroMesa);

        // Verificamos que la vista siga existiendo antes de navegar
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.action_mesas_to_detalle, args);
        }
    }

    // ADAPTER
    class MesasAdapter extends RecyclerView.Adapter<MesasAdapter.ViewHolder> {
        private List<Mesa> lista;
        public MesasAdapter(List<Mesa> l) { this.lista = l; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usamos un layout simple grande
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Mesa m = lista.get(position);
            holder.text.setText("MESA " + m.numero);
            holder.text.setTextSize(30);
            holder.text.setPadding(40,40,40,40);

            // AL PULSAR EN LA LISTA, VAMOS A ESA MESA CON SUS DATOS
            holder.itemView.setOnClickListener(v -> irAMesa(m.numero));
        }

        @Override public int getItemCount() { return lista == null ? 0 : lista.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }
    }
}