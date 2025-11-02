package service;

import dao.MaterialDAO;
import model.Material;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class MaterialService {

    private final MaterialDAO dao;

    public MaterialService() {
        this.dao = new MaterialDAO();
    }

    public MaterialService(MaterialDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    // === Consultas ===
    public List<Material> listar() throws ServiceException {
        try {
            return dao.listar();
        } catch (SQLException e) {
            throw wrap("Erro ao listar materiais", e);
        }
    }

    // === Criação ===
    public long criar(String nome, Integer quantidade) throws ServiceException {
        // validações
        if (nome == null || nome.isBlank()) {
            throw new ServiceException("Nome do material é obrigatório.");
        }
        int qtd = (quantidade == null ? 0 : quantidade);
        if (qtd < 0) {
            throw new ServiceException("Quantidade inicial não pode ser negativa.");
        }

        Material m = new Material();
        m.setNOME(nome.trim());
        m.setQUANTIDADE(qtd);

        try {
            return dao.inserirMaterial(m);
        } catch (SQLException e) {
            throw wrap("Erro ao criar material", e);
        }
    }

    // === Atualização total (nome e quantidade) ===
    public void atualizar(Material m) throws ServiceException {
        if (m == null) throw new ServiceException("Material não pode ser nulo.");
        if (m.getID() == null) throw new ServiceException("ID do material é obrigatório.");
        if (m.getNOME() == null || m.getNOME().isBlank())
            throw new ServiceException("Nome do material é obrigatório.");
        if (m.getQUANTIDADE() == null || m.getQUANTIDADE() < 0)
            throw new ServiceException("Quantidade não pode ser negativa.");

        try {
            dao.atualizarMaterial(m);
        } catch (SQLException e) {
            throw wrap("Erro ao atualizar material (ID=" + m.getID() + ")", e);
        }
    }

    // === Exclusão ===
    public void excluir(int materialId) throws ServiceException {
        if (materialId <= 0) throw new ServiceException("ID inválido para exclusão.");
        try {
            dao.excluirMaterial(materialId);
        } catch (SQLException e) {
            throw wrap("Erro ao excluir material (ID=" + materialId + ")", e);
        }
    }

    // === Regras de estoque ===

    /** Ajusta a quantidade somando delta (pode ser negativo). Retorna a nova quantidade. */
    public int ajustarQuantidade(int materialId, int delta) throws ServiceException {
        if (materialId <= 0) throw new ServiceException("ID inválido.");
        try {
            // Carrega atual
            List<Material> todos = dao.listar(); // simples; ideal ter um dao.buscarPorId(...)
            Material atual = todos.stream()
                    .filter(m -> m.getID() != null && m.getID() == materialId)
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("Material não encontrado (ID=" + materialId + ")."));

            int nova = (atual.getQUANTIDADE() == null ? 0 : atual.getQUANTIDADE()) + delta;
            if (nova < 0) throw new ServiceException("Resultado deixaria o estoque negativo.");

            atual.setQUANTIDADE(nova);
            dao.atualizarMaterial(atual);
            return nova;

        } catch (SQLException e) {
            throw wrap("Erro ao ajustar quantidade (ID=" + materialId + ", delta=" + delta + ")", e);
        }
    }

    /** Define a quantidade explicitamente (não pode ser negativa). */
    public void definirQuantidade(int materialId, int novaQuantidade) throws ServiceException {
        if (materialId <= 0) throw new ServiceException("ID inválido.");
        if (novaQuantidade < 0) throw new ServiceException("Quantidade não pode ser negativa.");

        try {
            // idem: ideal ter dao.buscarPorId(...)
            List<Material> todos = dao.listar();
            Material atual = todos.stream()
                    .filter(m -> m.getID() != null && m.getID() == materialId)
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("Material não encontrado (ID=" + materialId + ")."));

            atual.setQUANTIDADE(novaQuantidade);
            dao.atualizarMaterial(atual);

        } catch (SQLException e) {
            throw wrap("Erro ao definir quantidade (ID=" + materialId + ")", e);
        }
    }

    // === Util ===
    private ServiceException wrap(String msg, SQLException cause) {
        return new ServiceException(msg + ": " + cause.getMessage(), cause);
    }

    // Exceção de domínio do service
    public static class ServiceException extends Exception {
        public ServiceException(String message) { super(message); }
        public ServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
