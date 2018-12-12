package rdejage.fixme.broker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class FixmeBroker {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the broker");

        AsynchronousSocketChannel   channel = AsynchronousSocketChannel.open();
        SocketAddress   serverAddr = new InetSocketAddress("localhost", 5000);
        Future<Void>    result = channel.connect(serverAddr);
        result.get();
        System.out.println("Connected");
        Attachment      attach = new Attachment();
        attach.channel = channel;
        attach.buffer = ByteBuffer.allocate(2048);
        attach.isRead = false;
        attach.mainThread = Thread.currentThread();

        Charset         cs = Charset.forName("UTF-8");
        String          message = "Hello. Broker here";
        byte[]          data = message.getBytes(cs);
        attach.buffer.put(data);
        attach.buffer.flip();

        ReadWriteHandler    rwHandler = new ReadWriteHandler();
        channel.write(attach.buffer, attach, rwHandler);
        attach.mainThread.join();
    }
}

class Attachment {
    AsynchronousSocketChannel   channel;
    ByteBuffer                  buffer;
    Thread                      mainThread;
    boolean                     isRead;
}

class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
        if(attach.isRead) {
            attach.buffer.flip();
            int     limits = attach.buffer.limit();
            byte    bytes[] = new byte[limits];
            attach.buffer.get(bytes, 0, limits);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);
            System.out.format("Server responded: " + message);
            try {
                message = this.getTextFromUser();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(message.equalsIgnoreCase("bye")) {
                attach.mainThread.interrupt();
                return;
            }
            attach.buffer.clear();
            byte[]  data = message.getBytes();
            attach.buffer.put(data);
            attach.buffer.flip();
            attach.isRead = false;
            attach.channel.write(attach.buffer, attach, this);
        } else {
            attach.isRead = true;
            attach.buffer.clear();
            attach.channel.read(attach.buffer, attach, this);
        }
    }

    @Override
    public void failed(Throwable e, Attachment attach) {
        e.printStackTrace();
    }

    private String  getTextFromUser() throws Exception {
        System.out.print("please enter a message (bye to quit):");
        BufferedReader  consoleReader = new BufferedReader(
                new InputStreamReader(System.in));
        String          message = consoleReader.readLine();
        return message;
    }
}

// Broker need to send a message... BUY | SELL
// Will receive messages from market... Execute | Rejected