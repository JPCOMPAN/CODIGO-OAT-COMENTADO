// Importações necessárias para o código funcionar
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

// Classe principal do servidor
public class servidor {
    // Porta que o servidor vai usar para se comunicar
    private static final int PORTA = 12345;

    // Caminho base onde os arquivos dos usuários serão armazenados
    private static final String CAMINHO_BASE = "D:\\Faculdade\\OAT_S\\OAT_DE_REDES\\OAT_REDES_DIRETORIO";

    // Lista de usuários e senhas válidos
    private static final String[] USUARIOS = {"usuario1", "usuario2", "usuario3"};
    private static final String[] SENHAS = {"senha1", "senha2", "senha3"};

    // Subpastas que serão criadas para cada usuário
    private static final String[] SUBPASTAS = {"pdf", "jpg", "txt"};

    // Método principal do servidor
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            // Mensagem indicando que o servidor foi iniciado
            System.out.println("Servidor iniciado na porta " + PORTA);

            // Loop infinito para aceitar conexões de clientes
            while (true) {
                // Aceita uma conexão de um cliente
                Socket socket = serverSocket.accept();

                // Cria uma nova thread para lidar com o cliente
                new Thread(new ClienteHandler(socket)).start();
            }
        } catch (IOException e) {
            // Se ocorrer um erro, exibe a mensagem de erro
            e.printStackTrace();
        }
    }

    // Classe interna para lidar com cada cliente
    private static class ClienteHandler implements Runnable {
        private Socket socket; // Conexão com o cliente

        // Construtor que recebe o socket do cliente
        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        // Método que é executado quando a thread é iniciada
        public void run() {
            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

                // Recebe o nome de usuário e senha do cliente
                String usuario = (String) input.readObject();
                String senha = (String) input.readObject();

                // Verifica se o usuário e senha estão corretos
                boolean loginConcluido = false;
                for (int i = 0; i < USUARIOS.length; i++) {
                    if (usuario.equals(USUARIOS[i]) && senha.equals(SENHAS[i])) {
                        loginConcluido = true;
                        break;
                    }
                }

                // Se o login for bem-sucedido
                if (loginConcluido) {
                    // Envia uma mensagem de sucesso para o cliente
                    output.writeObject("LOGIN_SUCESSO");

                    // Cria as pastas para o usuário
                    criarPastasUsuario(usuario);

                    // Gerencia os arquivos do usuário
                    gerenciarArquivos(usuario, input, output);
                } else {
                    // Se o login falhar, envia uma mensagem de falha
                    output.writeObject("LOGIN_FALHA");
                }
            } catch (IOException | ClassNotFoundException e) {
                // Se ocorrer um erro, exibe a mensagem de erro
                e.printStackTrace();
            }
        }

        // Método para criar pastas para o usuário
        private void criarPastasUsuario(String usuario) throws IOException {
            // Cria o caminho da pasta do usuário
            Path caminhoPasta = Paths.get(CAMINHO_BASE, usuario);

            // Se a pasta não existir, cria a pasta e as subpastas
            if (!Files.exists(caminhoPasta)) {
                Files.createDirectories(caminhoPasta);
                for (String subpasta : SUBPASTAS) {
                    Path pathSubpasta = Paths.get(caminhoPasta.toString(), subpasta);
                    Files.createDirectories(pathSubpasta);
                }
                System.out.println("Pastas criadas para o usuário: " + usuario);
            }
        }

        // Método para listar a estrutura de diretórios
        private String listarDiretorios(Path caminho, int nivel) throws IOException {
            StringBuilder sb = new StringBuilder();
            String indentacao = "    ".repeat(nivel); // Indentação para organizar a exibição

            // Se o caminho for um diretório, lista seus arquivos e subdiretórios
            if (Files.isDirectory(caminho)) {
                sb.append(indentacao).append("└── ").append(caminho.getFileName()).append("/\n");
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(caminho)) {
                    for (Path entry : stream) {
                        sb.append(listarDiretorios(entry, nivel + 1));
                    }
                }
            } else {
                // Se for um arquivo, apenas adiciona ao resultado
                sb.append(indentacao).append("└── ").append(caminho.getFileName()).append("\n");
            }

            return sb.toString();
        }

        // Método para gerenciar os arquivos do usuário
        private void gerenciarArquivos(String usuario, ObjectInputStream input, ObjectOutputStream output) throws IOException, ClassNotFoundException {
            while (true) {
                // Recebe o comando do cliente
                String comando = (String) input.readObject();

                // Se o comando for "ENVIAR"
                if (comando.equals("ENVIAR")) {
                    // Recebe o nome e o tamanho do arquivo
                    String nomeArquivo = (String) input.readObject();
                    long tamanhoArquivo = (long) input.readObject();

                    // Verifica a extensão do arquivo para decidir em qual subpasta salvar
                    String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf(".") + 1).toLowerCase();
                    String subpasta = null;
                    for (String sp : SUBPASTAS) {
                        if (sp.equalsIgnoreCase(extensao)) {
                            subpasta = sp;
                            break;
                        }
                    }

                    // Define o caminho onde o arquivo será salvo
                    Path caminhoArquivo;
                    if (subpasta != null) {
                        caminhoArquivo = Paths.get(CAMINHO_BASE, usuario, subpasta, nomeArquivo);
                    } else {
                        caminhoArquivo = Paths.get(CAMINHO_BASE, usuario, nomeArquivo);
                    }

                    // Recebe e salva o arquivo no servidor
                    try (FileOutputStream fileOutput = new FileOutputStream(caminhoArquivo.toFile())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        while (totalBytesRead < tamanhoArquivo) {
                            bytesRead = input.read(buffer);
                            fileOutput.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                    }
                    // Envia uma mensagem de confirmação para o cliente
                    output.writeObject("ARQUIVO_RECEBIDO");
                    System.out.println("Arquivo salvo: " + caminhoArquivo.toString());
                }

                // Se o comando for "BAIXAR"
                else if (comando.equals("BAIXAR")) {
                    // Recebe o nome do arquivo que o cliente deseja baixar
                    String nomeArquivo = (String) input.readObject();
                    Path caminhoArquivo = null;
                    boolean arquivoEncontrado = false;

                    // Procura o arquivo nas subpastas do usuário
                    for (String subpasta : SUBPASTAS) {
                        caminhoArquivo = Paths.get(CAMINHO_BASE, usuario, subpasta, nomeArquivo);
                        if (Files.exists(caminhoArquivo)) {
                            arquivoEncontrado = true;
                            break;
                        }
                    }

                    // Se o arquivo for encontrado
                    if (arquivoEncontrado) {
                        // Envia uma mensagem de confirmação e o tamanho do arquivo
                        output.writeObject("ARQUIVO_ENCONTRADO");
                        output.writeObject(Files.size(caminhoArquivo));

                        // Envia o arquivo para o cliente
                        try (FileInputStream fileInput = new FileInputStream(caminhoArquivo.toFile())) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileInput.read(buffer)) > 0) {
                                output.write(buffer, 0, bytesRead);
                            }
                            output.flush();
                        }
                        System.out.println("Arquivo enviado para o cliente: " + nomeArquivo);
                    } else {
                        // Se o arquivo não for encontrado, envia uma mensagem de erro
                        output.writeObject("ARQUIVO_NAO_ENCONTRADO");
                        System.out.println("Arquivo não encontrado: " + nomeArquivo);
                    }
                }

                // Se o comando for "LISTAR"
                else if (comando.equals("LISTAR")) {
                    // Lista a estrutura de diretórios e envia para o cliente
                    Path caminhoBase = Paths.get(CAMINHO_BASE);
                    String estrutura = listarDiretorios(caminhoBase, 0);
                    output.writeObject("ESTRUTURA_DIRETORIOS");
                    output.writeObject(estrutura);
                }

                // Se o comando for "SAIR"
                else if (comando.equals("SAIR")) {
                    // Sai do loop e encerra a conexão com o cliente
                    break;
                }
            }
        }
    }
}