// Módulo de UI e Notificações (Toasts)

export function mostrarToast(mensagem, tipo = "info", titulo = "") {
    const container = document.getElementById("toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast toast-${tipo}`;

    const icones = {
        sucesso: "✓",
        erro: "✕",
        info: "ℹ",
        alerta: "⚠"
    };

    const icone = icones[tipo] || "ℹ";

    toast.innerHTML = `
        <div class="toast-icone">${icone}</div>
        <div class="toast-conteudo">
            ${titulo ? `<strong class="toast-titulo">${titulo}</strong>` : ""}
            <p class="toast-mensagem">${mensagem}</p>
        </div>
        <button class="toast-fechar" aria-label="Fechar">&times;</button>
    `;

    const botaoFechar = toast.querySelector(".toast-fechar");
    botaoFechar.addEventListener("click", () => removerToast(toast));

    container.appendChild(toast);

    // Auto-remover após 5 segundos
    setTimeout(() => {
        removerToast(toast);
    }, 5000);
}

function removerToast(toast) {
    if (!toast || toast.classList.contains("toast-saindo")) return;
    toast.classList.add("toast-saindo");
    setTimeout(() => {
        if (toast.parentElement) {
            toast.parentElement.removeChild(toast);
        }
    }, 300);
}

export function formatarMoeda(valor) {
    if (valor === null || valor === undefined) return "R$ 0,00";
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    }).format(valor);
}

export function formatarData(dataIso) {
    if (!dataIso) return "-";
    try {
        const data = new Date(dataIso);
        return new Intl.DateTimeFormat("pt-BR", {
            dateStyle: "short",
            timeStyle: "short"
        }).format(data);
    } catch {
        return dataIso;
    }
}

export function formatarCpf(cpf) {
    if (!cpf) return "";
    const numeros = cpf.replace(/\D/g, "");
    if (numeros.length !== 11) return cpf;
    return numeros.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
}

export function formatarCep(cep) {
    if (!cep) return "";
    const numeros = cep.replace(/\D/g, "");
    if (numeros.length !== 8) return cep;
    return numeros.replace(/(\d{5})(\d{3})/, "$1-$2");
}
