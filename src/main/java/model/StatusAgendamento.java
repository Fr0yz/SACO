package model;

public enum StatusAgendamento {
    PENDENTE("Pendente"),
    CONFIRMADO("Confirmado"),
    CANCELADO("Cancelado"),
    CONCLUIDO("Concluído");

    private final String descricao;
    StatusAgendamento(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }

    public static StatusAgendamento fromString(String valor) {
        if (valor == null) return null;
        for (StatusAgendamento s : values()) {
            if (s.name().equalsIgnoreCase(valor) || s.descricao.equalsIgnoreCase(valor)) return s;
        }
        throw new IllegalArgumentException("Status inválido: " + valor);
    }
}
