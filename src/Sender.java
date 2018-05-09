import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Arrays;

public class Sender extends JFrame implements ActionListener{

    private JFileChooser chooser;
    private File file;

    private JButton send;
    private JTextField path;
    private JButton attach;

    private DatagramPacket packet;
    private DatagramSocket socket;

    private InetAddress ip;
    private int port = 8080;

    public Sender(){

        super("File Transfer");
        setSize(new Dimension(500,100));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel jp = new JPanel(new FlowLayout());

        path = new JTextField(30);
        attach = new JButton("Attach");

        attach.addActionListener(this);

        jp.add(path);
        jp.add(attach);

        send = new JButton("Send");
        send.addActionListener(this);

        add(jp, BorderLayout.CENTER);
        add(send, BorderLayout.SOUTH);

        try {
            socket = new DatagramSocket();
        }catch (SocketException e){
            System.out.println("Something went wrong, socket already exists on this port: " + socket.getPort());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getActionCommand().equals("Attach")) {

            chooser = new JFileChooser();
            chooser.showOpenDialog(null);
            file = chooser.getSelectedFile();
            path.setText(file.getAbsolutePath());

        }else if(e.getActionCommand().equals("Send")){

            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                chooseReceiver(bytes);
            }catch (Exception ex){
                System.out.println("Something went wrong");
            }
        }
    }

    private void sendPackets(byte[] bytes, int i){

        if(bytes.length - i * 1000 > 1000) {
            byte[] arr = Arrays.copyOfRange(bytes, i * 1000, (i + 1) * 1000);
            packet = new DatagramPacket(arr, 1000, ip, port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                System.out.println("Something went wrong");
            }
            PackageThread pt = new PackageThread(socket, this) {
                @Override
                public void doWork() throws Exception {
                    if (new String(receivedBuffer).trim().equals("I received")) {
                        sendPackets(bytes, i + 1);
                    }else{
                        System.out.println("error occurred");
                    }
                }
            };
            pt.startListening();
        }else {

            byte[] arr = Arrays.copyOfRange(bytes, bytes.length - ( bytes.length % 1000), bytes.length);
            packet = new DatagramPacket(arr, 1000, ip, port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                System.out.println("Something went wrong");
            }
        }
    }


    private void chooseReceiver(byte[] bytes){

        try {
            packet = new DatagramPacket((file.getName() + "." + bytes.length).getBytes(), file.getName().length(),
                            InetAddress.getByName("255.255.255.255"), port);
            socket.send(packet);
        }catch (Exception e){
            System.out.println("Something went wrong!");
        }

        PackageThread pt = new PackageThread(socket, this) {
            @Override
            public void doWork() throws Exception {
                String msg = new String(receivedBuffer);
                if(msg.trim().equals("ACK")) {
                    ip = packet.getAddress();
                    int answer = JOptionPane.showConfirmDialog(parent, "Send file to " + ip.toString().substring(1));
                    if(answer == 0){
                        sendPackets(bytes, 0);
                    }
                }else if(msg.trim().equals("DEC")){
                    JOptionPane.showMessageDialog(parent, "Other side has to accept the file");
                }
            }
        };
        pt.startListening();
    }


    //main
    public static void main(String[] args) {

        Sender sd = new Sender();
        sd.setVisible(true);
    }
}
