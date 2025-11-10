package model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_dentista")
public class Dentista {
    @Id
    public int id_dentista;
    public String cro;
    public String especialidade;
    public String nome;
}
