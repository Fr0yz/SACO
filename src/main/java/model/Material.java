package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Integer ID;

    @Column(name = "nome")
    private String NOME;

    @Column(name = "quantidade")
    private Integer QUANTIDADE;

    /**
     * Quantidade consumida em um tratamento.
     * Campo não mapeado para o banco (apenas uso temporário na aplicação).
     */
    @Transient
    private Integer qtdConsumo;
}
