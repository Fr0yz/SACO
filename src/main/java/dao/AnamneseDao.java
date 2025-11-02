package dao;

import model.Anamnese;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por manipular tb_anamnese e tb_odontograma.
 * - Cada paciente tem 1 anamnese e 0/1 odontograma.
 */
public class AnamneseDao {

    /* ----------------- ANAMNESE ----------------- */

    public void inserirOuAtualizarAnamnese(Anamnese a) throws SQLException {
        String sqlVerifica = "SELECT ID_ANAMNESE FROM TB_ANAMNESE WHERE ID_PACIENTE = ?";
        String sqlInsert = """
                INSERT INTO TB_ANAMNESE (ID_PACIENTE, ALERGIAS, HISTORICO_MEDICO, MEDICAMENTOS, DETALHES, DATA_REGISTRO)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;
        String sqlUpdate = """
                UPDATE TB_ANAMNESE
                SET ALERGIAS=?, HISTORICO_MEDICO=?, MEDICAMENTOS=?, DETALHES=?, DATA_REGISTRO=NOW()
                WHERE ID_PACIENTE=?
                """;

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement check = conn.prepareStatement(sqlVerifica)) {
                check.setLong(1, a.id_paciente);
                ResultSet rs = check.executeQuery();

                boolean existe = rs.next();
                if (existe) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                        ps.setString(1, a.alergias);
                        ps.setString(2, a.historico_medico);
                        ps.setString(3, a.medicamentos);
                        ps.setString(4, a.detalhes);
                        ps.setLong(5, a.id_paciente);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                        ps.setLong(1, a.id_paciente);
                        ps.setString(2, a.alergias);
                        ps.setString(3, a.historico_medico);
                        ps.setString(4, a.medicamentos);
                        ps.setString(5, a.detalhes);
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
        }
    }

    public Anamnese buscarPorPaciente(long idPaciente) throws SQLException {
        String sql = "SELECT * FROM TB_ANAMNESE WHERE ID_PACIENTE = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Anamnese a = new Anamnese();
                    a.id_anamnese = rs.getLong("ID_ANAMNESE");
                    a.id_paciente = rs.getLong("ID_PACIENTE");
                    a.alergias = rs.getString("ALERGIAS");
                    a.historico_medico = rs.getString("HISTORICO_MEDICO");
                    a.medicamentos = rs.getString("MEDICAMENTOS");
                    a.detalhes = rs.getString("DETALHES");
                    a.data_registro = rs.getTimestamp("DATA_REGISTRO");
                    a.imagem_odontograma = buscarImagemOdontograma(idPaciente);
                    return a;
                }
                return null;
            }
        }
    }

    public List<Anamnese> listarTodas() throws SQLException {
        List<Anamnese> lista = new ArrayList<>();
        String sql = "SELECT * FROM TB_ANAMNESE ORDER BY DATA_REGISTRO DESC";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Anamnese a = new Anamnese();
                a.id_anamnese = rs.getLong("ID_ANAMNESE");
                a.id_paciente = rs.getLong("ID_PACIENTE");
                a.alergias = rs.getString("ALERGIAS");
                a.historico_medico = rs.getString("HISTORICO_MEDICO");
                a.medicamentos = rs.getString("MEDICAMENTOS");
                a.detalhes = rs.getString("DETALHES");
                a.data_registro = rs.getTimestamp("DATA_REGISTRO");
                lista.add(a);
            }
        }
        return lista;
    }

    public void excluirPorPaciente(long idPaciente) throws SQLException {
        String sqlA = "DELETE FROM TB_ANAMNESE WHERE ID_PACIENTE = ?";
        String sqlO = "DELETE FROM TB_ODONTOGRAMA WHERE ID_PACIENTE = ?";
        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(sqlO);
                 PreparedStatement p2 = conn.prepareStatement(sqlA)) {
                p1.setLong(1, idPaciente);
                p2.setLong(1, idPaciente);
                p1.executeUpdate();
                p2.executeUpdate();
            }
            conn.commit();
        }
    }

    /* ----------------- ODONTOGRAMA ----------------- */

    public void salvarOuAtualizarImagem(long idPaciente, byte[] imagem) throws SQLException {
        if (imagem == null) return;
        String sqlUpdate = "UPDATE TB_ODONTOGRAMA SET IMAGEM_REF=?, DATA_CRIACAO=NOW() WHERE ID_PACIENTE=?";
        String sqlInsert = "INSERT INTO TB_ODONTOGRAMA (ID_PACIENTE, IMAGEM_REF, DATA_CRIACAO) VALUES (?, ?, NOW())";
        String sqlVerifica = "SELECT ID_ODONTOGRAMA FROM TB_ODONTOGRAMA WHERE ID_PACIENTE = ?";

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);

            boolean existe;
            try (PreparedStatement check = conn.prepareStatement(sqlVerifica)) {
                check.setLong(1, idPaciente);
                ResultSet rs = check.executeQuery();
                existe = rs.next();
            }

            if (existe) {
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setBytes(1, imagem);
                    ps.setLong(2, idPaciente);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setLong(1, idPaciente);
                    ps.setBytes(2, imagem);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    public byte[] buscarImagemOdontograma(long idPaciente) throws SQLException {
        String sql = "SELECT IMAGEM_REF FROM TB_ODONTOGRAMA WHERE ID_PACIENTE = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBytes("IMAGEM_REF");
            return null;
        }
    }
    // UPDATE da anamnese pelos campos (usa ID_PACIENTE)
    public void atualizar(Anamnese a) throws SQLException {
        final String sql = """
        UPDATE TB_ANAMNESE
           SET ALERGIAS=?, HISTORICO_MEDICO=?, MEDICAMENTOS=?, DETALHES=?, DATA_REGISTRO=NOW()
         WHERE ID_PACIENTE=?
    """;
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.alergias);
            ps.setString(2, a.historico_medico);
            ps.setString(3, a.medicamentos);
            ps.setString(4, a.detalhes);
            ps.setLong(5, a.id_paciente);
            ps.executeUpdate();
        }
    }

    // Excluir por ID de anamnese (útil quando listar todas e clicar excluir)
    public void excluirPorId(long idAnamnese) throws SQLException {
        final String sql = "DELETE FROM TB_ANAMNESE WHERE ID_ANAMNESE = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idAnamnese);
            ps.executeUpdate();
        }
    }

    // Remover apenas a imagem do odontograma (deixa a anamnese)
    public void removerImagemOdontograma(long idPaciente) throws SQLException {
        final String sql = "DELETE FROM TB_ODONTOGRAMA WHERE ID_PACIENTE = ?";
        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            ps.executeUpdate();
        }
    }

    /**
     * Transação completa:
     * - Upsert da anamnese (inserirOuAtualizar)
     * - Se houver imagem -> upsert da imagem
     * - Se NÃO houver imagem -> remove imagem (se existir)
     */
    public void salvarCompleto(Anamnese a) throws SQLException {
        final String sqlVerificaAna = "SELECT ID_ANAMNESE FROM TB_ANAMNESE WHERE ID_PACIENTE = ?";
        final String sqlInsertAna = """
        INSERT INTO TB_ANAMNESE (ID_PACIENTE, ALERGIAS, HISTORICO_MEDICO, MEDICAMENTOS, DETALHES, DATA_REGISTRO)
        VALUES (?, ?, ?, ?, ?, NOW())
    """;
        final String sqlUpdateAna = """
        UPDATE TB_ANAMNESE
           SET ALERGIAS=?, HISTORICO_MEDICO=?, MEDICAMENTOS=?, DETALHES=?, DATA_REGISTRO=NOW()
         WHERE ID_PACIENTE=?
    """;

        final String sqlVerificaOdo = "SELECT ID_ODONTOGRAMA FROM TB_ODONTOGRAMA WHERE ID_PACIENTE = ?";
        final String sqlInsertOdo = """
        INSERT INTO TB_ODONTOGRAMA (ID_PACIENTE, IMAGEM_REF, DATA_CRIACAO)
        VALUES (?, ?, NOW())
    """;
        final String sqlUpdateOdo = """
        UPDATE TB_ODONTOGRAMA
           SET IMAGEM_REF=?, DATA_CRIACAO=NOW()
         WHERE ID_PACIENTE=?
    """;
        final String sqlDeleteOdo = "DELETE FROM TB_ODONTOGRAMA WHERE ID_PACIENTE=?";

        Connection conn = Conexao.getConnection();
        try {
            conn.setAutoCommit(false);

            // Upsert ANAMNESE
            boolean existeAna;
            try (PreparedStatement check = conn.prepareStatement(sqlVerificaAna)) {
                check.setLong(1, a.id_paciente);
                try (ResultSet rs = check.executeQuery()) {
                    existeAna = rs.next();
                }
            }

            if (existeAna) {
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdateAna)) {
                    ps.setString(1, a.alergias);
                    ps.setString(2, a.historico_medico);
                    ps.setString(3, a.medicamentos);
                    ps.setString(4, a.detalhes);
                    ps.setLong(5, a.id_paciente);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertAna)) {
                    ps.setLong(1, a.id_paciente);
                    ps.setString(2, a.alergias);
                    ps.setString(3, a.historico_medico);
                    ps.setString(4, a.medicamentos);
                    ps.setString(5, a.detalhes);
                    ps.executeUpdate();
                }
            }

            // Odontograma: upsert ou remove
            if (a.imagem_odontograma != null && a.imagem_odontograma.length > 0) {
                boolean existeOdo;
                try (PreparedStatement checkO = conn.prepareStatement(sqlVerificaOdo)) {
                    checkO.setLong(1, a.id_paciente);
                    try (ResultSet rs = checkO.executeQuery()) {
                        existeOdo = rs.next();
                    }
                }
                if (existeOdo) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlUpdateOdo)) {
                        ps.setBytes(1, a.imagem_odontograma);
                        ps.setLong(2, a.id_paciente);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsertOdo)) {
                        ps.setLong(1, a.id_paciente);
                        ps.setBytes(2, a.imagem_odontograma);
                        ps.executeUpdate();
                    }
                }
            } else {
                // veio sem imagem -> remove registro do odontograma (se existir)
                try (PreparedStatement del = conn.prepareStatement(sqlDeleteOdo)) {
                    del.setLong(1, a.id_paciente);
                    del.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignore) {}
            try { conn.close(); } catch (SQLException ignore) {}
        }
    }

}
