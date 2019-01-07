package rdejage.fixme.market;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

import org.json.JSONObject;

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
                // get the ID from the charBuffer written to the channel
                if(message.charAt(1) == 'I') {
                    //System.out.println(message);
                    String      messageID = message.replaceAll("[^0-9]", "");
                    Integer     id = Integer.parseInt(messageID);
                    attach.ID = id;
                    attach.isRead = true;
                    attach.buffer.clear();
                    attach.channel.read(attach.buffer, attach, this);
                } else {
                    System.out.println("Server Responded: " + message + "\n");

                    // API data based on symbol
                    try {
                        // split the message into fix message data
                        String[]    messageData = message.split("\\|");

                        // get market data and return fix message
                        // get broker ID
                        Integer     brokerID = Integer.parseInt(messageData[1]);

                        // get BUY / SELL
                        String      action = messageData[2];
                        // if sell... then add to sales available??

                        // get Instrument symbol
                        String      instrument = messageData[3].toLowerCase();

                        // get Price
                        Integer     price = Integer.parseInt(messageData[4]);

                        // get Quantity
                        Integer     quantity = Integer.parseInt(messageData[5]);

                        // get API data from json string
                        String apiData = getMarketData(instrument);
                        System.out.println(apiData);
                        JSONObject  json = new JSONObject(apiData);
                        String    mPrice = json.getString("iexBidPrice");
                        System.out.println("Json has returned " + mPrice + " as the price");
                        // symbol
                        // "iexVolume": 82451,
                        //        "avgTotalVolume": 29623234,
                        //        "iexBidPrice": 153.01,
                        //        "iexBidSize": 100,
                        //        "iexAskPrice": 158.66,
                        //        "iexAskSize": 100,

                        // get status from API data analysis
                        String      status = "Rejected";


                        // Construct message
                        String  marketMessage = "";
                        marketMessage += brokerID + "|" + attach.ID + "|";
                        marketMessage += status + "|";
                        marketMessage += action + "|";
                        marketMessage += price + "|";
                        marketMessage += quantity + "|";

                        // calculate checksum
                        int     checksum = 0;
                        for(int i = 0; i < message.length(); i++) {
                            checksum += message.charAt(i);
                        }

                        marketMessage += Integer.toString(checksum);

                        // Send message
                        message = marketMessage;
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
        String      URLstring = "https://api.iextrading.com/1.0/stock/" + symbol + "/quote";

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

    private static String   getMessageStatus(String message, String data) {

        return "rejected";
    }
}

// Market hold business logic
// Receives messages from Broker...
// Replies with... Executed | Rejected