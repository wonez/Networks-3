import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

abstract public class PackageThread {

    public byte[] receivedBuffer;

    public JFrame parent;

    public DatagramPacket packet;
    public DatagramSocket socket;

    public Thread listen;

    public PackageThread(DatagramSocket socket, JFrame parent){
        this.socket = socket;
        this.parent = parent;
    }

    public void startListening(){
        receivedBuffer = new byte[1000];
        packet = new DatagramPacket(receivedBuffer, receivedBuffer.length);
        listen = new Thread( () -> {
            try {
                socket.receive(packet);
                doWork();
            } catch (Exception e) {
                System.out.println("Already listening on this socket, this happens if game is run on the same pc");
            }
        });
        listen.start();
    }

    public abstract void doWork() throws Exception;
}
