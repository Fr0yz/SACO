package dao;

import model.ConsumoMaterial;
import model.Material;
import model.Tratamento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TratamentoDao {

    // ============================================================
    // ========================== LISTAR ==========================
    // ============================================================

    public List<Tratamento> listar() throws SQLException {
        List<Tratamento> lista = new ArrayList<>();
        String sql = "SELECT ID_TRATAMENTO, NOME, DESCRICAO FROM TB_TRATAMENTO ORDER BY NOME DESC";

        try (Connection conn = Conexao.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Tratamento t = new Tratamento();
                t.setId_tratamento(rs.getInt("ID_TRATAMENTO"));
                t.setNome(rs.getString("NOME"));
                t.setDescricao(rs.getString("DESCRICAO"));
                lista.add(t);
            }
        }
        return lista;
    }

    // ============================================================
    // ========================== CRIAR ===========================
    // ============================================================

    /** Insere o tratamento e retorna o ID gerado. */
    public long salvarTratamento(String nome, String descricao) throws SQLException {
        String sql = "INSERT INTO TB_TRATAMENTO (NOME, DESCRICAO) VALUES (?, ?)";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nome);
            ps.setString(2, descricao);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("Falha ao obter ID gerado para TB_TRATAMENTO.");
            }
        }
    }

    // ============================================================
    // =============== CONSUMO: INSERIR (COM QTD) =================
    // ============================================================

    /**
     * Salva consumos (material + quantidade) para um tratamento.
     * Espera que a coluna QUANTIDADE exista em TB_CONSUMO_MATERIAL.
     */
    public void salvarConsumoMaterial(List<ConsumoMaterial> consumos, int idTratamento) throws SQLException {
        String sql = "INSERT INTO TB_CONSUMO_MATERIAL (ID_TRATAMENTO, ID_MATERIAL, QUANTIDADE) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (ConsumoMaterial c : consumos) {
                ps.setInt(1, idTratamento);
                ps.setInt(2, c.getIdMaterial());
                ps.setInt(3, c.getQuantidade());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            throw e;
        }
    }

    // (Compatibilidade) Versão antiga que recebia apenas IDs (sem quantidade) — assume quantidade = 1
    public void salvarConsumoMaterial(List<Integer> idsMateriais, int idTratamento, int quantidadePadrao) throws SQLException {
        if (idsMateriais == null || idsMateriais.isEmpty()) return;
        List<ConsumoMaterial> conv = new ArrayList<>();
        for (Integer idMat : idsMateriais) {
            conv.add(new ConsumoMaterial(idMat, Math.max(1, quantidadePadrao)));
        }
        salvarConsumoMaterial(conv, idTratamento);
    }

    // ============================================================
    // ====================== ATUALIZAR TRATAMENTO ================
    // ============================================================

    public void atualizarTratamento(int idTratamento, String nome, String descricao) throws SQLException {
        String sql = "UPDATE TB_TRATAMENTO SET NOME = ?, DESCRICAO = ? WHERE ID_TRATAMENTO = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nome);
            ps.setString(2, descricao);
            ps.setInt(3, idTratamento);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Nenhum tratamento atualizado (ID inexistente?).");
            }
        }
    }

    // ============================================================
    // ===================== EXCLUIR TRATAMENTO ===================
    // ============================================================

    public void excluirTratamento(int idTratamento) throws SQLException {
        String sqlDelConsumos = "DELETE FROM TB_CONSUMO_MATERIAL WHERE ID_TRATAMENTO = ?";
        String sqlDelTrat     = "DELETE FROM TB_TRATAMENTO WHERE ID_TRATAMENTO = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps1 = conn.prepareStatement(sqlDelConsumos);
             PreparedStatement ps2 = conn.prepareStatement(sqlDelTrat)) {

            conn.setAutoCommit(false);
            try {
                ps1.setInt(1, idTratamento);
                ps1.executeUpdate();

                ps2.setInt(1, idTratamento);
                int affected = ps2.executeUpdate();

                if (affected == 0) {
                    conn.rollback();
                    throw new SQLException("Nenhum tratamento excluído (ID inexistente?).");
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // ============================================================
    // ============ CONSUMO: ATUALIZAR (APAGA E RECRIA) ===========
    // ============================================================

    /**
     * Substitui os consumos do tratamento: apaga todos e insere a nova lista (com quantidades).
     * Se a lista for vazia, fica sem nenhum consumo.
     * Se usar UNIQUE(ID_TRATAMENTO, ID_MATERIAL), essa abordagem evita conflitos.
     */
    public void atualizarConsumoMaterial(int idTratamento, List<ConsumoMaterial> novosConsumos) throws SQLException {
        String sqlDel = "DELETE FROM TB_CONSUMO_MATERIAL WHERE ID_TRATAMENTO = ?";
        String sqlIns = "INSERT INTO TB_CONSUMO_MATERIAL (ID_TRATAMENTO, ID_MATERIAL, QUANTIDADE) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement psDel = conn.prepareStatement(sqlDel);
             PreparedStatement psIns = conn.prepareStatement(sqlIns)) {

            conn.setAutoCommit(false);
            try {
                // remove todos os vínculos atuais
                psDel.setInt(1, idTratamento);
                psDel.executeUpdate();

                // recria conforme a nova lista (se houver)
                if (novosConsumos != null && !novosConsumos.isEmpty()) {
                    for (ConsumoMaterial c : novosConsumos) {
                        psIns.setInt(1, idTratamento);
                        psIns.setInt(2, c.getIdMaterial());
                        psIns.setInt(3, c.getQuantidade());
                        psIns.addBatch();
                    }
                    psIns.executeBatch();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    // (Compatibilidade) Atualizar consumos recebendo só IDs — assume quantidade = 1
    public void atualizarConsumoMaterialIds(int idTratamento, List<Integer> novosIdsMateriais) throws SQLException {
        List<ConsumoMaterial> conv = new ArrayList<>();
        if (novosIdsMateriais != null) {
            for (Integer idMat : novosIdsMateriais) {
                conv.add(new ConsumoMaterial(idMat, 1));
            }
        }
        atualizarConsumoMaterial(idTratamento, conv);
    }

    // ============================================================
    // =================== CONSUMO: EXCLUIR ITEM ==================
    // ============================================================

    public void excluirConsumoMaterial(int idTratamento, int idMaterial) throws SQLException {
        String sql = "DELETE FROM TB_CONSUMO_MATERIAL WHERE ID_TRATAMENTO = ? AND ID_MATERIAL = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTratamento);
            ps.setInt(2, idMaterial);

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Nenhum vínculo tratamento-material removido (par não encontrado).");
            }
        }
    }

    // ============================================================
    // ============ LISTAR CONSUMOS (JOIN COM MATERIAL) ===========
    // ============================================================

    /**
     * Retorna os materiais vinculados a um tratamento, incluindo a quantidade consumida.
     * Se preferir não mexer no modelo Material, crie um DTO específico para esta consulta.
     */
    public List<Material> listarMateriaisPorTratamento(int idTratamento) throws SQLException {
        String sql = """
            SELECT m.ID_MATERIAL,
                   m.NOME,
                   m.QUANTIDADE AS ESTOQUE_ATUAL,
                   c.QUANTIDADE AS QTD_CONSUMO
              FROM TB_CONSUMO_MATERIAL c
              JOIN TB_MATERIAL m ON m.ID_MATERIAL = c.ID_MATERIAL
             WHERE c.ID_TRATAMENTO = ?
             ORDER BY m.NOME
            """;

        List<Material> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTratamento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Material m = new Material();
                    m.setID(rs.getInt("ID_MATERIAL"));
                    m.setNOME(rs.getString("NOME"));
                    m.setQUANTIDADE(rs.getInt("ESTOQUE_ATUAL"));
                    // SUGESTÃO: adicione um campo transient em Material (ex.: qtdConsumo) + getters/setters
                    // ou troque o retorno para um DTO MaterialConsumoDTO.
                    try {
                        // se você criou setQtdConsumo no modelo:
                        m.setQtdConsumo(rs.getInt("QTD_CONSUMO"));
                    } catch (NoSuchMethodError | Exception ignore) {
                        // ignore se o método não existir; opte por um DTO se preferir
                    }
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    // ============================================================
    // ======== (Opcional) UPSERT de consumo por material =========
    // ============================================================

    /**
     * Faz "upsert" de um único material consumido:
     * - se não existe, insere (quantidade);
     * - se já existe, atualiza a quantidade.
     * Requer UNIQUE(ID_TRATAMENTO, ID_MATERIAL) para funcionar bem.
     */
    public void upsertConsumoMaterial(int idTratamento, int idMaterial, int quantidade) throws SQLException {
        // Exemplo para bancos que suportam MERGE/ON CONFLICT:
        // Ajuste conforme seu SGBD (Postgres, MySQL, SQL Server, etc.)
        String driver = Conexao.getDriverName().toLowerCase();

        String sql;
        if (driver.contains("postgresql")) {
            sql = """
                INSERT INTO TB_CONSUMO_MATERIAL (ID_TRATAMENTO, ID_MATERIAL, QUANTIDADE)
                VALUES (?, ?, ?)
                ON CONFLICT (ID_TRATAMENTO, ID_MATERIAL)
                DO UPDATE SET QUANTIDADE = EXCLUDED.QUANTIDADE
                """;
        } else if (driver.contains("mysql")) {
            sql = """
                INSERT INTO TB_CONSUMO_MATERIAL (ID_TRATAMENTO, ID_MATERIAL, QUANTIDADE)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE QUANTIDADE = VALUES(QUANTIDADE)
                """;
        } else {
            // fallback: tenta atualizar; se não afetar linhas, insere
            try (Connection conn = Conexao.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE TB_CONSUMO_MATERIAL SET QUANTIDADE = ? WHERE ID_TRATAMENTO = ? AND ID_MATERIAL = ?")) {
                    upd.setInt(1, quantidade);
                    upd.setInt(2, idTratamento);
                    upd.setInt(3, idMaterial);
                    int affected = upd.executeUpdate();
                    if (affected == 0) {
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO TB_CONSUMO_MATERIAL (ID_TRATAMENTO, ID_MATERIAL, QUANTIDADE) VALUES (?, ?, ?)")) {
                            ins.setInt(1, idTratamento);
                            ins.setInt(2, idMaterial);
                            ins.setInt(3, quantidade);
                            ins.executeUpdate();
                        }
                    }
                    conn.commit();
                    return;
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            }
        }

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTratamento);
            ps.setInt(2, idMaterial);
            ps.setInt(3, quantidade);
            ps.executeUpdate();
        }
    }
}
