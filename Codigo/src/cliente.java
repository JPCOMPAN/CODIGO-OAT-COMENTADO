// Importações necessárias para o código funcionar
import java.io.*;
import java.net.*;
import java.util.*;

// Classe principal do cliente
public class cliente {
    // Endereço IP do servidor (neste caso, o próprio computador)
    private static final String SERVIDOR_IP = "localhost";

    // Porta que o servidor está usando
    private static final int PORTA = 12345;

    // Método principal do cliente
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVIDOR_IP, PORTA);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            // Pede ao usuário para digitar nome de usuário e senha
            System.out.print("Digite o nome de usuário: ");
            String usuario = scanner.nextLine();
            System.out.print("Digite a senha: ");
            String senha = scanner.nextLine();

            // Envia o nome de usuário e senha para o servidor
            output.writeObject(usuario);
            output.writeObject(senha);
            output.flush();

            // Recebe a resposta do servidor (sucesso ou falha no login)
            String resposta = (String) input.readObject();
            if (resposta.equals("LOGIN_SUCESSO")) {
                System.out.println("Login bem-sucedido!");

                // Loop para permitir que o usuário escolha comandos
                while (true) {
                    System.out.println("Escolha uma opção: ENVIAR, BAIXAR, LISTAR, SAIR");
                    String comando = scanner.nextLine();
                    output.writeObject(comando);
                    output.flush();

                    // Se o comando for "ENVIAR"
                    if (comando.equalsIgnoreCase("ENVIAR")) {
                        // Pede o caminho do arquivo que será enviado
                        System.out.print("Digite o caminho do arquivo para enviar: ");
                        String caminhoArquivo = scanner.nextLine();
                        File arquivo = new File(caminhoArquivo);

                        // Verifica se o arquivo existe
                        if (arquivo.exists() && arquivo.isFile()) {
                            // Envia o nome e o tamanho do arquivo para o servidor
                            output.writeObject(arquivo.getName());
                            output.writeObject(arquivo.length());
                            output.flush();

                            // Envia o conteúdo do arquivo em pedaços (buffers)
                            try (FileInputStream fileInput = new FileInputStream(arquivo)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fileInput.read(buffer)) > 0) {
                                    output.write(buffer, 0, bytesRead);
                                }
                                output.flush();
                            }
                            System.out.println("Arquivo enviado com sucesso.");
                        } else {
                            System.out.println("Arquivo não encontrado ou caminho inválido.");
                        }
                    }

                    // Se o comando for "BAIXAR"
                    else if (comando.equalsIgnoreCase("BAIXAR")) {
                        // Pede o nome do arquivo que será baixado
                        System.out.print("Digite o nome do arquivo para baixar: ");
                        String nomeArquivo = scanner.nextLine();
                        output.writeObject(nomeArquivo);
                        output.flush();

                        // Recebe a resposta do servidor (arquivo encontrado ou não)
                        String respostaArquivo = (String) input.readObject();
                        if (respostaArquivo.equals("ARQUIVO_ENCONTRADO")) {
                            // Recebe o tamanho do arquivo
                            long tamanhoArquivo = (long) input.readObject();
                            System.out.println("Tamanho do arquivo: " + tamanhoArquivo + " bytes");

                            // Pede o caminho onde o arquivo será salvo
                            System.out.print("Digite o caminho completo para salvar o arquivo (incluindo o nome do arquivo): ");
                            String caminhoSalvar = scanner.nextLine();

                            File arquivoSalvar = new File(caminhoSalvar);

                            // Verifica se o caminho é um diretório
                            if (arquivoSalvar.isDirectory()) {
                                System.out.println("Erro: O caminho especificado é um diretório. Por favor, inclua o nome do arquivo.");
                                continue;
                            }

                            // Se o arquivo já existir, pergunta se quer sobrescrever ou escolher um novo nome
                            if (arquivoSalvar.exists()) {
                                System.out.println("Arquivo já existe no caminho: " + arquivoSalvar.getAbsolutePath());
                                System.out.println("Tamanho do arquivo existente: " + arquivoSalvar.length() + " bytes");

                                System.out.println("Escolha uma opção:");
                                System.out.println("1. Sobrescrever o arquivo");
                                System.out.println("2. Escolher um novo nome");
                                System.out.print("Opção: ");
                                String opcao = scanner.nextLine();

                                if (opcao.equals("1")) {
                                    // Sobrescreve o arquivo existente
                                    try (FileOutputStream fileOutput = new FileOutputStream(arquivoSalvar)) {
                                        byte[] buffer = new byte[4096];
                                        int bytesRead;
                                        long totalBytesRead = 0;
                                        while (totalBytesRead < tamanhoArquivo) {
                                            bytesRead = input.read(buffer);
                                            fileOutput.write(buffer, 0, bytesRead);
                                            totalBytesRead += bytesRead;
                                        }
                                    }
                                    System.out.println("Arquivo sobrescrito com sucesso.");
                                } else if (opcao.equals("2")) {
                                    // Escolhe um novo nome para o arquivo
                                    System.out.print("Digite o novo nome do arquivo: ");
                                    String novoNome = scanner.nextLine();
                                    File novoArquivo = new File(arquivoSalvar.getParent(), novoNome);
                                    try (FileOutputStream fileOutput = new FileOutputStream(novoArquivo)) {
                                        byte[] buffer = new byte[4096];
                                        int bytesRead;
                                        long totalBytesRead = 0;
                                        while (totalBytesRead < tamanhoArquivo) {
                                            bytesRead = input.read(buffer);
                                            fileOutput.write(buffer, 0, bytesRead);
                                            totalBytesRead += bytesRead;
                                        }
                                    }
                                    System.out.println("Arquivo salvo com o novo nome: " + novoNome);
                                } else {
                                    System.out.println("Opção inválida. Download cancelado.");
                                }
                            } else {
                                // Se o arquivo não existir, salva normalmente
                                try (FileOutputStream fileOutput = new FileOutputStream(arquivoSalvar)) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    long totalBytesRead = 0;
                                    while (totalBytesRead < tamanhoArquivo) {
                                        bytesRead = input.read(buffer);
                                        fileOutput.write(buffer, 0, bytesRead);
                                        totalBytesRead += bytesRead;
                                    }
                                }
                                System.out.println("Arquivo baixado com sucesso.");
                            }
                        } else {
                            System.out.println("Arquivo não encontrado no servidor.");
                        }
                    }

                    // Se o comando for "LISTAR"
                    else if (comando.equalsIgnoreCase("LISTAR")) {
                        // Solicita a lista de diretórios ao servidor
                        output.writeObject("LISTAR");
                        output.flush();

                        // Recebe a estrutura de diretórios e exibe
                        String respostaListar = (String) input.readObject();
                        if (respostaListar.equals("ESTRUTURA_DIRETORIOS")) {
                            String estrutura = (String) input.readObject();
                            System.out.println("Estrutura de diretórios e arquivos:");
                            System.out.println(estrutura);
                        }
                    }

                    // Se o comando for "SAIR"
                    else if (comando.equalsIgnoreCase("SAIR")) {
                        // Sai do loop e encerra o cliente
                        System.out.println("Encerrando conexão...");
                        break;
                    }

                    // Se o comando for inválido
                    else {
                        System.out.println("Comando inválido. Tente novamente.");
                    }
                }
            } else {
                // Se o login falhar, exibe uma mensagem de erro
                System.out.println("Login falhou. Usuário ou senha incorretos.");
            }
        } catch (IOException | ClassNotFoundException e) {
            // Se ocorrer um erro, exibe a mensagem de erro
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}