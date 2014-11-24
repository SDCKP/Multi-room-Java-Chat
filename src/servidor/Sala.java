package servidor;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class Sala {
    private ArrayList<Usuario> usuarios;
    private String nombre;
    private String password;
    private ArrayList<Usuario> baneados;
    
    public Sala(String nombre) {
        this.nombre = nombre;
        this.password = "";
        this.usuarios = new ArrayList<>();
        this.baneados = new ArrayList<>();
    }
    
    public Sala(String nombre, String pw) {
        this.nombre = nombre;
        this.password = pw;
        this.usuarios = new ArrayList<>();
        this.baneados = new ArrayList<>();
    }
    
    public String entrar(Usuario u) {
        //Comprueba que el usuario no existe en la sala
        if (!existeUsuario(u)) {
            //Comprueba que el usuario no está baneado de la sala
            if (!estaBaneado(u)) {
                //Conecta al usuario
                u.setConectado(true);
                //Añadimos al usuario a la lista de usuarios de la sala
                usuarios.add(u);
                //Difundimos el mensaje de aviso de entrada del usuario a todos los usuarios de la sala
                difundir(u.getNick() + " a entrado a la sala " + this.nombre);
                //Enviamos la lista de usuarios actualizada a todos los usuarios de la sala
                actualizarListadoUsuarios();
                Log.log(u.getNick() + " a entrado a la sala " + this.nombre);
                //Enviamos el nombre de la sala al usuario
                u.enviar("SALA " + this.nombre);
                return "200 OK";
            } else {
                //Desconectamos al usuario
                u.setConectado(false);
                return "400 Estas baneado de esta sala";
            }
        } else {
            //Desconectamos al usuario
            u.setConectado(false);
            return "400 El usuario ya está en la sala";
        }
    }
    
    public String entrar(Usuario u, String pw) {
        //Comprueba que el usuario no existe en la sala
        if (!existeUsuario(u)) {
            //Comprueba que la contraseña es correcta
            if (pw.equals(this.password)) {
                //Comprueba que el usuario no está baneado de la sala
                if (!estaBaneado(u)) {
                    //Conecta al usuario
                    u.setConectado(true);
                    //Añadimos al usuario a la lista de usuarios de la sala
                    usuarios.add(u);
                    //Difundimos el mensaje de aviso de entrada del usuario a todos los usuarios de la sala
                    difundir(u.getNick() + " a entrado a la sala " + this.nombre);
                    //Enviamos la lista de usuarios actualizada a todos los usuarios de la sala
                    actualizarListadoUsuarios();
                    Log.log(u.getNick() + " a entrado a la sala " + this.nombre);
                    //Enviamos el nombre de la sala al usuario
                    u.enviar("SALA " + this.nombre);
                    return "200 OK";
                } else {
                    //Desconectamos al usuario
                    u.setConectado(false);
                    return "400 Estas baneado de esta sala";
                }
            } else {
                //Enviamos un error de contraseña al usuario
                return "500 La contraseña de la sala es incorrecta";
            }
        } else {
            //Desconectamos al usuario
            u.setConectado(false);
            return "400 El usuario ya está en la sala";
        }
    }
    
    public boolean tienePassword() {
        return !this.password.isEmpty();
    }
    
    public void salir(Usuario u) {
        //Si existe el usuario salimos de la sala
        if (existeUsuario(u)) {
            //Lo eliminamos de la lista de usuarios de la sala
            usuarios.remove(u);
            //Difundimos el mensaje de salida a todos los miembros de la sala
            difundir(u.getNick() + " a salido de la sala " + this.nombre);
            //Enviamos el listado de usuarios actualizado a todos los usuarios de la sala
            actualizarListadoUsuarios();
            Log.log(u.getNick() + " a salido de la sala " + this.nombre);
        }
    }
    
    public boolean existeUsuario(Usuario u) {
        for (Usuario usr : usuarios) {
            if (usr.getNick().equalsIgnoreCase(u.getNick())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean estaBaneado(Usuario u) {
        for (Usuario usr : baneados) {
            if (usr.getNick().equals(u.getNick()) || usr.getIP().equals(u.getIP())) {
                return true;
            }
        }
        return false;
    }
    
    public void difundir(String mensaje) {
        for (Usuario usr : usuarios) {
            usr.enviar(mensaje);
        }
    }
    
    public Usuario obtenerUsuario(String nick) {
        for (Usuario usr : usuarios) {
            if (usr.getNick().equalsIgnoreCase(nick)) {
                return usr;
            }
        }
        return null;
    }
    
    public void agregarBaneo(Usuario u) {
        baneados.add(u);
    }
    
    public void quitarBaneo(String usr) {
        for (int i = 0; i < baneados.size(); i++) {
            if (baneados.get(i).getNick().equals(usr)) {
                baneados.remove(i);
                break;
            }
        }
    }
    
    public void enviarMensajePrivado(Usuario de, Usuario a, String mensaje) {
        //Muestra el mensaje al remitente
        de.enviar("(Privado)" + de.getNick() + ": " + mensaje);
        //Muestra el mensaje al destinatario
        a.enviar("(Privado)" + de.getNick() + ": " + mensaje);
    }
    
    public void moverASala(Sala destino) {
        //Mueve todos los usuarios de una sala a otra (por ejemplo cuando se elimina una sala)
        try {
            //Recorremos los usuarios de la sala
            for (int i = usuarios.size()-1; i >= 0; i--) {
                //Le cambiamos la sala donde debe ir
                usuarios.get(0).setSala(destino);
                //Entra en la sala de destino
                destino.entrar(usuarios.get(0));
                //Desconectamos al usuario
                salir(usuarios.get(0));
            }
            //Enviamos la lista de usuarios actualizada a toda la sala tras acabar de mover todos los usuarios
            destino.actualizarListadoUsuarios();
        } catch (ConcurrentModificationException ex) {}
        finally {actualizarListadoUsuarios();}
        
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public int getCountUsuarios() {
        return usuarios.size();
    }

    public ArrayList<Usuario> getUsuarios() {
        return usuarios;
    }

    public void actualizarListadoUsuarios() {
        for (Usuario usr : usuarios) {
            usr.enviarListaUsuarios();
        }
    }
}
