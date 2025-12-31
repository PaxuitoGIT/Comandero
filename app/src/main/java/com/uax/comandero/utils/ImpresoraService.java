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
import com.uax.comandero.data.LineaComanda;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImpresoraService {

    private Context context;

    public ImpresoraService(Context context) {
        this.context = context;
    }

    // --- SOLUCIÓN ERROR 1: Método seguro para Toasts desde cualquier hilo ---
    // Esto evita el error: "Can't toast on a thread that has not called Looper.prepare()"
    private void showToast(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        );
    }

    // --- SOLUCIÓN ERROR 2: Chequeo de permisos antes de tocar Bluetooth ---
    // Esto evita el error: "SecurityException: Need BLUETOOTH_CONNECT"
    private boolean tienePermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // En Android 12+ requerimos BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Falta permiso de dispositivo cercano (CONNECT)");
                return false;
            }
        } else {
            // En Android 11- requerimos BLUETOOTH normal
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                showToast("Falta permiso Bluetooth");
                return false;
            }
        }
        return true;
    }

    // ==========================================
    // TICKET COCINA (SIN PRECIOS)
    // ==========================================

    public String getTicketCocinaBuilder(int mesa, List<LineaComanda> productos) {
        StringBuilder recibo = new StringBuilder();

        recibo.append("[C]<b><font size='big'>ORDEN COCINA</font></b>\n");
        recibo.append("[C]--------------------------------\n");
        recibo.append("[L]<b>MESA: " + mesa + "</b>[R]" + getFechaHora() + "\n");
        recibo.append("[C]--------------------------------\n");
        recibo.append("[L]\n");

        for (LineaComanda p : productos) {
            // Plato en grande
            recibo.append("[L]<b><font size='big'>- " + p.nombrePlato + "</font></b>\n");
            // Nota destacada
            if(p.notas != null && !p.notas.isEmpty()){
                recibo.append("[L]  *** " + p.notas + " ***\n");
            }
            recibo.append("[L]\n");
        }

        recibo.append("[L]\n[L]\n[L]\n");
        return recibo.toString();
    }

    public void imprimirTicketCocina(int mesa, List<LineaComanda> productos) {
        // 1. Verificamos permisos antes de hacer NADA
        if (!tienePermisos()) return;

        try {
            // 2. Buscamos impresora
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                showToast("No hay impresora conectada/emparejada");
                return;
            }

            // 3. Imprimimos
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
            String texto = getTicketCocinaBuilder(mesa, productos);
            printer.printFormattedText(texto);

            showToast("Enviado a Cocina...");

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error Impresora: " + e.getMessage());
        }
    }

    // ==========================================
    // TICKET RECIBO (CON PRECIOS)
    // ==========================================

    public String getTicketBuilder(int mesa, List<LineaComanda> productos, double total) {
        StringBuilder recibo = new StringBuilder();
        recibo.append("[C]<b><font size='big'>RESTAURANTE UAX</font></b>\n");
        recibo.append("[L]\n");
        recibo.append("[L]<b>Mesa: " + mesa + "</b>[R]" + getFechaHora() + "\n");
        recibo.append("[C]--------------------------------\n");

        for (LineaComanda p : productos) {
            recibo.append("[L]" + p.nombrePlato + "[R]" + String.format("%.2f €", p.precio) + "\n");
            if(p.notas != null && !p.notas.isEmpty()){
                recibo.append("[L]<font size='small'>  (" + p.notas + ")</font>\n");
            }
        }

        recibo.append("[C]--------------------------------\n");
        recibo.append("[L]<b>TOTAL:</b>[R]<b><font size='big'>" + String.format("%.2f €", total) + "</font></b>\n");
        recibo.append("[L]\n[L]\n[L]\n");

        return recibo.toString();
    }

    public void imprimirTicket(int mesa, List<LineaComanda> productos, double total) {
        if (!tienePermisos()) return;

        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                showToast("No hay impresora conectada");
                return;
            }
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
            String texto = getTicketBuilder(mesa, productos, total);
            printer.printFormattedText(texto);
            showToast("Imprimiendo Recibo...");

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error: " + e.getMessage());
        }
    }

    private String getFechaHora() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }
}