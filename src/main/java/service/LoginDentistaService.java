package service;

import dao.CadastroPessoaDao;
import model.Dentista;

import java.sql.SQLException;

public class LoginDentistaService {

    private final CadastroPessoaDao cadastroPessoaDao;

    // Construtor padrão (cria o DAO internamente)
    public LoginDentistaService() {
        this.cadastroPessoaDao = new CadastroPessoaDao();
    }

    // Construtor para injetar o DAO (útil para testes)
    public LoginDentistaService(CadastroPessoaDao cadastroPessoaDao) {
        this.cadastroPessoaDao = cadastroPessoaDao;
    }

    /**
     * Faz o login do dentista pelo NOME.
     *
     * Regras:
     * - Nome não pode ser vazio/nulo.
     * - Usa o DAO para:
     * - Validar se é dentista
     * - Verificar se já existem 2 dentistas logados
     * - Registrar o login em TB_LOGIN_DENTISTA
     *
     * @param nome Nome digitado no login.
     * @return Dentista logado (objeto carregado do banco).
     *
     * @throws IllegalArgumentException se nome for inválido.
     * @throws IllegalStateException    se já houver 2 dentistas logados.
     * @throws SQLException             para erros de banco (nome não encontrado,
     *                                  etc.).
     */
    public Dentista loginPorNome(String nome) throws SQLException {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do dentista.");
        }

        // Reaproveita toda a lógica que já colocamos no DAO
        return cadastroPessoaDao.loginDentistaPorNome(nome.trim());
    }

    /**
     * Faz logout do dentista, encerrando suas sessões ativas
     * em TB_LOGIN_DENTISTA (ATIVO = 0).
     *
     * @param idDentista ID do dentista (mesmo que ID_PESSOA).
     * @throws SQLException em erro de banco.
     */
    public void logout(long idDentista) throws SQLException {
        if (idDentista <= 0) {
            throw new IllegalArgumentException("ID do dentista inválido.");
        }
        cadastroPessoaDao.logoutDentista((int) idDentista);
    }

    /**
     * Overload de logout recebendo o objeto Dentista.
     */
    public void logout(Dentista dentista) throws SQLException {
        if (dentista == null) {
            throw new IllegalArgumentException("Dentista não informado.");
        }
        logout(dentista.getId_dentista());
    }
}
