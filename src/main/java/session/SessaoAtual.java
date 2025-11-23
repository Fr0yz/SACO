package session;

import model.Dentista;

public class SessaoAtual {

    private static Dentista dentistaLogado;

    public static Dentista getDentistaLogado() {
        return dentistaLogado;
    }

    public static void setDentistaLogado(Dentista dentista) {
        SessaoAtual.dentistaLogado = dentista;
    }

    public static void limpar() {
        dentistaLogado = null;
    }
}
