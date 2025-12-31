package com.uax.comandero.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection; // IMPORTANTE: Este faltaba o estaba mal
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

    public void imprimirTicket(int mesa, List<LineaComanda> productos, double total) {
        // 1. Verificar Permisos (Básico)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Faltan permisos de Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Buscar impresora conectada
        try {
            // CORRECCIÓN AQUÍ:
            // La variable debe ser de tipo 'BluetoothConnection', no 'BluetoothPrintersConnections'
            BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();

            if (connection == null) {
                Toast.makeText(context, "No hay impresora conectada o emparejada", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Configurar la impresora (203dpi es estándar, 48mm o 72mm de ancho)
            // Ahora 'connection' es del tipo correcto (DeviceConnection)
            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);

            // 4. Construir el texto con formato ESC/POS
            // [C] = Centrado, [L] = Izquierda, [R] = Derecha, <b> = Negrita
            StringBuilder recibo = new StringBuilder();

            // --- CABECERA ---
            recibo.append("[C]<b><font size='big'>RESTAURANTE UAX</font></b>\n");
            recibo.append("[C]Calle Universidad, 1\n");
            recibo.append("[C]Madrid, España\n");
            recibo.append("[L]\n");
            recibo.append("[L]<b>Mesa: " + mesa + "</b>[R]" + getFechaHora() + "\n");
            recibo.append("[C]--------------------------------\n");

            // --- PRODUCTOS ---
            for (LineaComanda p : productos) {
                // Formato: Nombre .............. Precio
                // El [L] y [R] en la misma línea hace el efecto de separación
                recibo.append("[L]" + p.nombrePlato + "[R]" + String.format("%.2f €", p.precio) + "\n");

                // Si hay nota, la ponemos debajo en pequeño
                if(p.notas != null && !p.notas.isEmpty()){
                    recibo.append("[L]<font size='small'>  (" + p.notas + ")</font>\n");
                }
            }

            recibo.append("[C]--------------------------------\n");

            // --- TOTAL ---
            recibo.append("[L]<b>TOTAL:</b>[R]<b><font size='big'>" + String.format("%.2f €", total) + "</font></b>\n");

            recibo.append("[L]\n");
            recibo.append("[C]¡Gracias por su visita!\n");
            recibo.append("[C]<font size='small'>IVA incluido</font>\n");

            // --- PIE Y CORTE ---
            recibo.append("[L]\n");
            recibo.append("[L]\n"); // Espacio para que salga el papel

            // 5. ENVIAR A IMPRIMIR
            printer.printFormattedText(recibo.toString());

            Toast.makeText(context, "Enviado a impresora", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            // Controlamos errores comunes de conexión
            String errorMsg = e.getMessage();
            if (errorMsg == null) errorMsg = "Error desconocido";

            Toast.makeText(context, "Error impresión: " + errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private String getFechaHora() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }
}