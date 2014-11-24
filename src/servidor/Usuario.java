package servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import static servidor.Servidor.ADMIN_PASSWORD;

public class Usuario implements Runnable {
    
    private String nick;
    private BufferedReader entrada;
    private BufferedWriter salida;
    private long loginTime;
    private boolean conectado, superUser, heartBeatOn;
    private String IP;
    private long ping;
    private Sala sala;
    private long lastBeat;
    
    public Usuario(String nick) {
        this.nick = nick;
    }
    
    public Usuario(Socket s, Sala sala) throws IOException {
        this.sala = sala;
        this.loginTime = System.currentTimeMillis();
        this.IP = s.getInetAddress().getHostAddress();
        this.ping = 0;
        this.superUser = false;
        this.heartBeatOn = true;
        entrada = new BufferedReader(new InputStreamReader(s.getInputStream()));
        salida = new BufferedWriter(new PrintWriter(s.getOutputStream()));
    }

    @Override
    public void run() {
        //Espera a recibir el mensaje de login del cliente
        String login = recibir();
        //Comprueba que el mensaje de login es correcto
        if (!login.startsWith("NICK")) {
            //Desconecta el cliente si hay un error
            enviar("400 Paquete inválido recibido");
            Log.log("Paquete de login inválido: " + login);
            conectado = false;
        } else {
            //Comprueba que el tamaño del nick no sea muy largo
            if (login.split("[ ]")[1].length() >= 12) {
                //Envia un error y desconecta al usuario
                enviar("400 El nick elegido es demasiado largo, introduce un nick de como máximo 12 carácteres");
                Log.log("Un usuario ha tratado de entrar con un nick demasiado largo. Nick: " + login.split("[ ]")[1]);
            } else {
                //Conecta al cliente si el usuario no existe en la sala
                conectado = !sala.existeUsuario(this);
            }
        }
        //Si todo esta correcto se conecta a la sala
        if (conectado) {
            //Obtenemos el nick recibido del cliente
            nick = login.split("[ ]")[1];
            
            //Conectamos al usuario a la sala
            enviar(sala.entrar(this));
            //Enviamos la lista de usuarios de la sala
            enviarListaUsuarios();
            
            if (heartBeatOn) {
                //Inicializamos el heartbeat para comprobar que el cliente mantiene la conexión
                lastBeat = System.currentTimeMillis();
                asyncBeatCheck();
            }
            //Enviamos el nombre de la sala
            enviar("SALA " + sala.getNombre());
            
            //Bucle que durará hasta que el usuario se desconecte (EXIT o errores)
            do {
                //Esperamos a recibir un mensaje del cliente
                String packet = recibir();

                //Si el paquete no está vacio lo analizamos
                if (packet != null && !packet.isEmpty()) {
                    analizarPacket(packet);
                }
                
            } while (conectado);
            
            //Enviamos el mensaje de desconexion al usuario
            enviar("400 Has sido desconectado del chat");
            
            //El cliente ya no está conectado, lo sacamos de la sala
            sala.salir(this);
        }
    }
    
