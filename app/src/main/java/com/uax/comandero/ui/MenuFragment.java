package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Plato;
import java.util.List;

public class MenuFragment extends Fragment {

    private AppDatabase db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        db = AppDatabase.getDatabase(getContext());

        RecyclerView recycler = view.findViewById(R.id.recyclerMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        db.dao().getMenu().observe(getViewLifecycleOwner(), platos -> {
            recycler.setAdapter(new MenuAdapter(platos));
        });

        view.findViewById(R.id.fabAddPlato).setOnClickListener(v -> mostrarDialogoPlato(null));

        return view;
    }

    private void mostrarDialogoPlato(Plato platoEditar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_plato, null);

        EditText etCodigo = view.findViewById(R.id.etCodigoPlato);
        EditText etNombre = view.findViewById(R.id.etNombrePlato);
        EditText etPrecio = view.findViewById(R.id.etPrecioPlato);
        CheckBox chkCocina = view.findViewById(R.id.chkEsCocina); // NUEVO

        if (platoEditar != null) {
            etCodigo.setText(platoEditar.codigo);
            etCodigo.setEnabled(false);
            etNombre.setText(platoEditar.nombre);
            etPrecio.setText(String.valueOf(platoEditar.precio));
            chkCocina.setChecked(platoEditar.esCocina); // CARGAR ESTADO
        }

        builder.setView(view)
                .setTitle(platoEditar == null ? "Nuevo Plato" : "Editar Plato")
                .setPositiveButton("Guardar", (d, w) -> {
                    String codigo = etCodigo.getText().toString();
                    String nombre = etNombre.getText().toString();
                    String precioStr = etPrecio.getText().toString();
                    boolean esCocina = chkCocina.isChecked(); // LEER ESTADO

                    if (codigo.isEmpty() || nombre.isEmpty() || precioStr.isEmpty()) return;

                    double precio = Double.parseDouble(precioStr);

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (platoEditar == null) {
                            if (db.dao().contarPlatosPorCodigo(codigo) > 0) {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Código ya existe", Toast.LENGTH_SHORT).show()
                                );
                            } else {
                                // GUARDAR CON CAMPO DE COCINA
                                db.dao().insertarPlato(new Plato(codigo, nombre, precio, esCocina));
                            }
                        } else {
                            platoEditar.nombre = nombre;
                            platoEditar.precio = precio;
                            platoEditar.esCocina = esCocina; // ACTUALIZAR
                            db.dao().actualizarPlato(platoEditar);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Adapter sencillo para Menú
    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        List<Plato> l; MenuAdapter(List<Plato> l){this.l=l;}
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_plato, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Plato p = l.get(pos);
            h.t1.setText(p.codigo + " - " + p.nombre);
            h.t2.setText(p.precio + " €");
            // Mostrar icono si es de cocina (opcional, concatenado al nombre)
            if(p.esCocina) h.t1.append(" 🍳");

            h.btnEdit.setOnClickListener(v-> mostrarDialogoPlato(p));
        }
        @Override public int getItemCount() { return l.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2; ImageButton btnEdit;
            ViewHolder(View v){super(v); t1=v.findViewById(R.id.tvNombrePlato); t2=v.findViewById(R.id.tvPrecioPlato); btnEdit=v.findViewById(R.id.btnEditPlato);}
        }
    }
}