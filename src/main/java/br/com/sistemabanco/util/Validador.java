package br.com.sistemabanco.util;

import java.util.regex.Pattern;

public class Validador {

    private static final Pattern PATTERN_NOME = Pattern.compile("^[\\p{L}\\s]+$");
    private static final Pattern PATTERN_EMAIL = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z]+\\.[a-zA-Z]+$");

    private Validador() {
    }

    public static boolean validarNome(String nome) {
        if (nome == null) {
            return false;
        }
        boolean temConteudo = !nome.trim().isEmpty();
        boolean bate = PATTERN_NOME.matcher(nome).matches();

        return temConteudo && bate;
    }

    public static boolean validarCpf(String cpf) {
        cpf = normalizarCpf(cpf);

        if (cpf == null || cpf.length() != 11) {
            return false;
        }

        if (!cpf.chars().allMatch(Character::isDigit)) {
            return false;
        }

        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int soma = 0;
        for (int i = 0; i <= 8; i++) {
            int num = Character.getNumericValue(cpf.charAt(i));
            soma += (num * (10 -i)) ;
        }
        int resto = soma % 11;
        int digitoVerificador;

        if (resto < 2){
            digitoVerificador = 0;
        }else{
             digitoVerificador = 11 - resto;
        }

        int soma2 = 0;
        for (int i = 0; i <= 9; i++) {
            int num = Character.getNumericValue(cpf.charAt(i));
            soma2 += (num *(11 - i));
        }
        int resto2 = soma2 % 11;
        int digitoVerificador2;

        if (resto2 < 2){
            digitoVerificador2 = 0;
        }else{
            digitoVerificador2 = 11 - resto2;
        }

        int digitoReal1 = Character.getNumericValue(cpf.charAt(9));
        int digitoReal2 = Character.getNumericValue(cpf.charAt(10));

        return (digitoVerificador == digitoReal1) && (digitoVerificador2 == digitoReal2);
    }

    public static String normalizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replace(".", "").replace("-", "").replace(" ", "");
    }

    public static boolean validarEmail(String email){
        if (email == null) {
            return false;
        }
        return PATTERN_EMAIL.matcher(email).matches();
    }

}
