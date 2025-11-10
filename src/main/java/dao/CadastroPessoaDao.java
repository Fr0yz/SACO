package dao;
import model.Dentista;
import model.Pessoa;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CadastroPessoaDao {

    public List<Dentista> listarDentista() throws SQLException {
        List<Dentista> lista = new ArrayList<>();

        String sql = """
        SELECT de.ID_DENTISTA, pe.NOME
        FROM TB_DENTISTA de
        INNER JOIN TB_PESSOA pe ON de.ID_DENTISTA = pe.ID_PESSOA
        ORDER BY pe.NOME
        """;

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Dentista d = new Dentista();
                d.setId_dentista(rs.getInt("ID_DENTISTA"));
                d.setNome(rs.getString("NOME"));
                lista.add(d);
            }
        }

        return lista;
    }

    public List<Pessoa> listarPacientes() throws SQLException {
        List<Pessoa> lista = new ArrayList<>();

        String sql = """
        SELECT pa.ID_PACIENTE, pe.NOME
        FROM TB_PACIENTE pa
        INNER JOIN TB_PESSOA pe ON pa.ID_PACIENTE = pe.ID_PESSOA
        ORDER BY pe.NOME
        """;

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pessoa p = new Pessoa();
                p.setId_pessoa(rs.getInt("ID_PACIENTE"));
                p.setNome(rs.getString("NOME"));
                lista.add(p);
            }
        }

        return lista;
    }

    Integer id_pessoa = null;

    public Integer ultimocadastro() throws SQLException {
        String sql = "SELECT ID_PESSOA FROM TB_PESSOA ORDER BY ID_PESSOA DESC LIMIT 1";
        try (Connection conn = Conexao.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                id_pessoa = rs.getInt("ID_PESSOA");
                return id_pessoa;
            }
            return null;
        }
    }

    public List<Pessoa> listar() throws SQLException {
        List<Pessoa> lista = new ArrayList<>();
        String sql = "SELECT ID_PESSOA, NOME, CPF, TELEFONE, EMAIL, DT_NASCIMENTO FROM TB_PESSOA ORDER BY ID_PESSOA";

        try (Connection conn = Conexao.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Pessoa p = new Pessoa();
                p.setId_pessoa(rs.getInt("ID_PESSOA"));
                p.setNome(rs.getString("NOME"));
                p.setCpf(rs.getString("CPF"));
                p.setTelefone(rs.getString("TELEFONE"));
                p.setEmail(rs.getString("EMAIL"));
                p.setDt_nascimento(rs.getDate("DT_NASCIMENTO"));
                lista.add(p);
            }
        }
        return lista;
    }



    public long inserirPessoa(Pessoa pessoa, Dentista dentista) throws SQLException {
        final String sqlPessoa   = "INSERT INTO TB_PESSOA (NOME, CPF, TELEFONE, EMAIL, DT_NASCIMENTO) VALUES (?,?,?,?,?)";
        final String sqlDentista = "INSERT INTO TB_DENTISTA (ID_DENTISTA, CRO, ESPECIALIDADE) VALUES (?,?,?)";
        final String sqlPaciente = "INSERT INTO TB_PACIENTE (ID_PACIENTE) VALUES (?)";

        Connection conn = Conexao.getConnection();
        try {
            conn.setAutoCommit(false);

            long novoId;
            try (PreparedStatement ps = conn.prepareStatement(sqlPessoa, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pessoa.nome);
                ps.setString(2, pessoa.cpf);        // <<<< CPF como String
                ps.setString(3, pessoa.telefone);   // <<<< Telefone como String
                ps.setString(4, pessoa.email);

                if (pessoa.dt_nascimento != null) {
                    ps.setDate(5, new java.sql.Date(pessoa.dt_nascimento.getTime()));
                } else {
                    ps.setNull(5, java.sql.Types.DATE);
                }

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Falha ao obter ID gerado para TB_PESSOA.");
                    novoId = keys.getLong(1); // use long para BIGINT/IDENTITY
                }
            }

            if (pessoa.dentista) {
                if (dentista == null) throw new SQLException("Dados de dentista ausentes.");
                try (PreparedStatement ps = conn.prepareStatement(sqlDentista)) {
                    ps.setLong(1, novoId);                 // FK = pessoa.id
                    ps.setString(2, dentista.cro);
                    ps.setString(3, dentista.especialidade);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sqlPaciente)) {
                    ps.setLong(1, novoId);                 // FK = pessoa.id
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return novoId;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    public Dentista buscarDentistaPorPessoaId(int pessoaId) throws SQLException {
        String sql = "SELECT ID_DENTISTA, CRO, ESPECIALIDADE " +
                "FROM TB_DENTISTA WHERE ID_DENTISTA = ?";

        Connection conn = Conexao.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pessoaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dentista d = new Dentista();
                    d.id_dentista = rs.getInt("ID_DENTISTA");
                    d.cro = rs.getString("CRO");
                    d.especialidade = rs.getString("ESPECIALIDADE");
                    return d;
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar dentista por pessoaId: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    public void excluirPessoa(int pessoaId) throws SQLException {
        final String sql = "DELETE FROM TB_PESSOA WHERE ID_PESSOA = ?";

        Connection conn = Conexao.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pessoaId);
            int afetados = ps.executeUpdate();
            if (afetados == 0) {
                throw new SQLException("Pessoa não encontrada para exclusão (ID=" + pessoaId + ").");
            }
            // Graças ao ON DELETE CASCADE, dentista/paciente caem juntos.
        } catch (SQLException e) {
            throw new SQLException("Erro ao excluir pessoa: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    public void atualizarPessoa(Pessoa p) throws SQLException {
        final String sql =
                "UPDATE TB_PESSOA " +
                        "   SET NOME = ?, CPF = ?, TELEFONE = ?, EMAIL = ?, DT_NASCIMENTO = ? " +
                        " WHERE ID_PESSOA = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.nome);         // recomendo CPF/telefone como String
            ps.setString(2, p.cpf);
            ps.setString(3, p.telefone);
            ps.setString(4, p.email);

            if (p.dt_nascimento != null) {
                ps.setDate(5, new java.sql.Date(p.dt_nascimento.getTime()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }

            ps.setInt(6, p.id_pessoa);       // ID (PK) da pessoa

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Pessoa não encontrada para atualização (ID=" + p.id_pessoa + ").");
            }
        } catch (SQLException e) {
            // Tratamento elegante para violação de UNIQUE (ex.: CPF duplicado)
            String state = e.getSQLState();
            if ("23000".equals(state) || "23505".equals(state)) { // MySQL/MariaDB / H2/PostgreSQL
                throw new SQLException("CPF já cadastrado para outra pessoa.", e);
            }
            throw e;
        }
    }

    public void upsertDentista(int pessoaId, Dentista d) throws SQLException {
        // Se não é mais dentista, removemos o vínculo (se existir) e saímos.
        if (d == null) {
            final String sqlDel = "DELETE FROM TB_DENTISTA WHERE ID_DENTISTA = ?";
            try (Connection conn = Conexao.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlDel)) {
                ps.setInt(1, pessoaId);
                ps.executeUpdate(); // ok se 0 linhas (não existia)
            }
            return;
        }

        // Normaliza entradas (evita NPE e espaços)
        final String cro = d.cro == null ? "" : d.cro.trim();
        final String esp = d.especialidade == null ? "" : d.especialidade.trim();

        final String sqlUpd = "UPDATE TB_DENTISTA SET CRO = ?, ESPECIALIDADE = ? WHERE ID_DENTISTA = ?";
        final String sqlIns = "INSERT INTO TB_DENTISTA (ID_DENTISTA, CRO, ESPECIALIDADE) VALUES (?, ?, ?)";

        Connection conn = Conexao.getConnection();
        try {
            conn.setAutoCommit(false);

            int linhas;
            try (PreparedStatement up = conn.prepareStatement(sqlUpd)) {
                up.setString(1, cro);
                up.setString(2, esp);
                up.setInt(3, pessoaId);
                linhas = up.executeUpdate();
            }

            if (linhas == 0) {
                try (PreparedStatement ins = conn.prepareStatement(sqlIns)) {
                    ins.setInt(1, pessoaId);   // PK = FK para TB_PESSOA(ID)
                    ins.setString(2, cro);
                    ins.setString(3, esp);
                    ins.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw new SQLException("Erro no upsert de dentista: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }


}
