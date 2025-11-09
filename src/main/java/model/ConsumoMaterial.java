package model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa o consumo de um material em um tratamento ou agendamento.
 * Armazena a quantidade utilizada de cada material.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_consumo_material")
public class ConsumoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo")
    private Integer idConsumo;

    @Column(name = "id_agendamento")
    private Integer idAgendamento;

    @Column(name = "id_material")
    private Integer idMaterial;

    @Column(name = "quantidade")
    private Integer quantidade;

    // 🔹 Construtor prático: criar consumo sem agendamento (tratamento direto)
    public ConsumoMaterial(Integer idMaterial, Integer quantidade) {
        this.idMaterial = idMaterial;
        this.quantidade = quantidade;
    }

    // 🔹 Construtor prático: criar consumo com agendamento
    public ConsumoMaterial(Integer idAgendamento, Integer idMaterial, Integer quantidade) {
        this.idAgendamento = idAgendamento;
        this.idMaterial = idMaterial;
        this.quantidade = quantidade;
    }
}
