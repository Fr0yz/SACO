package model;

import java.util.Date;

public class Financeiro {
    private Long id_financeiro;
    private Long id_agendamento;
    private java.math.BigDecimal valor_total;
    private Date dt_emissao;
    private StatusFinanceiro status;
    private MetodoPagamento metodo_pagamento;

    public Long getId_financeiro() { return id_financeiro; }
    public void setId_financeiro(Long id) { this.id_financeiro = id; }

    public Long getId_agendamento() { return id_agendamento; }
    public void setId_agendamento(Long id_agendamento) { this.id_agendamento = id_agendamento; }

    public java.math.BigDecimal getValor_total() { return valor_total; }
    public void setValor_total(java.math.BigDecimal valor_total) { this.valor_total = valor_total; }

    public Date getDt_emissao() { return dt_emissao; }
    public void setDt_emissao(Date dt_emissao) { this.dt_emissao = dt_emissao; }

    public StatusFinanceiro getStatus() { return status; }
    public void setStatus(StatusFinanceiro status) { this.status = status; }

    public MetodoPagamento getMetodo_pagamento() { return metodo_pagamento; }
    public void setMetodo_pagamento(MetodoPagamento metodo_pagamento) { this.metodo_pagamento = metodo_pagamento; }
}
