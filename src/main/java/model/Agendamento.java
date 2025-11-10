package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_agendamento")
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id_agendamento;
    public Integer id_paciente;
    public Integer id_dentista;
    public Integer id_tratamento;
    public Date data_hora;
    public String observacoes;
    public String nomePaciente;
    public String nomeDentista;
    public String nomeTratamento;

    private StatusAgendamento status;

    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }

}
