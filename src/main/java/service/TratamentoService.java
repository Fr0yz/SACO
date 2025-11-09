package service;

import dao.TratamentoDao;
import model.ConsumoMaterial;
import model.Material;
import model.Tratamento;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Camada de serviço para Tratamento:
 * - Regras/validações
 * - Orquestra chamadas ao DAO
 * - Converte exceções de SQL em ServiceException
 */
public class TratamentoService {

    private final TratamentoDao dao = new TratamentoDao();

    // ============================================================
    // ===================== EXCEÇÃO DE SERVIÇO ===================
    // ============================================================
    public static class ServiceException extends Exception {
        public ServiceException(String message) { super(message); }
        public ServiceException(String message, Throwable cause) { super(message, cause); }
    }

    private ServiceException wrap(String msg, Exception e) {
        return new ServiceException(msg + (e.getMessage() != null ? (": " + e.getMessage()) : ""), e);
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ============================================================
    // =========================== LISTAR =========================
    // ============================================================

    /** Retorna todos os tratamentos cadastrados. */
    public List<Tratamento> listar() throws ServiceException {
        try {
            return dao.listar();
        } catch (SQLException e) {
            throw wrap("Erro ao listar tratamentos", e);
        }
    }

    /** Lista materiais (com quantidade consumida) vinculados a um tratamento. */
    public List<Material> listarMateriaisPorTratamento(int idTratamento) throws ServiceException {
        try {
            return dao.listarMateriaisPorTratamento(idTratamento);
        } catch (SQLException e) {
            throw wrap("Erro ao listar materiais do tratamento", e);
        }
    }

    // ============================================================
    // ============================ CRIAR =========================
    // ============================================================

    /** Cria tratamento sem materiais. */
    public long criar(String nome, String descricao) throws ServiceException {
        return criar(nome, descricao, Collections.emptyList());
    }

    /**
     * Cria tratamento com consumos (material + quantidade).
     * @param nome         Nome do tratamento (obrigatório)
     * @param descricao    Descrição (opcional)
     * @param consumos     Lista de ConsumoMaterial (idMaterial + quantidade). Pode ser vazia.
     */
    public long criar(String nome, String descricao, List<ConsumoMaterial> consumos) throws ServiceException {
        String nomeOk = safeTrim(nome);
        String descOk = safeTrim(descricao);
        if (nomeOk.isBlank()) throw new ServiceException("Nome do tratamento é obrigatório.");

        try {
            long id = dao.salvarTratamento(nomeOk, descOk);

            if (consumos != null && !consumos.isEmpty()) {
                // Validação leve das quantidades
                for (ConsumoMaterial c : consumos) {
                    if (c == null) continue;
                    if (c.getIdMaterial() == null || c.getIdMaterial() <= 0)
                        throw new ServiceException("ID do material inválido no consumo.");
                    if (c.getQuantidade() == null || c.getQuantidade() <= 0)
                        throw new ServiceException("Quantidade deve ser maior que zero.");
                }
                dao.salvarConsumoMaterial(consumos, (int) id);
            }

            return id;
        } catch (SQLException e) {
            throw wrap("Erro ao criar tratamento", e);
        }
    }

    // ============================================================
    // =========================== ATUALIZAR ======================
    // ============================================================

    /** Atualiza somente dados básicos (nome/descrição). */
    public void atualizar(int idTratamento, String nome, String descricao) throws ServiceException {
        atualizar(idTratamento, nome, descricao, null); // null => não mexe nos consumos
    }

    /**
     * Atualiza dados e, opcionalmente, os consumos de materiais.
     * @param idTratamento     ID do tratamento
     * @param nome             Nome (obrigatório)
     * @param descricao        Descrição
     * @param novosConsumos    Nova lista de consumos (null = mantém; vazia = remove todos; lista = substitui)
     */
    public void atualizar(int idTratamento, String nome, String descricao, List<ConsumoMaterial> novosConsumos)
            throws ServiceException {

        if (idTratamento <= 0) throw new ServiceException("ID do tratamento inválido.");
        String nomeOk = safeTrim(nome);
        String descOk = safeTrim(descricao);
        if (nomeOk.isBlank()) throw new ServiceException("Nome do tratamento é obrigatório.");

        try {
            dao.atualizarTratamento(idTratamento, nomeOk, descOk);

            if (novosConsumos != null) {
                // validação leve
                for (ConsumoMaterial c : novosConsumos) {
                    if (c == null) continue;
                    if (c.getIdMaterial() == null || c.getIdMaterial() <= 0)
                        throw new ServiceException("ID do material inválido no consumo.");
                    if (c.getQuantidade() == null || c.getQuantidade() <= 0)
                        throw new ServiceException("Quantidade deve ser maior que zero.");
                }
                dao.atualizarConsumoMaterial(idTratamento, novosConsumos);
            }
        } catch (SQLException e) {
            throw wrap("Erro ao atualizar tratamento", e);
        }
    }

    // ============================================================
    // ============================ EXCLUIR =======================
    // ============================================================

    /** Exclui tratamento (e seus consumos). */
    public void excluir(int idTratamento) throws ServiceException {
        if (idTratamento <= 0) throw new ServiceException("ID do tratamento inválido.");
        try {
            dao.excluirTratamento(idTratamento);
        } catch (SQLException e) {
            throw wrap("Erro ao excluir tratamento", e);
        }
    }

    /** Exclui apenas um vínculo de material específico do tratamento. */
    public void excluirMaterialDoTratamento(int idTratamento, int idMaterial) throws ServiceException {
        if (idTratamento <= 0) throw new ServiceException("ID do tratamento inválido.");
        if (idMaterial <= 0)   throw new ServiceException("ID do material inválido.");
        try {
            dao.excluirConsumoMaterial(idTratamento, idMaterial);
        } catch (SQLException e) {
            throw wrap("Erro ao remover material do tratamento", e);
        }
    }

    // ============================================================
    // ======================= UTILIDADES =========================
    // ============================================================

    /**
     * Atalho para upsert de um único consumo (requer UNIQUE(ID_TRATAMENTO, ID_MATERIAL) no banco).
     * Útil para telas que adicionam/atualizam um material por vez.
     */
    public void upsertConsumo(int idTratamento, int idMaterial, int quantidade) throws ServiceException {
        if (idTratamento <= 0) throw new ServiceException("ID do tratamento inválido.");
        if (idMaterial   <= 0) throw new ServiceException("ID do material inválido.");
        if (quantidade   <= 0) throw new ServiceException("Quantidade deve ser maior que zero.");

        try {
            dao.upsertConsumoMaterial(idTratamento, idMaterial, quantidade);
        } catch (SQLException e) {
            throw wrap("Erro ao salvar consumo do material", e);
        }
    }

    /**
     * Compat: criar com apenas IDs de materiais (usa quantidade padrão = 1).
     * Obs.: prefira sempre a versão com List<ConsumoMaterial>.
     */
    public long criarComIds(String nome, String descricao, List<Integer> idsMateriais, int quantidadePadrao)
            throws ServiceException {
        String nomeOk = safeTrim(nome);
        if (nomeOk.isBlank()) throw new ServiceException("Nome do tratamento é obrigatório.");
        try {
            long id = dao.salvarTratamento(nomeOk, safeTrim(descricao));
            if (idsMateriais != null && !idsMateriais.isEmpty()) {
                dao.salvarConsumoMaterial(idsMateriais, (int) id, Math.max(1, quantidadePadrao));
            }
            return id;
        } catch (SQLException e) {
            throw wrap("Erro ao criar tratamento", e);
        }
    }
}
