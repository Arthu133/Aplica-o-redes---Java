import javax.sound.sampled.*;
import java.net.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import javax.swing.*;
import java.awt.event.*;

/**
 * Cliente de texto
 */
public class Cliente implements ActionListener{

    /**
     * Endereço do servidor de audio
     */
    private static String enderecoServidorAudio;

    /**
     * Porta do servidor de texto
     */
    private static String enderecoServidorTexto;

    /**
     * Porta do servidor de audio
     */
    private static final int portaServidorAudio = 9876;

    /**
     * Porta da cliente de audio
     */
    private static final int portaClienteAudio = 9879;

    /**
     * Porta do servidor de texto
     */
    private static int portaServidorTexto = 6790;

    private static boolean clienteRodando = true;
    private static DatagramSocket clientSocket;
    private static boolean servidorRodando = true;
    private static DatagramSocket serverSocket;

    static Socket socket;
    static DataOutputStream saida;
    static BufferedReader entrada;

    static JFrame frame;
    JTextField caixaMensagem;
    JTextArea caixaLog;
    JButton buttonSend;

    static Cliente cliente;
    //static BufferedImage imagem;
    static String chatLog = "";

    Cliente() {
        //Cria a tela
        frame = new JFrame("Cliente Chat");
        caixaMensagem = new JTextField("");
        caixaMensagem.setBounds(10,320, 310,30);
        caixaLog = new JTextArea("", 20, 1);
        caixaLog.setBounds(10, 10, 360, 300);
        caixaLog.setEditable(false);
        caixaLog.setLineWrap(true);
        buttonSend = new JButton(">");
        buttonSend.setBounds(320, 320, 50, 30);
        buttonSend.addActionListener(this);
        frame.add(caixaMensagem);
        frame.add(caixaLog);
        frame.add(buttonSend);
        frame.setSize(400, 400);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    // Trata evento de apertar um botão
    public void actionPerformed(ActionEvent e) {
        try {
            if(e.getSource() == buttonSend) {
                enviarMsg(caixaMensagem.getText());
            }
        } catch (Exception exc) {
            System.out.println(exc.getMessage());
        }
    }


    //Efetua a primitiva send
    public void enviarMsg(String msg) throws Exception {
        conectarEscrever();
        saida.writeBytes(msg + '\n');
        socket.close();
    }


    public static void conectarLer() throws Exception{
        //Efetua a primitiva socket e connect, respectivamente.
        socket = new Socket(enderecoServidorTexto, portaServidorTexto);
        saida = new DataOutputStream(socket.getOutputStream());

        //Efetua a primitiva receive
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        saida.writeBytes("#LER\n");
    }


    public static void conectarEscrever() throws Exception{
        //Efetua a primitiva socket e connect, respectivamente.
        socket = new Socket(enderecoServidorTexto, portaServidorTexto);
        saida = new DataOutputStream(socket.getOutputStream());
        //Efetua a primitiva receive
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    public static void rodarClienteTexto() throws Exception
    {
        cliente = new Cliente();

        try {
            while (true) {
                conectarLer();
                Object[] linhas = entrada.lines().toArray();
                chatLog = "";
                for(int i=0; i<linhas.length; i++) {
                    chatLog += linhas[i] + "\n";
                }
                cliente.caixaLog.setText(chatLog);

                //Efetua a primitiva close
                socket.close();
                Thread.sleep(1000);

            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void rodarClienteAudio() {
        try {
            clientSocket = new DatagramSocket(portaClienteAudio);

            // Configuração do formato de áudio
            AudioFormat audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            InetAddress serverAddress = InetAddress.getByName(enderecoServidorAudio);

            byte[] buffer = new byte[1024];

            // Thread para receber áudio do servidor
            Thread receiveThread = new Thread(() -> {
                try {
                    while (clienteRodando) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        clientSocket.receive(receivePacket);

                        // Reproduzir o áudio recebido
                        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
                        sourceDataLine.open(audioFormat);
                        sourceDataLine.start();
                        sourceDataLine.write(buffer, 0, buffer.length);
                        sourceDataLine.drain();
                        sourceDataLine.close();
                    }
                } catch (SocketException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            receiveThread.start();

            // Thread para enviar áudio para o servidor
            Thread sendThread = new Thread(() -> {
                try {
                    while (clienteRodando) {
                        int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                        DatagramPacket sendPacket = new DatagramPacket(buffer, bytesRead, serverAddress, portaServidorAudio);
                        clientSocket.send(sendPacket);
                    }
                } catch (SocketException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            sendThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void rodarServidorAudio() {
        try {
            serverSocket = new DatagramSocket(portaServidorAudio);

            // Configuração do formato de áudio
            AudioFormat audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            System.out.println("Servidor esperando conexões...");

            byte[] buffer = new byte[1024];

            // Thread para receber áudio do cliente
            Thread receiveThread = new Thread(() -> {
                try {
                    while (servidorRodando) {
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(receivePacket);

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivePacket.getData());
                        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, buffer.length / audioFormat.getFrameSize());

                        int bytesRead;
                        while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                            sourceDataLine.write(buffer, 0, bytesRead);
                        }
                        audioInputStream.close();
                    }
                } catch (SocketException e) {
                    // Esperado quando o socket é fechado
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            receiveThread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String obterEndereco(final String mensagem1, final String mensagem2) {

        /**
         * Endereço do servidor obtido do usuário
         */
        String enderecoServidor = "";

        // Criando o JFrame
        final JFrame jFrame = new JFrame(mensagem1);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (enderecoServidor.isEmpty()) {
            // Criando a entrada de texto
            final JTextField campoEnderecoServidor = new JTextField();

            // Array de componentes
            Object[] componentes = {mensagem2, campoEnderecoServidor};

            // Mostrando o diálogo de entrada
            int resultado = JOptionPane.showConfirmDialog(jFrame, componentes, "Digite o endereço do Servidor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // Processando o resultado do diálogo de entrada
            if (resultado == JOptionPane.OK_OPTION)
                enderecoServidor = campoEnderecoServidor.getText();
        }

        // Fechando o jFrame
        jFrame.dispose();

        return enderecoServidor;
    }


    public static void main(String[] args){
        enderecoServidorTexto = obterEndereco("Endereço do servidor de texto:", "Endereço do servidor de texto");
        enderecoServidorAudio = obterEndereco("Endereço do servidor de audio", "Endereço do servidor de audio");
        Thread threadServidor = new Thread(() -> {
            try {
                rodarServidorAudio();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });

        Thread threadCliente = new Thread(() -> {
            try {
                rodarClienteAudio();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });

        Thread threadTexto = new Thread(() -> {
            try {
                rodarClienteTexto();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });

        threadServidor.start();
        threadCliente.start();
        threadTexto.start();
    }
}