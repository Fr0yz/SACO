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
        String sql = "INSERT INTO TB_MATERIAL (NOME, QUANTIDADE) VALUES (?, ?)";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, material.getNOME());
            ps.setInt(2, material.getQUANTIDADE());

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Nenhuma linha inserida em TB_MATERIAL.");
            }

        } catch (SQLException e) {
            throw new SQLException("Erro ao inserir material: " + e.getMessage(), e);
        }
        return 0;
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
            ps.setInt(3, m.getID());

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Material não encontrado para atualização (ID=" + m.getID() + ").");
            }
        }
    }

}
