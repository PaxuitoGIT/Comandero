package com.uax.comandero.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.uax.comandero.R;
import com.uax.comandero.data.AppDatabase;
import com.uax.comandero.data.Plato;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {

    private AppDatabase db;
    private boolean esAdmin = false;
    private List<Plato> listaCompleta = new ArrayList<>();
    private MenuAdapter adapter;

    // Categorías (Añadimos "Todas" al principio para el filtro)
    private final String[] CATEGORIAS_FILTRO = {"Todas", "Entrantes", "Principales", "Postres", "Bebidas", "Otros"};
    // Categorías para crear plato (Sin "Todas")
    private final String[] CATEGORIAS_CREAR = {"Entrantes", "Principales", "Postres", "Bebidas", "Otros"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        db = AppDatabase.getDatabase(getContext());

        // Roles
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FloatingActionButton fab = view.findViewById(R.id.fabAddPlato);
        if (user != null && (user.getEmail().equals("admin@uax.com") || user.getEmail().startsWith("admin"))) {
            esAdmin = true;
            fab.setVisibility(View.VISIBLE);
        } else {
            esAdmin = false;
            fab.setVisibility(View.GONE);
        }

        // Configurar RecyclerView
        RecyclerView recycler = view.findViewById(R.id.recyclerMenu);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MenuAdapter(new ArrayList<>());
        recycler.setAdapter(adapter);

        // Configurar Filtro
        Spinner spFiltro = view.findViewById(R.id.spFiltroCategoria);
        ArrayAdapter<String> adapterFiltro = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, CATEGORIAS_FILTRO);
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFiltro.setAdapter(adapterFiltro);

        spFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtrarLista(CATEGORIAS_FILTRO[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Cargar datos
        db.dao().getMenu().observe(getViewLifecycleOwner(), platos -> {
            listaCompleta = platos;
            filtrarLista(spFiltro.getSelectedItem().toString());
        });

        fab.setOnClickListener(v -> mostrarDialogoPlato(null));

        return view;
    }

    private void filtrarLista(String categoria) {
        if (listaCompleta == null) return;
        List<Plato> filtrada = new ArrayList<>();
        if (categoria.equals("Todas")) {
            filtrada.addAll(listaCompleta);
        } else {
            for (Plato p : listaCompleta) {
                if (p.categoria != null && p.categoria.equals(categoria)) {
                    filtrada.add(p);
                }
            }
        }
        adapter.setPlatos(filtrada);
    }

    private void mostrarDialogoPlato(Plato platoEditar) {
        if (!esAdmin) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_plato, null);

        EditText etCodigo = view.findViewById(R.id.etCodigoPlato);
        EditText etNombre = view.findViewById(R.id.etNombrePlato);
        EditText etPrecio = view.findViewById(R.id.etPrecioPlato);
        CheckBox chkCocina = view.findViewById(R.id.chkEsCocina);
        Spinner spCategoria = view.findViewById(R.id.spCategoria);

        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, CATEGORIAS_CREAR);
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(adapterCat);

        if (platoEditar != null) {
            etCodigo.setText(platoEditar.codigo);
            etCodigo.setEnabled(false);
            etNombre.setText(platoEditar.nombre);
            etPrecio.setText(String.valueOf(platoEditar.precio));
            chkCocina.setChecked(platoEditar.esCocina);
            for (int i = 0; i < CATEGORIAS_CREAR.length; i++) {
                if (CATEGORIAS_CREAR[i].equals(platoEditar.categoria)) {
                    spCategoria.setSelection(i);
                    break;
                }
            }

            // BOTÓN ELIMINAR (Solo si estamos editando)
            builder.setNeutralButton("Eliminar Plato", (d, w) -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("¿Borrar definitivamente?")
                        .setMessage("Se eliminará " + platoEditar.nombre + " del menú.")
                        .setPositiveButton("Sí, borrar", (dialog, which) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> db.dao().borrarPlato(platoEditar));
                            Toast.makeText(getContext(), "Plato eliminado", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }

        builder.setView(view)
                .setTitle(platoEditar == null ? "Nuevo Plato" : "Editar Plato")
                .setPositiveButton("Guardar", (d, w) -> {
                    String codigo = etCodigo.getText().toString();
                    String nombre = etNombre.getText().toString();
                    String precioStr = etPrecio.getText().toString();
                    boolean esCocina = chkCocina.isChecked();
                    String cat = spCategoria.getSelectedItem().toString();

                    if (codigo.isEmpty() || nombre.isEmpty() || precioStr.isEmpty()) return;
                    double precio = Double.parseDouble(precioStr);

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (platoEditar == null) {
                            if (db.dao().contarPlatosPorCodigo(codigo) > 0) {
                                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Código repetido", Toast.LENGTH_SHORT).show());
                            } else {
                                db.dao().insertarPlato(new Plato(codigo, nombre, precio, esCocina, cat));
                            }
                        } else {
                            platoEditar.nombre = nombre;
                            platoEditar.precio = precio;
                            platoEditar.esCocina = esCocina;
                            platoEditar.categoria = cat;
                            db.dao().actualizarPlato(platoEditar);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        List<Plato> l;
        MenuAdapter(List<Plato> l){this.l=l;}

        void setPlatos(List<Plato> nuevos) {
            this.l = nuevos;
            notifyDataSetChanged();
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_plato, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Plato p = l.get(pos);
            // Muestra ID DB y Código
            h.t1.setText("#" + p.id + " [" + p.codigo + "] " + p.nombre);

            String info = String.format("%.2f € • %s", p.precio, (p.categoria!=null?p.categoria:"-"));
            if(p.esCocina) info += " 🍳";
            h.t2.setText(info);

            h.btnEdit.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
            h.btnEdit.setOnClickListener(v-> mostrarDialogoPlato(p));
        }
        @Override public int getItemCount() { return l.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2; ImageButton btnEdit;
            ViewHolder(View v){super(v); t1=v.findViewById(R.id.tvNombrePlato); t2=v.findViewById(R.id.tvPrecioPlato); btnEdit=v.findViewById(R.id.btnEditPlato);}
        }
    }
}