// dao/FinanceiroDAO.java
package dao;

import model.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public class FinanceiroDAO {

    public long inserir(Financeiro f) throws SQLException {
        String sql = """
            INSERT INTO TB_FINANCEIRO (ID_AGENDAMENTO, VALOR_TOTAL, DT_EMISSAO, STATUS, METODO_PAGAMENTO)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, f.getId_agendamento());
            ps.setBigDecimal(2, f.getValor_total());
            ps.setTimestamp(3, new Timestamp(f.getDt_emissao().getTime()));
            ps.setString(4, f.getStatus().name());
            ps.setString(5, f.getMetodo_pagamento().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public boolean atualizar(Financeiro f) throws SQLException {
        String sql = """
            UPDATE TB_FINANCEIRO
            SET ID_AGENDAMENTO=?, VALOR_TOTAL=?, DT_EMISSAO=?, STATUS=?, METODO_PAGAMENTO=?
            WHERE ID_FINANCEIRO=?
            """;
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, f.getId_agendamento());
            ps.setBigDecimal(2, f.getValor_total());
            ps.setTimestamp(3, new Timestamp(f.getDt_emissao().getTime()));
            ps.setString(4, f.getStatus().name());
            ps.setString(5, f.getMetodo_pagamento().name());
            ps.setLong(6, f.getId_financeiro());
            return ps.executeUpdate() > 0;
        }
    }

    public Financeiro buscarPorId(long id) throws SQLException {
        String sql = "SELECT * FROM TB_FINANCEIRO WHERE ID_FINANCEIRO=?";
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Financeiro> listar() throws SQLException {
        String sql = "SELECT * FROM TB_FINANCEIRO ORDER BY DT_EMISSAO DESC, ID_FINANCEIRO DESC";
        List<Financeiro> lista = new ArrayList<>();
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(map(rs));
        }
        return lista;
    }

    public boolean deletar(long id) throws SQLException {
        String sql = "DELETE FROM TB_FINANCEIRO WHERE ID_FINANCEIRO=?";
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public BigDecimal somaPagamentos(long idFinanceiro) throws SQLException {
        String sql = "SELECT COALESCE(SUM(VALOR),0) AS TOTAL FROM TB_PAGAMENTO WHERE ID_FINANCEIRO=? AND STATUS='LIQUIDADO'";
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idFinanceiro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("TOTAL");
            }
        }
        return BigDecimal.ZERO;
    }

    private Financeiro map(ResultSet rs) throws SQLException {
        Financeiro f = new Financeiro();
        f.setId_financeiro(rs.getLong("ID_FINANCEIRO"));
        f.setId_agendamento(rs.getLong("ID_AGENDAMENTO"));
        f.setValor_total(rs.getBigDecimal("VALOR_TOTAL"));
        Timestamp ts = rs.getTimestamp("DT_EMISSAO");
        f.setDt_emissao(ts != null ? new java.util.Date(ts.getTime()) : null);
        f.setStatus(model.StatusFinanceiro.valueOf(rs.getString("STATUS")));
        f.setMetodo_pagamento(model.MetodoPagamento.valueOf(rs.getString("METODO_PAGAMENTO")));
        return f;
    }
}
