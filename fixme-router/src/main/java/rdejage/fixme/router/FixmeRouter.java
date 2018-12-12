package rdejage.fixme.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FixmeRouter {
    private static int     ID;
    // store sockets in some sort of list of ConnectionAttachments

    // main function
    public static void  main(String[] args) {
        System.out.println("Starting the router");

        // Starting unique ID
//        ID = 1000;
        int     brokerPort = 5000;
        int     marketPort = 5001;

        try {
            // create a new socket for each
            AsynchronousServerSocketChannel brokerChannel;
            InetSocketAddress brokerHost = new InetSocketAddress("localhost", brokerPort);
            brokerChannel = AsynchronousServerSocketChannel.open().bind(brokerHost);

            AsynchronousServerSocketChannel marketChannel;
            InetSocketAddress marketHost = new InetSocketAddress("localhost", marketPort);
            marketChannel = AsynchronousServerSocketChannel.open().bind(marketHost);

            // listen to sockets
            System.out.println("Server is listening to port " + brokerHost.getPort());
            System.out.println("Server is listening to port " + marketHost.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    // attachment holds all the attachment properties
//    private class ConnectionAttachment {
//        AsynchronousServerSocketChannel     serverChannel;
//        int                                 ID;
//    }

    // test
//    private void ConnectionAttachment() throws IOException, InterruptedException, ExecutionException {
//        AsynchronousServerSocketChannel     serverChannel;
//        InetSocketAddress   hostAddress = new InetSocketAddress("localhost", brokerPort);
//        serverChannel = AsynchronousServerSocketChannel.open().bind(hostAddress);
//
//        System.out.println("Server channel bound to port: " + brokerPort);
//        System.out.println("Waiting for a connection....");
//
//        AsynchronousSocketChannel   clientChannel =
//
//        System.out.println("Messages from client: ");
//        if((clientChannel != null) && (clientChannel.isOpen())) {
//            while(true) {
//                ByteBuffer      buffer = ByteBuffer.allocate(32);
//                Future          result = clientChannel.read(buffer);
//
//                while(!result.isDone()) {
//                    // do nothing
//                }
//
//                buffer.flip();
//                String      message = new String(buffer.array()).trim();
//                System.out.println(message);
//
//                if(message.equals("Bye.")) {
//                    break;
//                }
//                buffer.clear();
//            }
//            clientChannel.close();
//        }
//        serverChannel.close();
//    }
}
