package com.uax.comandero.utils;

import android.content.Context;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
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

    // --- Solo genera el String (para la simulación) ---
    public String getTicketBuilder(int mesa, List<LineaComanda> productos, double total) {
        StringBuilder recibo = new StringBuilder();

        // Usamos tags HTML básicos que el TextView de Android puede entender parcialmente
        // O los tags propios de la librería que luego limpiaremos o mostraremos tal cual.

        recibo.append("[C]<b><font size='big'>RESTAURANTE Paco</font></b>\n");
        recibo.append("[C]Calle Paco, 57\n");
        recibo.append("[C]Madrid, España\n");
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
        recibo.append("[L]\n");
        recibo.append("[C]¡Gracias por su visita!\n");
        recibo.append("[C]<font size='small'>IVA incluido</font>\n");
        recibo.append("[L]\n[L]\n");

        return recibo.toString();
    }

    public void imprimirTicket(int mesa, List<LineaComanda> productos, double total) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Faltan permisos de Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
            if (connection == null) {
                Toast.makeText(context, "No hay impresora conectada", Toast.LENGTH_SHORT).show();
                return;
            }

            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);

            // LLAMAMOS AL GENERADOR AQUÍ
            String texto = getTicketBuilder(mesa, productos, total);

            printer.printFormattedText(texto);
            Toast.makeText(context, "Imprimiendo...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFechaHora() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }
}