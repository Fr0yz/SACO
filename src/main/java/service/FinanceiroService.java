// service/FinanceiroService.java
package service;

import dao.FinanceiroDAO;
import dao.PagamentoDAO;
import model.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FinanceiroService {

    private final FinanceiroDAO finDAO;
    private final PagamentoDAO pagDAO;

    public FinanceiroService() {
        this.finDAO = new FinanceiroDAO();
        this.pagDAO = new PagamentoDAO();
    }
    public FinanceiroService(FinanceiroDAO f, PagamentoDAO p) {
        this.finDAO = Objects.requireNonNull(f);
        this.pagDAO = Objects.requireNonNull(p);
    }

    // ===== CRUD Financeiro =====
    public long emitir(Long idAgendamento, BigDecimal valorTotal, MetodoPagamento metodo) throws ServiceException {
        if (idAgendamento == null || idAgendamento <= 0) throw new ServiceException("Agendamento inválido.");
        if (valorTotal == null || valorTotal.signum() <= 0) throw new ServiceException("Valor total deve ser > 0.");
        if (metodo == null) throw new ServiceException("Método de pagamento é obrigatório.");

        Financeiro f = new Financeiro();
        f.setId_agendamento(idAgendamento);
        f.setValor_total(valorTotal);
        f.setDt_emissao(new Date());
        f.setStatus(StatusFinanceiro.ABERTO);
        f.setMetodo_pagamento(metodo);

        try {
            long id = finDAO.inserir(f);
            if (id <= 0) throw new ServiceException("Falha ao emitir financeiro.");
            return id;
        } catch (SQLException e) {
            throw wrap("Erro ao emitir financeiro", e);
        }
    }

    public Financeiro buscarPorId(long id) throws ServiceException {
        try {
            Financeiro f = finDAO.buscarPorId(id);
            if (f == null) throw new ServiceException("Financeiro não encontrado (ID=" + id + ")");
            return f;
        } catch (SQLException e) {
            throw wrap("Erro ao buscar financeiro (ID=" + id + ")", e);
        }
    }

    public List<Financeiro> listar() throws ServiceException {
        try { return finDAO.listar(); }
        catch (SQLException e) { throw wrap("Erro ao listar financeiros", e); }
    }

    public void cancelar(long id) throws ServiceException {
        try {
            Financeiro f = buscarPorId(id);
            if (f.getStatus() == StatusFinanceiro.QUITADO)
                throw new ServiceException("Título já quitado. Não é possível cancelar.");
            f.setStatus(StatusFinanceiro.CANCELADO);
            if (!finDAO.atualizar(f)) throw new ServiceException("Não foi possível cancelar o financeiro.");
        } catch (SQLException e) {
            throw wrap("Erro ao cancelar financeiro (ID=" + id + ")", e);
        }
    }

    // ===== Pagamentos / Baixas =====
    public long registrarPagamento(long idFinanceiro,
                                   BigDecimal valor,
                                   Date dtPagamento,
                                   String numFatura,
                                   String numBoleto,
                                   StatusPagamento status) throws ServiceException {

        if (idFinanceiro <= 0) throw new ServiceException("Financeiro inválido.");
        if (valor == null || valor.signum() <= 0) throw new ServiceException("Valor do pagamento deve ser > 0.");
        if (dtPagamento == null) dtPagamento = new Date();
        if (status == null) status = StatusPagamento.LIQUIDADO;

        try {
            Financeiro f = buscarPorId(idFinanceiro);
            if (f.getStatus() == StatusFinanceiro.CANCELADO)
                throw new ServiceException("Título cancelado. Não é possível registrar pagamento.");

            Pagamento p = new Pagamento();
            p.setId_financeiro(idFinanceiro);
            p.setValor(valor);
            p.setDt_pagamento(dtPagamento);
            p.setNum_fatura(numFatura);
            p.setNum_boleto(numBoleto);
            p.setStatus(status);

            long idPg = pagDAO.inserir(p);

            // Se o pagamento foi LIQUIDADO, avalia quitação
            if (status == StatusPagamento.LIQUIDADO) {
                BigDecimal totalPago = finDAO.somaPagamentos(idFinanceiro);
                if (totalPago.compareTo(f.getValor_total()) >= 0) {
                    f.setStatus(StatusFinanceiro.QUITADO);
                    finDAO.atualizar(f);
                }
            }

            return idPg;
        } catch (SQLException e) {
            throw wrap("Erro ao registrar pagamento", e);
        }
    }

    public List<Pagamento> listarPagamentos(long idFinanceiro) throws ServiceException {
        try { return pagDAO.listarPorFinanceiro(idFinanceiro); }
        catch (SQLException e) { throw wrap("Erro ao listar pagamentos", e); }
    }

    public void estornarPagamento(long idPagamento, long idFinanceiro) throws ServiceException {
        try {
            if (!pagDAO.atualizarStatus(idPagamento, StatusPagamento.ESTORNADO))
                throw new ServiceException("Não foi possível estornar pagamento.");

            Financeiro f = buscarPorId(idFinanceiro);
            if (f.getStatus() == StatusFinanceiro.QUITADO) {
                BigDecimal totalPago = finDAO.somaPagamentos(idFinanceiro);
                if (totalPago.compareTo(f.getValor_total()) < 0) {
                    f.setStatus(StatusFinanceiro.ABERTO);
                    finDAO.atualizar(f);
                }
            }
        } catch (SQLException e) {
            throw wrap("Erro ao estornar pagamento", e);
        }
    }

    // ===== Util =====
    private ServiceException wrap(String msg, SQLException cause) {
        return new ServiceException(msg + ": " + cause.getMessage(), cause);
    }

    public static class ServiceException extends Exception {
        public ServiceException(String m){ super(m); }
        public ServiceException(String m, Throwable c){ super(m,c); }
    }
}
