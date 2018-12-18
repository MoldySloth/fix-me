package rdejage.fixme.broker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.Scanner;
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
        attach.isRead = true;
        attach.mainThread = Thread.currentThread();

//        Charset         cs = Charset.forName("UTF-8");
//        String          message = "Hello. Broker here";
//        byte[]          data = message.getBytes(cs);
//        attach.buffer.put(data);
//        attach.buffer.flip();

        ReadWriteHandler    rwHandler = new ReadWriteHandler();
        channel.read(attach.buffer, attach, rwHandler);
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
            if(message.charAt(1) == 'I') {
                System.out.println(message);
            } else {
                System.out.format("Server responded: " + message);
            }
            message = this.getTextFromUser();

            try {
                attach.buffer.clear();
                byte[]  data = message.getBytes();
                attach.buffer.put(data);
                attach.buffer.flip();
                attach.isRead = false;
                attach.channel.write(attach.buffer, attach, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private String  getTextFromUser() {
        System.out.print("please enter a message (bye to quit):\n");
        try {
            Scanner     scanner = new Scanner(System.in);
            String      message = scanner.nextLine();

            return message;
        } catch (Exception e) {
            System.out.println("No input given");
        }
        return "No input";
    }
}

// Broker need to send a message... BUY | SELL
// Will receive messages from market... Execute | Rejected