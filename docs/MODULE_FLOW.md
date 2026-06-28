# Fluxo Completo — Comunicação entre Módulos

Diagrama de sequência alto nível do fluxo principal: cadastro → cobrança → pagamento → baixa.

**Notação:**
- Linha **sólida** (`->>`) = chamada **síncrona**
- Linha **tracejada** (`-->>`) = comunicação **assíncrona** (evento ou processamento em background)

---

```mermaid
sequenceDiagram
    actor Admin
    actor Debtor as Devedor

    participant DB as caiman-debtor
    participant BI as caiman-billing
    participant PA as caiman-payment
    participant NO as caiman-notification
    participant AI as Anthropic API

    rect rgb(235, 245, 255)
        Note over Admin,DB: Fase 1 — Cadastro do Devedor
        Admin->>DB: POST /debtors
        DB-->>Admin: 201 Devedor criado
    end

    rect rgb(235, 245, 255)
        Note over Admin,BI: Fase 2 — Cadastro do ChargePlan
        Admin->>BI: POST /charge-plans
        BI-->>Admin: 201 ChargePlan criado
    end

    rect rgb(235, 245, 255)
        Note over Admin,BI: Fase 3 — Associação do Devedor ao ChargePlan
        Admin->>BI: POST /charge-plans/{id}/members
        BI->>DB: Busca dados do Devedor (gateway síncrono)
        DB-->>BI: Dados do Devedor
        BI-->>Admin: 201 Membro adicionado
    end

    rect rgb(255, 248, 235)
        Note over BI,NO: Fase 4 — Geração de Invoice (Odin · diário 00:00 UTC)
        BI->>BI: Odin calcula ciclo e gera Invoice
        BI-->>NO: InvoiceGeneratedEvent (assíncrono)
        NO->>NO: Cria entrada INVOICE_CREATED no outbox
    end

    rect rgb(255, 248, 235)
        Note over NO,Debtor: Fase 5 — Envio da Cobrança (Huginn · a cada minuto)
        NO->>NO: Huginn lê outbox (status SCHEDULED)
        NO->>Debtor: Email com link de upload (SMTP)
        NO->>BI: Atualiza Invoice: PENDING → SENT (gateway síncrono)
    end

    rect rgb(235, 255, 245)
        Note over Debtor,AI: Fase 6 — Upload e Análise do Comprovante
        Debtor->>PA: POST /public/invoices/{id}/proof (token JWT)
        PA->>BI: Busca dados da Invoice (gateway síncrono)
        BI-->>PA: Dados da Invoice
        PA-->>Debtor: 202 Comprovante recebido
        PA-->>AI: Análise do comprovante (assíncrono)
        AI-->>PA: Resultado da análise (assíncrono)
    end

    rect rgb(255, 235, 245)
        Note over PA,Debtor: Fase 7 — Baixa do Pagamento
        PA-->>BI: PaymentProofApprovedEvent (assíncrono)
        BI->>BI: Registra Payment, Invoice → PAID/PARTIALLY_PAID
        PA-->>NO: PaymentProofApprovedEvent (assíncrono)
        NO->>NO: Cria PAYMENT_APPROVED no outbox
        NO->>Debtor: Email de confirmação de pagamento (Huginn · SMTP)
    end
```

---

## Notas

- **Fase 3** — `caiman-billing` nunca acessa diretamente o banco de `caiman-debtor`. A busca ocorre via gateway (`DebtorGateway`) definido em `caiman-contracts` e implementado em `caiman-debtor:infrastructure`.
- **Fase 6** — `caiman-payment` nunca acessa diretamente o banco de `caiman-billing`. A busca da Invoice ocorre via gateway (`InvoiceGateway`) definido em `caiman-contracts`.
- **Fase 7** — `PaymentProofApprovedEvent` é consumido de forma independente por `caiman-billing` (baixa financeira) e `caiman-notification` (notificação ao devedor). Ambos reagem ao mesmo evento publicado por `caiman-payment`.
- **Odin** (scheduler diário em `caiman-billing`) também detecta invoices vencidas e agenda lembretes de cobrança — esses ciclos de reminder seguem o mesmo caminho da Fase 5, mas com `trigger_type` `OVERDUE_REMINDER` ou `PENDING_REMINDER`.
- **Modo de validação manual** (`MANUAL` / `AI_ASSISTED`) adiciona uma etapa entre as Fases 6 e 7: o comprovante vai para a fila de revisão do admin antes da baixa.
```
