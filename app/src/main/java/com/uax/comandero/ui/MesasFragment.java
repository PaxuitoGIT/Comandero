package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import java.util.List;

public class MesasFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mesas, container, false);
        RecyclerView recycler = view.findViewById(R.id.recyclerMesas);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columnas

        AppDatabase.getDatabase(getContext()).dao().getMesasAbiertas().observe(getViewLifecycleOwner(), mesas -> {
            recycler.setAdapter(new MesasAdapter(mesas));
        });

        view.findViewById(R.id.fabAddMesa).setOnClickListener(v -> {
            EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Número de mesa");

            new AlertDialog.Builder(getContext())
                    .setTitle("Abrir Mesa")
                    .setView(input)
                    .setPositiveButton("Abrir", (d, w) -> {
                        String num = input.getText().toString();
                        if (!num.isEmpty()) irAMesa(Integer.parseInt(num));
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        return view;
    }

    private void irAMesa(int numeroMesa) {
        Bundle args = new Bundle();
        args.putInt("numeroMesa", numeroMesa);
        // Asegurate de que esta accion existe en nav_graph
        Navigation.findNavController(getView()).navigate(R.id.action_mesas_to_detalle, args);
    }

    class MesasAdapter extends RecyclerView.Adapter<MesasAdapter.ViewHolder> {
        private List<Integer> mesas;
        public MesasAdapter(List<Integer> mesas) { this.mesas = mesas; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usamos un layout de tarjeta simple
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int num = mesas.get(position);
            holder.text.setText("MESA " + num);
            holder.text.setTextSize(24);
            holder.text.setPadding(30,30,30,30);
            holder.itemView.setOnClickListener(v -> irAMesa(num));
        }

        @Override public int getItemCount() { return mesas == null ? 0 : mesas.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }
    }
}
