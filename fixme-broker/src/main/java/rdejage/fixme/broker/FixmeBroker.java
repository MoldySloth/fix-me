package rdejage.fixme.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FixmeBroker {
    public static void main(String[] args) {
        System.out.println("Starting the broker");
        try (AsynchronousSocketChannel client = AsynchronousSocketChannel.open()) {
            Future<Void>    result = client.connect(
                    new InetSocketAddress("localhost",5000));
            result.get();
            String      message = "Hello. This is the broker";
            ByteBuffer  buffer = ByteBuffer.wrap(message.getBytes());
            Future<Integer> writeInterval = client.write(buffer);
            System.out.println("Writing to server: " + message);
            writeInterval.get();
            buffer.flip();
            Future<Integer> readVal = client.read(buffer);
            readVal.get();
            buffer.clear();
            System.out.println("Received from server: " + new String(buffer.array()).trim());
        } catch (IOException | ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Disconnected from server");
        }
    }
}
