package cliente.entidades;

public class Usuario {
    
    private String nick;
    private Sala sala;
    
    public Usuario(String nick, Sala sala) {
        this.nick = nick;
        this.sala = sala;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }
    
    

}
