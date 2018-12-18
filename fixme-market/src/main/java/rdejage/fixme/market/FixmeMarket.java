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
        System.out.println("Starting the broker");

        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        SocketAddress serverAddr = new InetSocketAddress("localhost", 5000);
        Future<Void> result = channel.connect(serverAddr);
        result.get();
        System.out.println("Connected");
        Attachment      attach = new Attachment();
        attach.channel = channel;
        attach.buffer = ByteBuffer.allocate(2048);
        attach.isRead = false;
        attach.mainThread = Thread.currentThread();

        Charset cs = Charset.forName("UTF-8");
        String          message = "Hello. Market here";
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
        // API data based on symbol
        try {
            String symbol = "AAPL";
            String apiData = getMarketData(symbol);
            System.out.println(apiData);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        BufferedReader consoleReader = new BufferedReader(
                new InputStreamReader(System.in));
        String          message = consoleReader.readLine();
        return message;
    }

    // Alpha Vantage free API, market data and json object return
    private static String       getMarketData(String symbol) throws Exception {
        // API key Alpha Vantage J61CV6N8FJQNMXQK
        String      APIKey = "J61CV6N8FJQNMXQK";

        // get symbol your would like to get data for...
        // json returns high and low cost for the day, also volume...
        // https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=MSFT&apikey=demo
        String      URLstring = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + APIKey;
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
        while ((output = in.readLine()) != null) {
            System.out.println(output);
        }
        in.close();
        con.disconnect();

        return "End of API call";
    }
}

// Market hold business logic
// Receives messages from Broker...
// Replies with... Executed | Rejected