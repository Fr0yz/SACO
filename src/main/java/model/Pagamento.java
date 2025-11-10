package model;

import java.util.Date;

public class Pagamento {
    private Long id_pagamento;
    private Long id_financeiro;
    private java.math.BigDecimal valor;
    private Date dt_pagamento;
    private String num_fatura;
    private String num_boleto;
    private StatusPagamento status;

    public Long getId_pagamento() { return id_pagamento; }
    public void setId_pagamento(Long id) { this.id_pagamento = id; }

    public Long getId_financeiro() { return id_financeiro; }
    public void setId_financeiro(Long id_financeiro) { this.id_financeiro = id_financeiro; }

    public java.math.BigDecimal getValor() { return valor; }
    public void setValor(java.math.BigDecimal valor) { this.valor = valor; }

    public Date getDt_pagamento() { return dt_pagamento; }
    public void setDt_pagamento(Date dt_pagamento) { this.dt_pagamento = dt_pagamento; }

    public String getNum_fatura() { return num_fatura; }
    public void setNum_fatura(String num_fatura) { this.num_fatura = num_fatura; }

    public String getNum_boleto() { return num_boleto; }
    public void setNum_boleto(String num_boleto) { this.num_boleto = num_boleto; }

    public StatusPagamento getStatus() { return status; }
    public void setStatus(StatusPagamento status) { this.status = status; }
}
