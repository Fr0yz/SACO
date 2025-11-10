package dao;

import model.Agendamento;
import model.Material;
import model.StatusAgendamento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgendamentoDAO {

    public List<Agendamento> listar() throws SQLException {
        List<Agendamento> lista = new ArrayList<>();

        String sql = """
        SELECT 
            ag.ID_AGENDAMENTO,
            ag.DATA_HORA,
            ag.STATUS,
            ag.OBSERVACOES,
            pa.ID_PACIENTE,
            pePac.NOME AS NOME_PACIENTE,
            de.ID_DENTISTA,
            peDen.NOME AS NOME_DENTISTA,
            tr.ID_TRATAMENTO,
            tr.DESCRICAO AS NOME_TRATAMENTO
        FROM TB_AGENDAMENTO ag
        INNER JOIN TB_PACIENTE pa ON ag.ID_PACIENTE = pa.ID_PACIENTE
        INNER JOIN TB_PESSOA pePac ON pa.ID_PACIENTE = pePac.ID_PESSOA
        INNER JOIN TB_DENTISTA de ON ag.ID_DENTISTA = de.ID_DENTISTA
        INNER JOIN TB_PESSOA peDen ON de.ID_DENTISTA = peDen.ID_PESSOA
        INNER JOIN TB_TRATAMENTO tr ON ag.ID_TRATAMENTO = tr.ID_TRATAMENTO
        ORDER BY ag.DATA_HORA DESC
        """;

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Agendamento a = new Agendamento();
                a.setId_agendamento(rs.getInt("ID_AGENDAMENTO"));
                a.setId_paciente(rs.getInt("ID_PACIENTE"));
                a.setId_dentista(rs.getInt("ID_DENTISTA"));
                a.setId_tratamento(rs.getInt("ID_TRATAMENTO"));
                a.setData_hora(rs.getTimestamp("DATA_HORA"));
                a.setStatus(StatusAgendamento.fromString(rs.getString("STATUS")));
                a.setObservacoes(rs.getString("OBSERVACOES"));

                // novos campos só para exibição
                a.setNomePaciente(rs.getString("NOME_PACIENTE"));
                a.setNomeDentista(rs.getString("NOME_DENTISTA"));
                a.setNomeTratamento(rs.getString("NOME_TRATAMENTO"));

                lista.add(a);
            }
        }

        return lista;
    }

    public int cadastrar(Agendamento a) throws SQLException {
        String sql = "INSERT INTO TB_AGENDAMENTO " +
                "(ID_PACIENTE, ID_DENTISTA, ID_TRATAMENTO, DATA_HORA, STATUS, OBSERVACOES) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // <- peça as chaves

            ps.setInt(1, a.getId_paciente());
            ps.setInt(2, a.getId_dentista());
            ps.setInt(3, a.getId_tratamento());
            ps.setTimestamp(4, new java.sql.Timestamp(a.getData_hora().getTime()));
            ps.setString(5, a.getStatus().name());
            if (a.getObservacoes() == null) ps.setNull(6, java.sql.Types.VARCHAR); else ps.setString(6, a.getObservacoes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int idGerado = rs.getInt(1);
                    a.setId_agendamento(idGerado);
                    return idGerado;
                }
            }
        }
        return -1;
    }


    public boolean atualizar(Agendamento a) throws SQLException {
        String sql = "UPDATE TB_AGENDAMENTO SET " +
                "ID_PACIENTE = ?, ID_DENTISTA = ?, ID_TRATAMENTO = ?, " +
                "DATA_HORA = ?, STATUS = ?, OBSERVACOES = ? " +
                "WHERE ID_AGENDAMENTO = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, a.getId_paciente());
            ps.setInt(2, a.getId_dentista());
            ps.setInt(3, a.getId_tratamento());
            ps.setTimestamp(4, new java.sql.Timestamp(a.getData_hora().getTime()));
            ps.setString(5, a.getStatus().name());
            ps.setString(6, a.getObservacoes());
            ps.setInt(7, a.getId_agendamento());

            int linhas = ps.executeUpdate();
            return linhas > 0;
        }
    }

    public boolean deletar(int idAgendamento) throws SQLException {
        String sql = "DELETE FROM TB_AGENDAMENTO WHERE ID_AGENDAMENTO = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idAgendamento);
            int linhas = ps.executeUpdate();
            return linhas > 0;
        }
    }

    public boolean existeConflitoHorario(int idDentista, Timestamp inicio, Timestamp fim, Integer ignorarId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT 1 ")
                .append("FROM TB_AGENDAMENTO ")
                .append("WHERE ID_DENTISTA = ? ")
                // sobreposição: inicioA < fimB AND fimA > inicioB
                .append("AND DATA_HORA < ? ")
                .append("AND DATE_ADD(DATA_HORA, INTERVAL 60 MINUTE) > ? ");

        if (ignorarId != null) {
            sb.append("AND ID_AGENDAMENTO <> ? ");
        }
        sb.append("LIMIT 1");

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int i = 1;
            ps.setInt(i++, idDentista);
            ps.setTimestamp(i++, fim);
            ps.setTimestamp(i++, inicio);
            if (ignorarId != null) ps.setInt(i++, ignorarId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Agendamento buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM TB_AGENDAMENTO WHERE ID_AGENDAMENTO = ?";

        try (Connection conn = Conexao.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Agendamento a = new Agendamento();
                    a.setId_agendamento(rs.getInt("ID_AGENDAMENTO"));
                    a.setId_paciente(rs.getInt("ID_PACIENTE"));
                    a.setId_dentista(rs.getInt("ID_DENTISTA"));
                    a.setId_tratamento(rs.getInt("ID_TRATAMENTO"));

                    Timestamp ts = rs.getTimestamp("DATA_HORA");
                    if (ts != null) {
                        a.setData_hora(new java.util.Date(ts.getTime()));
                    }

                    a.setStatus(StatusAgendamento.fromString(rs.getString("STATUS")));
                    a.setObservacoes(rs.getString("OBSERVACOES"));

                    return a;
                }
            }
        }
        return null; // não encontrado
    }


}
