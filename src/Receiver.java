import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver extends JFrame{

    private DatagramSocket socket;
    private InetAddress ip;
    private int port;

    private int length;
    private byte[] data;

    private int offset;

    private String fileName;

    private JProgressBar progressBar;
    private JLabel label;

    public Receiver(){

        super("Receiver");
        setSize(500,150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        label = new JLabel("Waiting for file ...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        progressBar = new JProgressBar(0, 100);

        add(label, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        try {
            socket = new DatagramSocket(8080);
        }catch (Exception e){
            System.out.println("Socket already created on this port " + socket.getPort());
        }
        PackageThread pt = new PackageThread(socket, this) {
            @Override
            public void doWork() throws Exception{

                ip = packet.getAddress();
                port = packet.getPort();
                offset = 0;

                String msg = new String(receivedBuffer).trim();

                fileName = msg.substring(0, msg.lastIndexOf('.'));
                length = Integer.parseInt(msg.substring( msg.lastIndexOf('.') + 1 ));
                data = new byte[length];

                int answer = JOptionPane.showConfirmDialog(parent, fileName + " from " + ip.toString().substring(1));

                if(answer == 0) {
                    packet = new DatagramPacket("ACK".getBytes(), 3, ip, port);
                    socket.send(packet);
                    listenForFile();
                } else {
                    packet = new DatagramPacket("DEC".getBytes(), 3, ip, port);
                    socket.send(packet);
                    System.exit(answer);
                }
            }
        };
        pt.startListening();
    }

    private void listenForFile() throws Exception{

            if(offset > length){
                saveFile();
                return;
            }

            PackageThread pt = new PackageThread(socket, this) {
                @Override
                public void doWork() throws Exception {
                    if(length - offset >= 1000) {
                        for (int i = 0; i < 1000; i++) {
                            data[offset + i] = receivedBuffer[i];
                            progressBar.setValue(progressBar.getValue() + 100 / (length / 1000));
                        }
                    }else{
                        for(int i=0; i<length % 1000; i++){
                            data[length - ( length % 1000 ) + i ] = receivedBuffer[i];
                            progressBar.setValue(100);
                        }
                    }
                    offset += 1000;
                    String msg = "I received";
                    socket.send(new DatagramPacket(msg.getBytes(), msg.length(), ip, port));
                    listenForFile();
                }
            };
            pt.startListening();
    }

    private void saveFile(){
        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            stream.write(data);
            stream.close();
            label.setText("File has been received");
        } catch (Exception e){
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    //main
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        receiver.setVisible(true);
    }
}
