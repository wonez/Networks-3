import javax.swing.*;
import java.awt.*;
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

    public Receiver(){

        super("Receiver");
        setSize(300,150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel("Waiting for file ...", SwingConstants.CENTER);

        add(label, BorderLayout.CENTER);

        try {
            socket = new DatagramSocket(8080);
        }catch (Exception e){
            System.out.println("Socket already created on this port " + socket.getPort());
        }
        PackageThread pt = new PackageThread(socket, this) {
            @Override
            public void doWork() throws Exception {

                ip = packet.getAddress();
                port = packet.getPort();
                offset = 0;

                String msg = new String(receivedBuffer).trim();

                length = Integer.parseInt(msg.substring( msg.lastIndexOf('.') + 1 ));
                data = new byte[length];

                int answer = JOptionPane.showConfirmDialog(parent,
                                 msg.substring(0, msg.lastIndexOf('.')) + " from " + ip.toString().substring(1));
                if(answer == 0) {
                    packet = new DatagramPacket("ACK".getBytes(), 3, ip, port);
                    listenForFile();
                } else {
                    packet = new DatagramPacket("DEC".getBytes(), 3, ip, port);
                    System.exit(answer);
                }
                socket.send(packet);
            }
        };
        pt.startListening();
    }

    private void listenForFile(){

            if(offset > length){
                return;
            }

            PackageThread pt = new PackageThread(socket, this) {
                @Override
                public void doWork() throws Exception {
                    if(length - offset > 1000) {
                        for (int i = 0; i < 1000; i++) {
                            data[offset + i] = receivedBuffer[i];
                        }
                    }else{
                        for(int i=0; i<length % 1000; i++){
                            data[length - ( length % 1000 ) + i ] = receivedBuffer[i];
                        }
                    }
                    offset += 1000;
                    listenForFile();
                }
            };
            pt.startListening();
    }

    //main
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        receiver.setVisible(true);
    }
}
