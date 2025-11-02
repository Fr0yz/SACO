package service;

import dao.CadastroPessoaDao;
import model.Dentista;
import model.Pessoa;
import java.sql.SQLException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CadastroPessoaService {

    private final CadastroPessoaDao dao = new CadastroPessoaDao();

    /* ======================
     * LISTAGEM
     * ====================== */

    /** Lista todas as pessoas cadastradas */
    public List<Pessoa> listarTodos() throws SQLException {
        return dao.listar();
    }

    /** Lista apenas pacientes (não dentistas) */
    public List<Pessoa> listarPacientes() throws SQLException {
        return dao.listarPacientes();
    }

    /* ======================
     * INSERÇÃO
     * ====================== */

    /** Insere uma nova pessoa (paciente ou dentista) */
    public long inserir(Pessoa p, Dentista d) throws SQLException {
        validarCamposObrigatorios(p, d);
        return dao.inserirPessoa(p, d);
    }

    /* ======================
     * ATUALIZAÇÃO
     * ====================== */

    /** Atualiza uma pessoa existente (e insere/atualiza/remover dentista) */
    public void atualizar(Pessoa p, Dentista d) throws SQLException {
        validarCamposObrigatorios(p, d);
        dao.atualizarPessoa(p);
        dao.upsertDentista(p.id_pessoa, d);
    }

    /* ======================
     * EXCLUSÃO
     * ====================== */

    /** Exclui uma pessoa e suas relações (CASCADE) */
    public void excluir(int idPessoa) throws SQLException {
        dao.excluirPessoa(idPessoa);
    }

    /* ======================
     * CONSULTAS AUXILIARES
     * ====================== */

    /** Busca o dentista correspondente à pessoa */
    public Dentista buscarDentista(int idPessoa) throws SQLException {
        return dao.buscarDentistaPorPessoaId(idPessoa);
    }

    /** Retorna o último ID cadastrado (se existir) */
    public Integer obterUltimoCadastro() throws SQLException {
        return dao.ultimocadastro();
    }

    /* ======================
     * VALIDAÇÕES DE NEGÓCIO
     * ====================== */

    /**
     * Valida os campos obrigatórios de Pessoa e Dentista.
     * Lança SQLException se algo inválido for detectado.
     */
    private void validarCamposObrigatorios(Pessoa p, Dentista d) throws SQLException {
        if (p == null) throw new SQLException("Objeto Pessoa nulo.");

        if (p.nome == null || p.nome.isBlank())
            throw new SQLException("Nome é obrigatório.");

        // Validação básica do CPF (apenas quantidade de dígitos)
        if (p.cpf == null || p.cpf.replaceAll("\\D", "").length() != 11)
            throw new SQLException("CPF inválido (deve conter 11 dígitos).");

        // Se for dentista, CRO e especialidade são obrigatórios
        if (p.dentista) {
            if (d == null)
                throw new SQLException("Dados de dentista não informados.");
            if (d.cro == null || d.cro.isBlank())
                throw new SQLException("CRO é obrigatório para dentistas.");
            if (d.especialidade == null || d.especialidade.isBlank())
                throw new SQLException("Especialidade é obrigatória para dentistas.");
        }
    }

}
