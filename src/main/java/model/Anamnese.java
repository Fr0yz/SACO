package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anamnese {
    public long id_anamnese;
    public long id_paciente;
    public String alergias;
    public String historico_medico;
    public String medicamentos;
    public String detalhes;
    public Date data_registro;
    public byte[] imagem_odontograma;
}
