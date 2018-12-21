package rdejage.fixme.market;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

public class FixmeMarket {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the market");

        AsynchronousSocketChannel   channel = AsynchronousSocketChannel.open();
        SocketAddress   serverAddr = new InetSocketAddress("localhost", 5001);
        Future<Void>    result = channel.connect(serverAddr);
        result.get();
        System.out.println("Connected");
        Attachment      attach = new Attachment();
        attach.channel = channel;
        attach.buffer = ByteBuffer.allocate(2048);
        attach.isRead = true;
        attach.mainThread = Thread.currentThread();

        ReadWriteHandler    rwHandler = new ReadWriteHandler();
        channel.read(attach.buffer, attach, rwHandler);
        attach.mainThread.join();
    }
}

class Attachment {
    Integer                     ID;
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
            byte    bytes[] = new byte[attach.buffer.limit()];
            attach.buffer.get(bytes);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);
            //System.out.format("Server responded: " + message);
            if(message.length() > 0) {
                if(message.charAt(1) == 'I') {
                    System.out.println(message);
                    attach.ID = Integer.parseInt(message.replaceAll("[\\D]", ""));
                    attach.isRead = true;
                    attach.buffer.clear();
                    attach.channel.read(attach.buffer, attach, this);
                } else {
                    System.out.println("Server Responded: " + message + "\n");

                    // API data based on symbol
                    try {
                        // split the message into fix message data
                        String[]    messageData = message.split("\\|");
                        for(String data : messageData) {
                            System.out.println(data);
                        }

//                        String symbol = "AAPL";
//                        String apiData = getMarketData(symbol);
//                        System.out.println(apiData);
                        message = "this is the returned data form the market";
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    attach.buffer.clear();
                    byte[] data = message.getBytes();
                    attach.buffer.put(data);
                    attach.buffer.flip();
                    attach.isRead = false;
                    attach.channel.write(attach.buffer, attach, this);
                }
            } else {
                System.out.println("Connection disconnected");
                System.exit(0);
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

    // Alpha Vantage free API, market data and json object return
    private static String       getMarketData(String symbol) throws Exception {
        // get symbol your would like to get data for...
        // json returns high and low cost for the day, also volume...
        // IEX Trading API
        String      URLstring = "https://api.iextrading.com/1.0/stock/" + symbol + "/previous";

        URL         UrlObj = new URL(URLstring);
        HttpURLConnection   con = (HttpURLConnection) UrlObj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        // check response
        if(con.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code: " + con.getResponseCode());
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String      output;
        System.out.println("Output from server.....");
        StringBuffer    content = new StringBuffer();
        while ((output = in.readLine()) != null) {
            content.append(output);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }
}

// Market hold business logic
// Receives messages from Broker...
// Replies with... Executed | Rejected