    public void analizarPacket(String s) {
        if (s.startsWith("EXIT")) { //Petición de salida del chat
            conectado = false;
        } else if (s.startsWith("BEAT")) { //Mensaje automático de heartbeat
            String[] p;
            p = s.split("[ ]");
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Actualizamos la última vez que recibimos un heartbeat
                lastBeat = System.currentTimeMillis();
                //Actualizamos el ping del usuario
                ping = System.currentTimeMillis() - Long.parseLong(p[1]);
            }
        } else if (s.startsWith("/NICK ")) { //Petición de cambio de nick
            String[] p;
            p = s.split("[ ]");
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Guardamos el nick antiguo
                String oldname = nick;
                //Comprobamos que no hay un usuario con el nuevo nick en la sala
                if (sala.existeUsuario(new Usuario(p[1]))) {
                    enviar("500 Ya hay un usuario llamado " + nick + " en la sala");
                    nick = oldname;
                } else {
                    if (!(p[1].length() > 12)) {
                        //Cambiamos el nick por el indicado
                        nick = p[1];
                        //Enviamos el listado de usuarios a todos los usuarios de la sala (para que reciban el nuevo nick en la lista)
                        sala.actualizarListadoUsuarios();
                        Log.log(oldname + " a cambiado de nombre a " + nick);
                        //Enviamos un mensaje a todos los usuarios de la sala indicando el cambio de nick
                        sala.difundir(oldname + " a cambiado de nombre a " + nick);
                        //Enviamos un mensaje indicando que se ha realizado la operación con éxito
                        enviar("200 OK");
                    } else {
                        enviar("500 El nick elegido es demasiado largo. Max 12 carácteres");
                    }
                }
            }
        } else if (s.startsWith("/WHOIS ")) { //Petición de información sobre un usuario
            String[] p;
            p = s.split("[ ]");
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Comprobamos que el usuario está en la sala
                if (!sala.existeUsuario(new Usuario(p[1]))) {
                    enviar("500 No hay ningun usuario con el nombre " + p[1]);
                } else {
                    //Obtenemos el usuario requerido de la sala
                    Usuario tmp = sala.obtenerUsuario(p[1]);
                    //Enviamos los datos del usuario
                    enviar("======================\nNombre: " + tmp.getNick() + "\nIP: " + tmp.getIP() + "\nPing: " + tmp.getPing() + "ms\nEntrada: " + new Date(tmp.getLoginTime()).toGMTString() + "======================");
                    Log.log("Petición de WHOIS de " + nick + " sobre " + tmp.getNick());
                }
            }
        } else if (s.startsWith("/HELP")) { //Petición de listado de comandos
            String[] p;
            p = s.split("[ ]");
            
            //Enviamos el listado con los comandos disponibles
            enviar("======================\nComandos\n======================\n- /WHOIS <usr>: Muestra información del usuario");
            enviar("- /P <usr> <msg>: Envia un mensaje privado a un usuario de la sala\n- /NICK <nuevo>: Cambia tu nombre de usuario");
            enviar("- /C <nombre> [PW]: Crea una sala nueva y te mete en ella. Contraseña opcional.\n- /J <nombre> [PW]: Cambia a la sala especificada");
            enviar("- /LIST: Lista las salas disponibles en el servidor\n- /DADO: Genera un numero aleatorio entre 1 y 6\n- EXIT: Sale del chat\n======================");
        }  else if (s.startsWith("/P")) { //Mensaje privado
            String[] p;
            p = s.split("[ ]");
            
            //Comprobamos que el usuario existe en la sala
            if (!sala.existeUsuario(new Usuario(p[1]))) {
                enviar("500 No hay ningun usuario con el nombre " + p[1]);
            } else {
                //Obtenemos el usuario al que enviaremos el mensaje
                Usuario tmp = sala.obtenerUsuario(p[1]);
                //Enviamos a la sala nuestro usuario, el usuario destino, y el contenido del mensaje privado
                //La sala se encargará de enviar el mensaje privado solo al destinatario del mismo
                sala.enviarMensajePrivado(this, tmp, s.substring(3+tmp.getNick().length()+1));
                Log.log("Mensaje privado de " + this.getNick() + " y " + tmp.getNick() + ": " + s.substring(3+tmp.getNick().length()));
            }
        }   else if (s.startsWith("/SUDO ")) { //Petición de permisos de administrador
            String[] p;
            p = s.split("[ ]");
            
            //Comprobamos que la contraseña de administrador enviada coincide
            if (p.length > 2) {
                if (!p[1].equals(ADMIN_PASSWORD)) {
                    enviar("500 La contraseña de privilegios SUDO es incorrecta");
                } else {
                    //Cambiamos el modo del usuario a superusuario
                    superUser = true;
                    //Enviamos un mensaje solo al usuario indicando que sus permisos se han cambiado correctamente
                    enviar("Has obtenido privilegios SUDO");
                    Log.log("Privilegios SUDO otorgados a " + nick);
                }
            }
        } else if (s.startsWith("/KICK ")) { //Petición de echar a alguien del chat
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else if (!superUser) { //Comprobamos que el usuario que realiza la acción es superusuario
                enviar("500 Permisos insuficientes");
                Log.log("Intento del uso de comando KICK sin superusuario: " + nick);
            } else {
                //Comprobamos que el usuario indicado está en la sala
                if (!sala.existeUsuario(new Usuario(p[1]))) {
                    enviar("500 No hay ningun usuario con el nombre " + p[1]);
                } else {
                    //Obtenemos el usuario
                    Usuario tmp = sala.obtenerUsuario(p[1]);
                    //Lo desconectamos del chat
                    tmp.conectado = false;
                    //Distribuimos un mensaje indicando el kick
                    sala.difundir(nick + " ha echado a " + tmp.getNick() + " del chat");
                    //Avisamos al usuario que ha sido expulsado
                    tmp.enviar("400 Has sido expulsado del chat por " + nick);
                    Log.log(nick + " ha echado a " + tmp.getNick());
                }
            }
        } else if (s.startsWith("/BAN ")) { //Petición de baneo de usuario (permanente)
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else if (!superUser) { //Comprobamos que el usuario que realiza la acción es superusuario
                enviar("500 Permisos insuficientes");
                Log.log("Intento del uso de comando BAN sin superusuario: " + nick);
            } else {
                //Comprobamos que el usuario indicado está en la sala
                if (!sala.existeUsuario(new Usuario(p[1]))) {
                    enviar("500 No hay ningun usuario con el nombre " + p[1]);
                } else {
                    //Obtenemos el usuario
                    Usuario tmp = sala.obtenerUsuario(p[1]);
                    //Lo desconectamos
                    tmp.conectado = false;
                    //Agregamos al usuario como baneado de la sala
                    sala.agregarBaneo(tmp);
                    //Mostramos un mensaje en la sala indicando el baneo
                    sala.difundir(nick + " ha baneado a " + tmp.getNick() + " de la sala");
                    //Avisamos al usuario que ha sido expulsado
                    tmp.enviar("400 Has sido expulsado del chat por " + nick);
                    Log.log(nick + " ha baneado a " + tmp.getNick());
                }
            }
        } else if (s.startsWith("/UNBAN ")) { //Quita el baneo a un usuario
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else if (!superUser) { //Comprobamos que el usuario que realiza la acción es superusuario
                enviar("500 Permisos insuficientes");
                Log.log("Intento del uso de comando UNBAN sin superusuario: " + nick);
            } else {
                //Comprobamos que el usuario indicado está realmente en la lista de baneados de la sala
                if (!sala.estaBaneado(new Usuario(p[1]))) {
                    enviar("500 El usuario " + p[1] + " no está baneado");
                } else {
                    //Eliminamos al usuario de la lista de baneados de la sala
                    sala.quitarBaneo(p[1]);
                    Log.log(nick + " ha quitado el ban a " + p[1]);
                }
            }
        } else if (s.startsWith("/C ")) { //Petición de creación de una nueva sala
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 3) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Comprobamos que la sala no existe
                if (!Servidor.existeSala(new Sala(p[1]))) {
                    Sala sl = null;
                    //Sala sin contraseña (1 parametro)
                    if (p.length == 2) {
                        //Creamos la sala
                        sl = new Sala(p[1]);
                    } else if (p.length == 3) { //Sala con contraseña (2 parametros)
                        //Creamos la sala
                        sl = new Sala(p[1], p[2]);
                    }
                    if (sl != null) {
                        //Agregamos la sala a la lista de salas
                        Servidor.agregarSala(sl);
                        //Sacamos al usuario de la sala actual
                        sala.salir(this);
                        //Lo metemos en la nueva sala
                        sl.entrar(this);
                        //Cambiamos la sala en el usuario
                        sala = sl;
                        //Enviamos el nombre de la nueva sala al usuario
                        enviar("SALA " + sala.getNombre());
                        //Actualizamos la lista de usuarios para todos los usuarios de la sala
                        sala.actualizarListadoUsuarios();
                    }
                } else {
                    enviar("500 Ya existe una sala con ese nombre");
                }
            }
        } else if (s.startsWith("/J ")) { //Petición de entrada a una sala existente
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 3) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Comprobamos que la sala existe
                if (Servidor.existeSala(new Sala(p[1]))) {
                    //Comprobamos que el usuario no esta baneado de la sala
                    if (!Servidor.obtenerSala(p[1]).estaBaneado(this)) {
                        //Recibido 1 parametro (nombre sala)
                        if (p.length == 2) {
                            //Obtenemos la sala a partir del nombre
                            Sala sl = Servidor.obtenerSala(p[1]);
                            //Si tiene contraseña, indicamos que no puede entrar sin especificarla
                            if (sl.tienePassword()) {
                                enviar("500 Esta sala requiere contraseña");
                            } else { //Si no tiene contraseña, entramos a la sala
                                //Sacamos al usuario de la sala actual
                                sala.salir(this);
                                //Lo metemos en la nueva sala
                                sl.entrar(this);
                                //Cambiamos la sala en el usuario
                                sala = sl;
                                //Enviamos el nombre de la sala
                                enviar("SALA " + sala.getNombre());
                                //Actualizamos el listado de usuarios para todos los usuarios de la sala
                                sala.actualizarListadoUsuarios();
                            }
                        } else if (p.length == 3) { //Recibidos 2 parametros (nombre sala+contraseña)
                            //Obtenemos la sala a partir del nombre
                            Sala sl = servidor.Servidor.obtenerSala(p[1]);
                            //Comprobamos que la contraseña indicada coincide con la de la sala
                            if (!sl.getPassword().equalsIgnoreCase(p[2])) {
                                enviar("500 Contraseña de sala incorrecta");
                            } else { //La contraseña coincide
                                //Sacamos al usuario de la sala actual
                                sala.salir(this);
                                //Lo metemos en la nueva sala
                                sl.entrar(this);
                                //Cambiamos la sala en el usuario
                                sala = sl;
                                //Enviamos el nombre de la sala
                                enviar("SALA " + sala.getNombre());
                                //Actualizamos el listado de usuarios para todos los usuarios de la sala
                                sala.actualizarListadoUsuarios();
                            }
                        }
                    } else {
                        enviar("500 No puedes acceder a la sala " + p[1] + " porque estas baneado en ella");
                    }
                } else { //No existe la sala
                    enviar("500 No existe ninguna sala llamada " + p[1]);
                }
            }
        } else if (s.startsWith("/D ")) { //Petición de eliminación de una sala
            String[] p;
            p = s.split("[ ]");
            
            if (p.length > 2) {
                enviar("500 Sintaxis incorrecta");
                Log.log("Paquete inválido: " + s);
            } else {
                //Comprobamos que el usuario que realiza esta acción tiene permisos de superusuario
                if (p.length == 2 && superUser) {
                    //Comprobamos que no está eliminando la sala principal
                    if (p[1].equalsIgnoreCase("Principal")) {
                        enviar("500 La sala principal no puede ser eliminada!");
                    } else {
                        //Comprobamos que existe la sala
                        if (Servidor.existeSala(new Sala(p[1]))) {
                            //Obtenemos la sala a partir del nombre
                            Sala sl = servidor.Servidor.obtenerSala(p[1]);
                            //Informamos a todos los usuarios de la sala que el usuario ha eliminado la sala antes de borrarla
                            sl.difundir(nick + " ha eliminado la sala");
                            //Eliminamos la sala de la lista de salas del servidor (mueve los usuarios a la sala Principal)
                            Servidor.eliminarSala(sl);
                        } else { //No existe la sala
                            enviar("500 No existe ninguna sala llamada " + p[1]);
                        }
                    }
                } else {
                    enviar("500 Privilegios insuficientes");
                }
            }
        } else if (s.startsWith("/LIST")) { //Petición de listado de las salas disponibles
            //Obtenemos un array de strings con los nombres de las salas
            Sala[] sl = servidor.Servidor.obtenerSalas();
            //Enviamos la información de las salas al usuario que realiza la petición
            enviar("===========================");
            enviar("Salas disponibles: " + sl.length);
            enviar("===========================");
            for (Sala sl1 : sl) {
                enviar(sl1.getNombre() + " - Usuarios: " + sl1.getCountUsuarios() + ((sl1.tienePassword())?" (con contraseña)":""));
            }
            enviar("===========================");
        } else if (s.startsWith("/DADO")) { //Lanza un dado
            //Generamos un numero aleatorio entre 1 y 6
            int numero = new Random().nextInt(6)+1;
            //Mostramos el resultado en la sala
            sala.difundir(nick + " lanza un dado y obtiene un " + numero);
        } else { //Recibido un mensaje de texto normal
            if (s.length() < 140) {
                //Difundimos el mensaje recibido a todos los usuarios de la sala
                sala.difundir(nick + ": " + s);
                Log.log("Recibido mensaje de " + nick + " en la sala " + sala.getNombre() + ". Contenido: " + s);
            } else {
                Log.log("Recibido mensaje demasiado largo de " + nick);
            }
        }
    }
    
    public void enviarListaUsuarios() {
        StringBuilder strb = new StringBuilder();
        strb.append("LIST ");
        for (Usuario usr : sala.getUsuarios()) {
            strb.append(usr.getNick());
            strb.append(" ");
        }
        enviar(strb.toString());
    }
    
    public void enviar(String s) {
        try {
            salida.write(s + "\n");
            salida.flush();
        } catch (IOException ex) {
            Logger.getLogger(Usuario.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String recibir() {
        String s = "";
        try {
            s = entrada.readLine();
        } catch (Exception ex) {}
        return s;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public long getPing() {
        return ping;
    }

    public void setPing(long ping) {
        this.ping = ping;
    }

    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }

    public boolean isConectado() {
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    private void asyncBeatCheck() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Continuamos comprobando que está conectado el usuario mientras que este siga realmente conectado
                while (conectado) {
                    //Esperamos 1 segundo
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {}
                    //Comprobamos que no han pasado más de 7 segundos desde que se envió el último heartbeat
                    if (System.currentTimeMillis() - lastBeat >= 7000) {
                        //Desconectamos al usuario por inactividad
                        enviar("400 Desconectado por inactividad");
                        conectado = false;
                        Log.log(nick + " a sido desconectado por inactividad");                
                    }
                }
            }
        }).start();
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }
    
}
