package service;

import dao.AnamneseDao;
import model.Anamnese;

import java.sql.SQLException;
import java.util.List;

public class AnamneseService {
    private final AnamneseDao dao = new AnamneseDao();

    // uma chamada que faz tudo (upsert + imagem)
    public void salvarOuAtualizar(Anamnese a) throws SQLException {
        if (a == null) throw new SQLException("Objeto anamnese nulo.");
        if (a.id_paciente <= 0) throw new SQLException("Paciente inválido.");
        dao.salvarCompleto(a);
    }

    public Anamnese buscarPorPaciente(long idPaciente) throws SQLException {
        return dao.buscarPorPaciente(idPaciente);
    }

    public List<Anamnese> listarTodas() throws SQLException {
        return dao.listarTodas();
    }

    public void excluirPorPaciente(long idPaciente) throws SQLException {
        dao.excluirPorPaciente(idPaciente);
    }

    public void excluirPorId(long idAnamnese) throws SQLException {
        dao.excluirPorId(idAnamnese);
    }

    public void removerImagem(long idPaciente) throws SQLException {
        dao.removerImagemOdontograma(idPaciente);
    }
}

