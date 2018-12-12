package rdejage.fixme.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

public class FixmeRouter {
    private static int ID;
    // store sockets in some sort of list of ConnectionAttachments

    // main function
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the router");

        // Starting unique ID
        ID = 1000;
        int     brokerPort = 5000;
        int     marketPort = 5001;
        String host = "localhost";

        // create a new socket for each
        AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open();
        InetSocketAddress brokerHost = new InetSocketAddress(host, brokerPort);
        brokerChannel.bind(brokerHost);

        AsynchronousServerSocketChannel marketChannel;
        InetSocketAddress marketHost = new InetSocketAddress("localhost", marketPort);
        marketChannel = AsynchronousServerSocketChannel.open().bind(marketHost);

        // listen to sockets
        System.out.println("Server is listening to port " + brokerHost.getPort());
        System.out.println("Server is listening to port " + marketHost.getPort());

        // attach multiple channels...
        Attachment attachBroker = new Attachment();
        attachBroker.serverChannel = brokerChannel;
        brokerChannel.accept(attachBroker, new ConnectionHandler());
        Thread.currentThread().join();

        Attachment attachMarket = new Attachment();
        attachBroker.serverChannel = marketChannel;
        marketChannel.accept(attachMarket, new ConnectionHandler());
        Thread.currentThread().join();

//            // socket accepting connections
//            brokerChannel.accept();
//            marketChannel.accept();
//            Future<AsynchronousSocketChannel>   acceptCon = brokerChannel.accept();
//            AsynchronousSocketChannel           client = acceptCon.get();
//            //Thread.currentThread().join();
//            if((client != null) && (client.isOpen())) {
//                ByteBuffer      buffer = ByteBuffer.allocate(1024);
//                Future<Integer> readVal = client.read(buffer);
////                System.out.println("Received from client: " + new String(buffer.array()).trim());
//                readVal.get();
//                buffer.flip();
//                System.out.println("Received from client: " + new String(buffer.array()).trim());
//
//                String          message = "Hi. This is router";
//                Future<Integer> writeVal = client.write(ByteBuffer.wrap(message.getBytes()));
//                System.out.println("Writing back to client: " + message);
//                writeVal.get();
//                buffer.clear();
//            }
//            client.close();
//            // Thread??
    }
}


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

// attachment holds all the attachment properties
class Attachment {
    AsynchronousServerSocketChannel     serverChannel;
    AsynchronousSocketChannel           clientChannel;
    ByteBuffer                          buffer;
    SocketAddress                       clientAddress;
    Boolean                             isRead;
    int                                 ID;
}

class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
    @Override
    public void completed(AsynchronousSocketChannel client, Attachment attach) {
        try {
            SocketAddress   clientAddr = client.getRemoteAddress();
            System.out.format("Accepted a connection from %s%n", clientAddr);
            attach.serverChannel.accept(attach, this);
            ReadWriteHandler    rwHandler = new ReadWriteHandler();
            Attachment          newAttach = new Attachment();
            newAttach.serverChannel = attach.serverChannel;
            newAttach.clientChannel = client;
            newAttach.buffer = ByteBuffer.allocate(2048);
            newAttach.isRead = true;
            newAttach.clientAddress = clientAddr;
            client.read(newAttach.buffer, newAttach, rwHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Throwable e, Attachment attach) {
        System.out.println("Failed to accept a connection.");
        e.printStackTrace();
    }
}

class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
        if(result == -1) {
            try {
                attach.clientChannel.close();
                System.out.format("Stopped listening to the client %s%n", attach.clientAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if(attach.isRead) {
            attach.buffer.flip();
            int     limits = attach.buffer.limit();
            byte    bytes[] = new byte[limits];
            attach.buffer.get(bytes, 0, limits);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);
            System.out.format("Client a %s says: %s%n", attach.clientAddress, message);
            attach.isRead = false;
            attach.buffer.rewind();
        } else {
            // write to the client
            attach.clientChannel.write(attach.buffer, attach, this);
            attach.isRead = true;
            attach.buffer.clear();
            attach.clientChannel.read(attach.buffer, attach, this);
        }
    }

    @Override
    public void failed(Throwable e, Attachment attach) {
        e.printStackTrace();
    }
}

// router opens connections for Brokers and Markets on specific Ports
// it then relays messages between the two sockets
