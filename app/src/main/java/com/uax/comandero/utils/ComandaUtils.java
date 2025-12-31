package com.uax.comandero.utils;

import com.uax.comandero.data.ItemAgrupado;
import com.uax.comandero.data.LineaComanda;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComandaUtils {
    public static List<ItemAgrupado> agruparLineas(List<LineaComanda> listaOriginal) {
        // LinkedHashMap mantiene el orden en que se pidieron
        Map<String, ItemAgrupado> mapa = new LinkedHashMap<>();

        for (LineaComanda linea : listaOriginal) {
            // La clave única es Nombre + Notas + Precio.
            // Si la nota cambia (ej: una con hielo y otra sin), no se agrupan.
            String notaSafe = (linea.notas != null) ? linea.notas : "";
            String clave = linea.nombrePlato + "|" + notaSafe + "|" + linea.precio;

            if (mapa.containsKey(clave)) {
                mapa.get(clave).agregarUno(linea);
            } else {
                mapa.put(clave, new ItemAgrupado(linea));
            }
        }
        return new ArrayList<>(mapa.values());
    }
}