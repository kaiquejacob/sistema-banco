import { apiFetch, definirCredenciais, limparCredenciais, estaAutenticado, obterUsuarioLogado } from "./api.js";
import { mostrarToast, formatarMoeda, formatarData, formatarCpf, formatarCep } from "./ui.js";

// Estado da Aplicação
let dadosContaAtual = null;
let perfilAdmin = false;

document.addEventListener("DOMContentLoaded", () => {
    configurarNavegacaoAuth();
    configurarNavegacaoSidebar();
    configurarAtalhosDashboard();
    configurarFormularios();
    configurarConsultaCep();
});

/* ==========================================================================
   1. CONTROLE DE TELAS & AUTENTICAÇÃO
   ========================================================================== */

function configurarNavegacaoAuth() {
    const tabLogin = document.getElementById("tab-login");
    const tabCadastro = document.getElementById("tab-cadastro");
    const formLogin = document.getElementById("form-login");
    const formCadastro = document.getElementById("form-cadastro");
    const subtitulo = document.getElementById("auth-subtitulo");

    tabLogin.addEventListener("click", () => {
        tabLogin.classList.add("ativo");
        tabCadastro.classList.remove("ativo");
        formLogin.style.display = "block";
        formCadastro.style.display = "none";
        subtitulo.textContent = "Acesso seguro ao Internet Banking";
    });

    tabCadastro.addEventListener("click", () => {
        tabCadastro.classList.add("ativo");
        tabLogin.classList.remove("ativo");
        formCadastro.style.display = "block";
        formLogin.style.display = "none";
        subtitulo.textContent = "Preencha a proposta de abertura de conta";
    });

    document.getElementById("btn-logout").addEventListener("click", () => {
        limparCredenciais();
        dadosContaAtual = null;
        perfilAdmin = false;
        document.getElementById("tela-app").style.display = "none";
        document.getElementById("tela-auth").style.display = "flex";
        document.getElementById("form-login").reset();
        mostrarToast("Sessão finalizada com segurança.", "info");
    });
}

function alternarParaApp(ehAdmin) {
    perfilAdmin = ehAdmin;
    document.getElementById("tela-auth").style.display = "none";
    document.getElementById("tela-app").style.display = "flex";

    const adminGroup = document.getElementById("admin-nav-group");
    const userBadge = document.getElementById("user-badge");

    if (ehAdmin) {
        adminGroup.style.display = "block";
        userBadge.textContent = "ADMINISTRADOR";
        userBadge.classList.add("admin");
        document.getElementById("user-nome").textContent = "Administrador";
        document.getElementById("user-detalhe").textContent = "Módulo de Gestão";

        ativarSecao("secao-admin-contas", "Gestão de Contas");
        carregarContasAdmin();
    } else {
        adminGroup.style.display = "none";
        userBadge.textContent = "CLIENTE";
        userBadge.classList.remove("admin");

        ativarSecao("secao-dashboard", "Visão Geral");
        carregarDadosContaCliente();
    }
}

/* ==========================================================================
   2. NAVEGAÇÃO DA SIDEBAR & ATALHOS RÁPIDOS
   ========================================================================== */

function configurarNavegacaoSidebar() {
    const navItems = document.querySelectorAll(".sidebar-nav .nav-item");

    navItems.forEach(item => {
        item.addEventListener("click", () => {
            const secaoId = item.getAttribute("data-secao");
            const titulo = item.querySelector(".nav-icon + span")?.textContent || item.textContent.trim();
            ativarSecao(secaoId, titulo);
        });
    });

    document.getElementById("btn-ir-extrato")?.addEventListener("click", () => {
        ativarSecao("secao-extrato", "Extrato de Conta");
    });

    document.getElementById("btn-atualizar-extrato")?.addEventListener("click", () => {
        carregarExtratoCompleto();
    });

    document.getElementById("btn-atualizar-admin")?.addEventListener("click", () => {
        carregarContasAdmin();
    });
}

function configurarAtalhosDashboard() {
    document.getElementById("atalho-pix")?.addEventListener("click", () => {
        ativarSecao("secao-transferir", "Transferência Pix");
    });
    document.getElementById("atalho-saque")?.addEventListener("click", () => {
        ativarSecao("secao-saque", "Saque em Conta");
    });
    document.getElementById("atalho-deposito")?.addEventListener("click", () => {
        ativarSecao("secao-deposito", "Depósito em Conta");
    });
}

