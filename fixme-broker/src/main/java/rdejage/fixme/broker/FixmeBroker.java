package rdejage.fixme.broker;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.InputMismatchException;
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
//        AsynchronousSocketChannel   socketChannel = AsynchronousSocketChannel.open();
//        socketChannel.connect(new InetSocketAddress("localhost", 5000));
//
//        // reading from a channel
//        ByteBuffer      buffer = ByteBuffer.allocate(48);
//
//        int             bytesRead = socketChannel.read(buffer);
//        if(bytesRead == -1) {
//            // end of the stream
//        }
//
//        // writing to the channel
//        String          newData = "New string to write to file... " + System.currentTimeMillis();
//
//        ByteBuffer      buffer = ByteBuffer.allocate(1048);
//        buffer.clear();
//        buffer.put(newData.getBytes());
//        buffer.flip();
//
//        while(buffer.hasRemaining()) {
//            socketChannel.write(buffer);
//        }
//
//        socketChannel.close();
    }
}

class Attachment {
    AsynchronousSocketChannel   channel;
    Integer                     ID;
    ByteBuffer                  buffer;
    Thread                      mainThread;
    boolean                     isRead;
}

class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
        if(attach.isRead) {
            attach.buffer.flip();
            byte[]    bytes = new byte[attach.buffer.limit()];
            attach.buffer.get(bytes);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);

            if(message.charAt(1) == 'I') {
                System.out.println(message);
                attach.ID = Integer.parseInt(message.replaceAll("[\\D]", ""));
            } else {
                System.out.format("Server responded: " + message + "\n");
            }
            message = this.getTextFromUser(attach.ID);

            try {
                attach.buffer.clear();
                byte[]  data = message.getBytes(cs);
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

    private String  getTextFromUser(Integer id) {
        String      message = "";

        // get market ID?
        System.out.println("Please enter a market ID:");
        boolean     validInput = false;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                int         MID = scanner.nextInt();
                if(MID < 1000) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(MID) + "|" + id + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a market ID (ID's start at 1000) ");
            }
        }

        // get BUY / SELL
        System.out.println("Would you like to:\n1. Buy\n2. Sell");
        validInput = false;
        int         option;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                option = scanner.nextInt();
                if(option < 1 || option > 2) {
                    System.out.println("Invalid input");
                } else if(option == 1) {
                    message += "BUY|";
                    validInput = true;
                } else if(option == 2) {
                    message += "SELL|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a number");
            }
        }

        // get Symbol
        System.out.println("Instrument symbol:");
        try {
            Scanner     scanner = new Scanner(System.in);

            message += scanner.nextLine().trim() + "|";
        } catch (InputMismatchException e) {
            System.out.println("Invalid input --> You have not entered a valid instrument");
        }
        // get Price
        System.out.println("Price:");
        validInput = false;
        int         price = 0;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                price = scanner.nextInt();
                if(price < 1) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(price) + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a valid price");
            }
        }

        // get Quantity
        System.out.println("Quantity:");
        validInput = false;
        int         quantity = 0;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                quantity = scanner.nextInt();
                if(quantity < 1) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(quantity) + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a valid quantity");
            }
        }

        // calculate checksum
        int     checksum = 0;
        for(int i = 0; i < message.length(); i++) {
            checksum += message.charAt(i);
            System.out.println("Checksum count: " + checksum);
        }

        message += Integer.toString(checksum);

        System.out.println(message);

        return message;
    }
}

// Broker need to send a message... BUY | SELL
// Will receive messages from market... Execute | Rejected
// BID|MID|BUY/SELL|SYMBOL|PRICE|QUANTITY|CHECKSUM
// message example = 1001|1000|buy|aapl|12|12|1955