package model;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_paciente")
public class Paciente {
    @Id
    private int id_paciente;
}
