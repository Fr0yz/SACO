package service;

import dao.AgendamentoDAO;
import model.Agendamento;
import model.StatusAgendamento;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AgendamentoService {

    private final AgendamentoDAO dao;
    private final DurationProvider durationProvider;

    // ======= Forma simples: usa 60 minutos por padrão =======
    public AgendamentoService() {
        this(new AgendamentoDAO(), idTratamento -> 60L);
    }

    public AgendamentoService(AgendamentoDAO dao) {
        this(dao, idTratamento -> 60L);
    }

    // ======= Forma avançada: injeta provedor de duração =======
    public AgendamentoService(AgendamentoDAO dao, DurationProvider durationProvider) {
        this.dao = Objects.requireNonNull(dao);
        this.durationProvider = Objects.requireNonNull(durationProvider);
    }

    /** Fornece a duração (em minutos) de um tratamento (ex.: vindo da TB_TRATAMENTO). */
    @FunctionalInterface
    public interface DurationProvider {
        long getDuracaoMinutos(int idTratamento) throws SQLException;
    }

    // === Consultas ===
    public List<Agendamento> listar() throws ServiceException {
        try {
            return dao.listar();
        } catch (SQLException e) {
            throw wrap("Erro ao listar agendamentos", e);
        }
    }

    public Agendamento buscarPorId(int id) throws ServiceException {
        if (id <= 0) throw new ServiceException("ID inválido.");
        try {
            Agendamento a = dao.buscarPorId(id);
            if (a == null) throw new ServiceException("Agendamento não encontrado (ID=" + id + ").");
            return a;
        } catch (SQLException e) {
            throw wrap("Erro ao buscar agendamento (ID=" + id + ")", e);
        }
    }

    // === Criação ===
    public int criar(Agendamento a) throws ServiceException {
        validarCamposObrigatorios(a);
        validarDataNoFuturo(a);

        try {
            long duracaoMin = durationProvider.getDuracaoMinutos(a.getId_tratamento());
            Timestamp inicio = ts(a.getData_hora());
            Timestamp fim = addMin(inicio, duracaoMin);

            if (dao.existeConflitoHorario(a.getId_dentista(), inicio, fim, null)) {
                throw new ServiceException("Conflito de agenda para o dentista no horário informado.");
            }

            return dao.cadastrar(a);
        } catch (SQLException e) {
            throw wrap("Erro ao criar agendamento", e);
        }
    }

    // === Atualização ===
    public void atualizar(Agendamento a) throws ServiceException {
        if (a == null || a.getId_agendamento() == null || a.getId_agendamento() <= 0) {
            throw new ServiceException("ID do agendamento é obrigatório.");
        }
        validarCamposObrigatorios(a);
        validarDataNoFuturo(a);

        try {
            // garante que existe
            buscarPorId(a.getId_agendamento());

            long duracaoMin = durationProvider.getDuracaoMinutos(a.getId_tratamento());
            Timestamp inicio = ts(a.getData_hora());
            Timestamp fim = addMin(inicio, duracaoMin);

            if (dao.existeConflitoHorario(a.getId_dentista(), inicio, fim, a.getId_agendamento())) {
                throw new ServiceException("Conflito de agenda para o dentista no horário informado.");
            }

            boolean ok = dao.atualizar(a);
            if (!ok) throw new ServiceException("Não foi possível atualizar o agendamento.");
        } catch (SQLException e) {
            throw wrap("Erro ao atualizar agendamento (ID=" + a.getId_agendamento() + ")", e);
        }
    }

    // === Exclusão ===
    public void excluir(int idAgendamento) throws ServiceException {
        if (idAgendamento <= 0) throw new ServiceException("ID inválido para exclusão.");
        try {
            // garante que existe
            buscarPorId(idAgendamento);

            boolean ok = dao.deletar(idAgendamento);
            if (!ok) throw new ServiceException("Não foi possível excluir o agendamento.");
        } catch (SQLException e) {
            throw wrap("Erro ao excluir agendamento (ID=" + idAgendamento + ")", e);
        }
    }

    // === Operações de status (opcional, mas comum em agenda) ===
    public void alterarStatus(int idAgendamento, String novoStatus) throws ServiceException {
        if (idAgendamento <= 0) throw new ServiceException("ID inválido.");
        if (novoStatus == null || novoStatus.isBlank()) throw new ServiceException("Status é obrigatório.");

        try {
            Agendamento atual = buscarPorId(idAgendamento);
            atual.setStatus(StatusAgendamento.valueOf(novoStatus));
            boolean ok = dao.atualizar(atual);
            if (!ok) throw new ServiceException("Não foi possível alterar o status.");
        } catch (SQLException e) {
            throw wrap("Erro ao alterar status (ID=" + idAgendamento + ")", e);
        }
    }

    // === Validações ===
    private void validarCamposObrigatorios(Agendamento a) throws ServiceException {
        if (a == null) throw new ServiceException("Agendamento não pode ser nulo.");
        if (a.getId_paciente() == null || a.getId_paciente() <= 0)
            throw new ServiceException("Paciente é obrigatório.");
        if (a.getId_dentista() == null || a.getId_dentista() <= 0)
            throw new ServiceException("Dentista é obrigatório.");
        if (a.getId_tratamento() == null || a.getId_tratamento() <= 0)
            throw new ServiceException("Tratamento é obrigatório.");
        if (a.getData_hora() == null)
            throw new ServiceException("Data/Hora é obrigatória.");
        if (a.getStatus() == null)
            throw new ServiceException("Status é obrigatório.");
        // se quiser, valide status permitido aqui
    }

    private void validarDataNoFuturo(Agendamento a) throws ServiceException {
        Date agora = new Date();
        if (a.getData_hora() != null && a.getData_hora().before(agora)) {
            throw new ServiceException("Data/Hora não pode estar no passado.");
        }
    }

    // === Utils ===
    private Timestamp ts(Date d) { return new Timestamp(d.getTime()); }
    private Timestamp addMin(Timestamp inicio, long minutos) { return new Timestamp(inicio.getTime() + minutos * 60_000L); }

    private ServiceException wrap(String msg, SQLException cause) {
        // mantém a mensagem raiz visível (útil pra diagnose), tal como no seu MaterialService
        return new ServiceException(msg + ": " + cause.getMessage(), cause);
    }

    // Exceção de domínio do service
    public static class ServiceException extends Exception {
        public ServiceException(String message) { super(message); }
        public ServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
