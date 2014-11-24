package cliente.entidades;

import java.util.ArrayList;

public class Sala {
    
    private final ArrayList<Usuario> usuarios;
    private String nombre;
    
    public Sala(String nombre) {
        usuarios = new ArrayList<>();
        this.nombre = nombre;
    }

    public ArrayList<Usuario> getUsuarios() {
        return usuarios;
    }

    public void addUsuario(Usuario usr) {
        usuarios.add(usr);
    }
    
    public void eliminarUsuario(Usuario usr) {
        usuarios.remove(usr);
    }
    
    public int getCantidadUsuarios() {
        return usuarios.size();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
