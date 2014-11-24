package cliente;

import cliente.entidades.Usuario;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import servidor.Log;

public class NetworkManager {
    
    private Socket socket;
    private BufferedReader entrada;
    private BufferedWriter salida;
    private static NetworkManager instancia;
    private VentanaChat interfaz;
    
    public static NetworkManager getInstance() {
        if (instancia == null) {
            instancia = new NetworkManager();
        }
        return instancia;
    }
    
    
    public void escucharServidor() {
        inicializarHeartBeat();
        try {
            while (true) {
                String packet = recibir();
                if (packet.startsWith("200")) {
                    //Success
                } else if (packet.startsWith("400")) {
                    JOptionPane.showMessageDialog(interfaz, packet.substring(4), "Desconectado", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                } else if (packet.startsWith("500")) {
                    JOptionPane.showMessageDialog(interfaz, packet.substring(4), "Error", JOptionPane.WARNING_MESSAGE);
                } else if (packet.startsWith("SALA")) {
                    String[] p = packet.split("[ ]");
                    interfaz.setTitle("Sala " + p[1] + "@" + socket.getInetAddress().getHostAddress());
                } else if (packet.startsWith("LIST")) {
                    interfaz.limpiarListado();
                    int count = packet.split("[ ]").length;
                    for (int i = 1; i < count; i++) {
                        Usuario u = new Usuario(packet.split("[ ]")[i], null);
                        interfaz.agregarUsuario(u);
                    }
                    //interfaz.actualizarLista();
                } else if(!packet.isEmpty()) {
                    interfaz.agregarMensaje(packet);
                }
            }
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(interfaz, "Se ha perdido la conexión con el servidor", "Desconectado", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
    
    public void setServer(String IP, int port) {
        try {
            socket = new Socket(IP, port);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new BufferedWriter(new PrintWriter(socket.getOutputStream()));
        } catch (IOException ex) {
            Logger.getLogger(NetworkManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void enviar(String var) {
        if (!var.isEmpty()) {
            try {
                salida.write(var + "\n");
                salida.flush();
            } catch (IOException ex) {
                Logger.getLogger(NetworkManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public String recibir() {
        String s = "";
        try {
            s = entrada.readLine();
        } catch (IOException ex) {
            
        }
        return s;
    }
    
    public void setInterfaz(VentanaChat interfaz) {
        this.interfaz = interfaz;
    }

    private void inicializarHeartBeat() {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    long heartbeat = System.currentTimeMillis();
                    while (true) {
                        if (System.currentTimeMillis() - heartbeat >= 5000) {
                            enviar("BEAT " + System.currentTimeMillis());
                            heartbeat = System.currentTimeMillis();              
                        }
                    }
                }
            }).start();
    }
}
