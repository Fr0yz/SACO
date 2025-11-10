package dao;

import model.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public class PagamentoDAO {

    public long inserir(Pagamento p) throws SQLException {
        String sql = """
            INSERT INTO TB_PAGAMENTO (ID_FINANCEIRO, VALOR, DT_PAGAMENTO, NUM_FATURA, NUM_BOLETO, STATUS)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, p.getId_financeiro());
            ps.setBigDecimal(2, p.getValor());
            ps.setTimestamp(3, new Timestamp(p.getDt_pagamento().getTime()));
            ps.setString(4, p.getNum_fatura());
            ps.setString(5, p.getNum_boleto());
            ps.setString(6, p.getStatus().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }

    public List<Pagamento> listarPorFinanceiro(long idFinanceiro) throws SQLException {
        String sql = "SELECT * FROM TB_PAGAMENTO WHERE ID_FINANCEIRO=? ORDER BY DT_PAGAMENTO DESC, ID_PAGAMENTO DESC";
        List<Pagamento> lista = new ArrayList<>();
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idFinanceiro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        }
        return lista;
    }

    public boolean atualizarStatus(long idPagamento, StatusPagamento status) throws SQLException {
        String sql = "UPDATE TB_PAGAMENTO SET STATUS=? WHERE ID_PAGAMENTO=?";
        try (Connection c = Conexao.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, idPagamento);
            return ps.executeUpdate() > 0;
        }
    }

    private Pagamento map(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento();
        p.setId_pagamento(rs.getLong("ID_PAGAMENTO"));
        p.setId_financeiro(rs.getLong("ID_FINANCEIRO"));
        p.setValor(rs.getBigDecimal("VALOR"));
        Timestamp ts = rs.getTimestamp("DT_PAGAMENTO");
        p.setDt_pagamento(ts != null ? new java.util.Date(ts.getTime()) : null);
        p.setNum_fatura(rs.getString("NUM_FATURA"));
        p.setNum_boleto(rs.getString("NUM_BOLETO"));
        p.setStatus(StatusPagamento.valueOf(rs.getString("STATUS")));
        return p;
    }
}
