package dao;

import model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterialDAO {

    public List<Material> listar() throws SQLException {
        List<Material> lista = new ArrayList<>();
        String sql = "SELECT ID_MATERIAL, NOME, QUANTIDADE FROM TB_MATERIAL ORDER BY QUANTIDADE DESC";

        try (Connection conn = Conexao.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Material p = new Material();
                p.setID(rs.getInt("ID_MATERIAL"));
                p.setNOME(rs.getString("NOME"));
                p.setQUANTIDADE(rs.getInt("QUANTIDADE"));
                lista.add(p);
            }
        }
        return lista;
    }

    public long inserirMaterial(Material material) throws SQLException {
        final String sql = "INSERT INTO TB_MATERIAL (NOME, QUANTIDADE) VALUES (?, ?)";

        Connection conn = Conexao.getConnection();
        Long generatedId = null;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, material.getNOME());
                ps.setInt(2, material.getQUANTIDADE());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Nenhuma linha inserida em TB_MATERIAL.");
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Falha ao obter chave gerada de TB_MATERIAL.");
                    }
                    generatedId = keys.getLong(1);
                }
            }

            conn.commit();
            return generatedId;

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    ex.addSuppressed(rbEx);
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignore) {
                }
                try {
                    conn.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    public void excluirMaterial(int materialID) throws SQLException {
        final String sql = "DELETE FROM TB_MATERIAL WHERE ID_MATERIAL = ?";

        Connection conn = Conexao.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, materialID);
            int afetados = ps.executeUpdate();
            if (afetados == 0) {
                throw new SQLException("Material não encontrado para exclusão (ID=" + materialID + ").");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao excluir material: " + e.getMessage(), e);
        } finally {
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    public void atualizarMaterial(Material m) throws SQLException {
        final String sql =
                "UPDATE TB_MATERIAL SET NOME = ?, QUANTIDADE = ? WHERE ID_MATERIAL = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getNOME());
            ps.setInt(2, m.getQUANTIDADE());
            ps.setInt(3, m.getID()); // <--- faltava

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Material não encontrado para atualização (ID=" + m.getID() + ").");
            }
        }
    }

}