function ativarSecao(secaoId, titulo) {
    document.querySelectorAll(".sidebar-nav .nav-item").forEach(btn => {
        if (btn.getAttribute("data-secao") === secaoId) {
            btn.classList.add("ativo");
        } else {
            btn.classList.remove("ativo");
        }
    });

    document.querySelectorAll(".secao-conteudo").forEach(secao => {
        secao.classList.remove("ativo");
    });

    const alvo = document.getElementById(secaoId);
    if (alvo) {
        alvo.classList.add("ativo");
    }

    if (titulo) {
        document.getElementById("topbar-titulo").textContent = titulo;
    }

    if (secaoId === "secao-extrato" && !perfilAdmin) {
        carregarExtratoCompleto();
    } else if (secaoId === "secao-admin-contas" && perfilAdmin) {
        carregarContasAdmin();
    }
}

/* ==========================================================================
   3. CONSULTA AUTOMÁTICA DE CEP (VIACEP)
   ========================================================================== */

function configurarConsultaCep() {
    const inputCep = document.getElementById("cad-cep");
    const preview = document.getElementById("preview-cep");

    inputCep.addEventListener("blur", async () => {
        const cepLimpo = inputCep.value.replace(/\D/g, "");
        if (cepLimpo.length !== 8) {
            preview.textContent = "";
            return;
        }

        preview.textContent = "Consultando base de endereços...";
        try {
            const resp = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`);
            const dados = await resp.json();
            if (dados.erro) {
                preview.textContent = "CEP não localizado.";
                preview.style.color = "var(--danger)";
            } else {
                preview.textContent = `${dados.logradouro || "Logradouro"}, ${dados.bairro || ""} - ${dados.localidade}/${dados.uf}`;
                preview.style.color = "var(--text-secondary)";
            }
        } catch {
            preview.textContent = "";
        }
    });
}

/* ==========================================================================
   4. FORMULÁRIOS & REQUISIÇÕES
   ========================================================================== */

function configurarFormularios() {
    // 4.1 Login
    document.getElementById("form-login").addEventListener("submit", async (e) => {
        e.preventDefault();
        const usuario = document.getElementById("login-usuario").value.trim();
        const senha = document.getElementById("login-senha").value;

        try {
            definirCredenciais(usuario, senha);

            if (usuario.toLowerCase() === "admin") {
                await apiFetch("/contas?pagina=0&tamanho=1");
                mostrarToast("Autenticação de administrador realizada com sucesso.", "sucesso", "Acesso Autorizado");
                alternarParaApp(true);
            } else {
                const conta = await apiFetch("/contas/me");
                dadosContaAtual = conta;
                mostrarToast(`Bem-vindo, ${conta.titular}.`, "sucesso", "Acesso Concedido");
                alternarParaApp(false);
            }
        } catch (erro) {
            mostrarToast(erro.message, "erro", "Falha de Autenticação");
        }
    });

    // 4.2 Cadastro de Conta
    document.getElementById("form-cadastro").addEventListener("submit", async (e) => {
        e.preventDefault();

        const titular = document.getElementById("cad-titular").value.trim();
        const cpf = document.getElementById("cad-cpf").value.replace(/\D/g, "");
        const email = document.getElementById("cad-email").value.trim();
        const cep = document.getElementById("cad-cep").value.replace(/\D/g, "");
        const tipo = document.getElementById("cad-tipo").value;
        const saldoInicial = parseFloat(document.getElementById("cad-saldo").value);
        const senha = document.getElementById("cad-senha").value;

        try {
            const payload = {
                titular,
                cpf,
                email,
                cep,
                tipo,
                saldoInicial,
                senha
            };

            await apiFetch("/contas", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            mostrarToast("Conta criada com sucesso. Efetue o acesso.", "sucesso", "Abertura Concluída");
            document.getElementById("form-cadastro").reset();
            document.getElementById("preview-cep").textContent = "";

            document.getElementById("tab-login").click();
            document.getElementById("login-usuario").value = cpf;
            document.getElementById("login-senha").focus();
        } catch (erro) {
            mostrarToast(erro.message, "erro", "Não foi possível abrir a conta");
        }
    });

    // 4.3 Transferência
    document.getElementById("form-transferir").addEventListener("submit", async (e) => {
        e.preventDefault();
        const cpfDestino = document.getElementById("transf-cpf-destino").value.replace(/\D/g, "");
        const valor = parseFloat(document.getElementById("transf-valor").value);

        try {
            const contaAtualizada = await apiFetch("/transferencias", {
                method: "POST",
                body: JSON.stringify({ cpfDestino, valor })
            });

            mostrarToast(`Transferência de ${formatarMoeda(valor)} efetuada.`, "sucesso", "Ordem Processada");
            document.getElementById("form-transferir").reset();
            dadosContaAtual = { ...dadosContaAtual, ...contaAtualizada };
            atualizarCardsDashboard(dadosContaAtual);
            ativarSecao("secao-dashboard", "Visão Geral");
        } catch (erro) {
            mostrarToast(erro.message, "erro", "Erro na Transferência");
        }
    });

    // 4.4 Depósito
    document.getElementById("form-deposito").addEventListener("submit", async (e) => {
        e.preventDefault();
        const valor = parseFloat(document.getElementById("deposito-valor").value);

        try {
            const contaAtualizada = await apiFetch("/contas/me/depositos", {
                method: "POST",
                body: JSON.stringify({ valor })
            });

            mostrarToast(`Depósito de ${formatarMoeda(valor)} creditado em conta.`, "sucesso", "Crédito Efetuado");
            document.getElementById("form-deposito").reset();
            dadosContaAtual = { ...dadosContaAtual, ...contaAtualizada };
            atualizarCardsDashboard(dadosContaAtual);
            ativarSecao("secao-dashboard", "Visão Geral");
        } catch (erro) {
            mostrarToast(erro.message, "erro", "Falha no Depósito");
        }
    });

    // 4.5 Saque
    document.getElementById("form-saque").addEventListener("submit", async (e) => {
        e.preventDefault();
        const valor = parseFloat(document.getElementById("saque-valor").value);

        try {
            const contaAtualizada = await apiFetch("/contas/me/saques", {
                method: "POST",
                body: JSON.stringify({ valor })
            });

            mostrarToast(`Saque de ${formatarMoeda(valor)} liquidado.`, "sucesso", "Saque Concluído");
            document.getElementById("form-saque").reset();
            dadosContaAtual = { ...dadosContaAtual, ...contaAtualizada };
            atualizarCardsDashboard(dadosContaAtual);
            ativarSecao("secao-dashboard", "Visão Geral");
        } catch (erro) {
            mostrarToast(erro.message, "erro", "Falha no Saque");
        }
    });
}

/* ==========================================================================
   5. CARREGAMENTO DE DADOS: CLIENTE
   ========================================================================== */

async function carregarDadosContaCliente() {
    try {
        const conta = await apiFetch("/contas/me");
        dadosContaAtual = conta;
        atualizarCardsDashboard(conta);

        const extrato = await apiFetch("/contas/me/extrato");
        if (extrato.endereco) {
            dadosContaAtual.endereco = extrato.endereco;
            atualizarEnderecoDashboard(extrato.endereco);
        }
        renderizarTabelaUltimasMovimentacoes(extrato.transacoes || []);
    } catch (erro) {
        mostrarToast(erro.message, "erro", "Erro ao carregar dados da conta");
    }
}

function nomeLegivelTipoConta(tipo) {
    if (tipo === "INVESTIMENTO") return "Conta Investimento";
    if (tipo === "POUPANCA") return "Conta Poupança";
    return "Conta Corrente";
}

function atualizarCardsDashboard(conta) {
    document.getElementById("user-nome").textContent = conta.titular;
    document.getElementById("user-detalhe").textContent = `CPF: ${formatarCpf(conta.cpf)}`;
    const dashboardNome = document.getElementById("dashboard-nome");
    if (dashboardNome) {
        dashboardNome.textContent = conta.titular.split(" ")[0];
    }

    document.getElementById("card-saldo").textContent = formatarMoeda(conta.saldo);
    document.getElementById("card-tipo-conta").textContent = nomeLegivelTipoConta(conta.tipo);
    document.getElementById("card-cpf-titular").textContent = formatarCpf(conta.cpf);

    if (conta.endereco) {
        atualizarEnderecoDashboard(conta.endereco);
    }
}

function atualizarEnderecoDashboard(endereco) {
    const elRua = document.getElementById("card-endereco-rua");
    const elCidade = document.getElementById("card-endereco-cidade");

    if (endereco && (endereco.logradouro || endereco.cidade || endereco.cep)) {
        elRua.textContent = `${endereco.logradouro || "Logradouro"}, ${endereco.bairro || ""}`;
        elCidade.textContent = `${endereco.cidade || ""} / ${endereco.estado || ""} • CEP ${formatarCep(endereco.cep)}`;
    } else {
        elRua.textContent = "Endereço não cadastrado";
        elCidade.textContent = "-";
    }
}

function renderizarTabelaUltimasMovimentacoes(transacoes) {
    const tbody = document.getElementById("tabela-ultimas-movimentacoes");
    if (!tbody) return;

    if (!transacoes || transacoes.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 20px;">Nenhum lançamento recente registrado.</td>
            </tr>
        `;
        return;
    }

    // Apenas os últimos 3 lançamentos na home para não roubar o papel do extrato
    const ultimas = [...transacoes].reverse().slice(0, 3);

    tbody.innerHTML = ultimas.map(t => {
        const ehPositivo = t.tipo === "DEPOSITO" || (t.tipo === "TRANSFERENCIA" && t.valor > 0);

        return `
            <tr>
                <td>${formatarData(t.dataHora || t.momento)}</td>
                <td><span class="tag-tipo">${t.tipo}</span></td>
                <td>${t.descricao || t.detalhes || "Lançamento em conta"}</td>
                <td style="text-align: right;" class="${ehPositivo ? 'valor-positivo' : 'valor-negativo'}">
                    ${ehPositivo ? "+" : "-"} ${formatarMoeda(Math.abs(t.valor))}
                </td>
            </tr>
        `;
    }).join("");
}

async function carregarExtratoCompleto() {
    const tbody = document.getElementById("tabela-extrato-completo");
    tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 32px;">Carregando extrato...</td></tr>`;

    try {
        const extrato = await apiFetch("/contas/me/extrato");
        document.getElementById("extrato-saldo-atual").textContent = formatarMoeda(extrato.saldoAtual ?? dadosContaAtual?.saldo);

        if (extrato.endereco && (!dadosContaAtual || !dadosContaAtual.endereco)) {
            if (dadosContaAtual) dadosContaAtual.endereco = extrato.endereco;
            atualizarEnderecoDashboard(extrato.endereco);
        }

        const transacoes = extrato.transacoes || [];
        if (transacoes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 32px;">Nenhum lançamento encontrado para esta conta.</td></tr>`;
            return;
        }

        tbody.innerHTML = [...transacoes].reverse().map(t => {
            const ehPositivo = t.tipo === "DEPOSITO" || (t.tipo === "TRANSFERENCIA" && t.valor > 0);

            return `
                <tr>
                    <td>${formatarData(t.dataHora || t.momento)}</td>
                    <td><span class="tag-tipo">${t.tipo}</span></td>
                    <td>${t.descricao || t.detalhes || "Lançamento em conta"}</td>
                    <td style="text-align: right;" class="${ehPositivo ? 'valor-positivo' : 'valor-negativo'}">
                        ${ehPositivo ? "+" : "-"} ${formatarMoeda(Math.abs(t.valor))}
                    </td>
                </tr>
            `;
        }).join("");
    } catch (erro) {
        mostrarToast(erro.message, "erro", "Erro ao consultar extrato");
    }
}

/* ==========================================================================
   6. CARREGAMENTO DE DADOS: ADMINISTRADOR
   ========================================================================== */

async function carregarContasAdmin() {
    const tbody = document.getElementById("tabela-admin-contas");
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 32px;">Consultando carteira...</td></tr>`;

    try {
        const pagina = await apiFetch("/contas?pagina=0&tamanho=50");
        const contas = pagina.itens || pagina.content || [];

        if (contas.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 32px;">Nenhuma conta localizada na base.</td></tr>`;
            return;
        }

        tbody.innerHTML = contas.map(c => `
            <tr>
                <td><strong>${c.titular}</strong><br><small style="color: var(--text-muted)">${c.email || ""}</small></td>
                <td>${formatarCpf(c.cpf)}</td>
                <td><span class="tag-tipo">${c.tipo}</span></td>
                <td>${c.endereco ? `${c.endereco.cidade || c.endereco.localidade || ""}/${c.endereco.estado || c.endereco.uf || ""}` : "-"}</td>
                <td style="text-align: right; font-weight: 700; color: #FFFFFF;">${formatarMoeda(c.saldo)}</td>
                <td style="text-align: center;">
                    <div style="display: flex; gap: 6px; justify-content: center;">
                        <button class="btn btn-secondary btn-sm btn-taxa" data-cpf="${c.cpf}">Tarifar</button>
                        <button class="btn btn-danger btn-sm btn-remover" data-cpf="${c.cpf}">Encerrar</button>
                    </div>
                </td>
            </tr>
        `).join("");

        tbody.querySelectorAll(".btn-taxa").forEach(btn => {
            btn.addEventListener("click", async () => {
                const cpf = btn.getAttribute("data-cpf");
                try {
                    await apiFetch(`/contas/admin/${cpf}/taxa-manutencao`, { method: "POST" });
                    mostrarToast("Tarifa de manutenção debitada com sucesso.", "sucesso");
                    carregarContasAdmin();
                } catch (erro) {
                    mostrarToast(erro.message, "erro", "Falha ao tarifar conta");
                }
            });
        });

        tbody.querySelectorAll(".btn-remover").forEach(btn => {
            btn.addEventListener("click", async () => {
                const cpf = btn.getAttribute("data-cpf");
                if (!confirm(`Confirma o encerramento da conta do CPF ${formatarCpf(cpf)}?`)) return;

                try {
                    await apiFetch(`/contas/admin/${cpf}`, { method: "DELETE" });
                    mostrarToast("Conta encerrada com sucesso.", "sucesso");
                    carregarContasAdmin();
                } catch (erro) {
                    mostrarToast(erro.message, "erro", "Não foi possível encerrar a conta");
                }
            });
        });

    } catch (erro) {
        mostrarToast(erro.message, "erro", "Erro no Módulo de Gestão");
    }
}
