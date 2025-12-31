package com.uax.comandero.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.uax.comandero.data.ItemAgrupado; // IMPORTAR
import com.uax.comandero.data.LineaComanda;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImpresoraService {
    private Context context;
    public ImpresoraService(Context context) { this.context = context; }

    private void showToast(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show());
    }

    private boolean tienePermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Falta permiso BLUETOOTH_CONNECT"); return false;
            }
        }
        return true;
    }

    // ==========================================
    // TICKET COCINA STACKEADO
    // ==========================================
    public String getTicketCocinaBuilder(int mesa, List<LineaComanda> productosRaw) {
        StringBuilder recibo = new StringBuilder();
        recibo.append("[C]<b><font size='big'>ORDEN COCINA</font></b>\n");
        recibo.append("[C]--------------------------------\n");
        recibo.append("[L]<b>MESA: " + mesa + "</b>[R]" + getFechaHora() + "\n");
        recibo.append("[C]--------------------------------\n[L]\n");

        // AGRUPAR
        List<ItemAgrupado> lista = ComandaUtils.agruparLineas(productosRaw);

        for (ItemAgrupado item : lista) {
            String textoItem;
            if (item.cantidad > 1) textoItem = item.cantidad + "x " + item.nombre;
            else textoItem = "- " + item.nombre;

            recibo.append("[L]<b><font size='big'>" + textoItem + "</font></b>\n");

            if(item.notas != null && !item.notas.isEmpty()){
                recibo.append("[L]  *** " + item.notas + " ***\n");
            }
            recibo.append("[L]\n");
        }
        recibo.append("[L]\n[L]\n[L]\n");
        return recibo.toString();
    }

    public void imprimirTicketCocina(int mesa, List<LineaComanda> productos) {
        if (!tienePermisos()) return;
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) { showToast("Sin impresora"); return; }
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
            printer.printFormattedText(getTicketCocinaBuilder(mesa, productos));
            showToast("Enviado Cocina");
        } catch (Exception e) { e.printStackTrace(); showToast("Error: " + e.getMessage()); }
    }

    // ==========================================
    // RECIBO CLIENTE STACKEADO
    // ==========================================
    public String getTicketBuilder(int mesa, List<LineaComanda> productosRaw, double total) {
        StringBuilder recibo = new StringBuilder();
        recibo.append("[C]<b><font size='big'>RESTAURANTE PACO</font></b>\n");
        recibo.append("[C]Calle Paco, 57\n");
        recibo.append("[C]Madrid, España\n");
        recibo.append("[L]\n[L]<b>Mesa: " + mesa + "</b>[R]" + getFechaHora() + "\n");
        recibo.append("[C]--------------------------------\n");

        // AGRUPAR
        List<ItemAgrupado> lista = ComandaUtils.agruparLineas(productosRaw);

        for (ItemAgrupado item : lista) {
            if (item.cantidad > 1) {
                // FORMATO: 3x Cerveza (2.00) ..... 6.00 €
                String izq = item.cantidad + "x " + item.nombre + " (" + String.format("%.2f", item.precioUnitario) + ")";
                recibo.append("[L]" + izq + "[R]" + String.format("%.2f €", item.precioTotal) + "\n");
            } else {
                recibo.append("[L]" + item.nombre + "[R]" + String.format("%.2f €", item.precioTotal) + "\n");
            }

            if(item.notas != null && !item.notas.isEmpty()){
                recibo.append("[L]<font size='small'>  (" + item.notas + ")</font>\n");
            }
        }

        recibo.append("[C]--------------------------------\n");
        recibo.append("[L]<b>TOTAL:</b>[R]<b><font size='big'>" + String.format("%.2f €", total) + "</font></b>\n");
        recibo.append("[L]\n");
        recibo.append("[C]¡Gracias por su visita!\n");
        recibo.append("[C]<font size='small'>IVA incluido</font>\n");
        recibo.append("[L]\n[L]\n");
        return recibo.toString();
    }

    public void imprimirTicket(int mesa, List<LineaComanda> productos, double total) {
        if (!tienePermisos()) return;
        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) { showToast("Sin impresora"); return; }
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
            printer.printFormattedText(getTicketBuilder(mesa, productos, total));
            showToast("Imprimiendo Recibo");
        } catch (Exception e) { e.printStackTrace(); showToast("Error: " + e.getMessage()); }
    }

    private String getFechaHora() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }
}