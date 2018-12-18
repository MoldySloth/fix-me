package rdejage.fixme.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class FixmeRouter {
    private static Integer  IDcurr;
    private static HashMap<Integer, Attachment>  routingTable = new HashMap<Integer, Attachment>();
    private static String   host = "localhost";
    private static int      brokerPort = 5000;
    private static int      marketPort = 5001;
    // store sockets in some sort of list of ConnectionAttachments

    // main function
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the router");
        // Starting unique ID
        IDcurr = 1000;

        // create a new socket for each
        AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open();
        InetSocketAddress brokerHost = new InetSocketAddress(host, brokerPort);
        brokerChannel.bind(brokerHost);

        AsynchronousServerSocketChannel marketChannel;
        InetSocketAddress marketHost = new InetSocketAddress(host, marketPort);
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

        // if thread is interrupted, then exit
        if(Thread.currentThread().isInterrupted()) {
            // Should close??
            return ;
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
    private static class Attachment {
        AsynchronousServerSocketChannel     serverChannel;
        AsynchronousSocketChannel           clientChannel;
        ByteBuffer                          buffer;
        SocketAddress                       clientAddress;
        Boolean                             isRead;
        Integer                             ID;
    }

    private static class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
        @Override
        public void completed(AsynchronousSocketChannel client, Attachment attach) {
            try {
                SocketAddress   clientAddr = client.getRemoteAddress();
                attach.serverChannel.accept(attach, this);
                ReadWriteHandler    rwHandler = new ReadWriteHandler();
                Attachment          newAttach = new Attachment();

                newAttach.serverChannel = attach.serverChannel;
                newAttach.clientChannel = client;
                newAttach.buffer = ByteBuffer.allocate(2048);
                newAttach.isRead = false;
                newAttach.ID = IDcurr;
                newAttach.clientAddress = clientAddr;

                newAttach.clientChannel.write(newAttach.buffer);
                newAttach.clientChannel.read(newAttach.buffer, newAttach, rwHandler);

                routingTable.put(newAttach.ID, newAttach);
                System.out.format("Accepted a connection from %s%n", clientAddr);
                System.out.println("Attachment created: " + IDcurr + "\n");
                IDcurr++;
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

    private static class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
        @Override
        public void completed(Integer result, Attachment attach) {
            if(result == -1) {
                try {
                    // remove connection from routing table
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

                // check message checksum??

                // get attachment id that needs to receive a message
                // find id in message
                Integer     id = 1000;
                // create a new attachment from attachment found
                Attachment  send = null;
                while(routingTable != null) {
                    if(routingTable.containsKey(id)) {
                        send = routingTable.get(id);
                    }
                }
                send.buffer.clear();
                System.out.format("Client at %s says: %s%n", attach.clientAddress, message);
                byte[]  data = message.getBytes(cs);
                send.buffer.put(data);
                send.buffer.flip();
                attach.isRead = false;
                attach.buffer.rewind();
                // send message to broker... using broker ID... from router table
                send.clientChannel.write(send.buffer, send, this);
            } else {
                // write to the client
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
}




// router opens connections for Brokers and Markets on specific Ports
// it then relays messages between the two sockets
