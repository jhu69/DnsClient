
/*
    This class contains all info about sent packet, server, requests, response,
 */

import java.net.*;

public class DnsApp {


    //------------------------
    // MEMBER VARIABLES
    //------------------------

    // associations fields
    private DnsRequest request;

    // Defining the arguments that will be used to send packets to the servers
    private int timeOut;
    private int max_retries;
    private int portNumber;
    private Query queryType;
    private String domainName;
    private byte[] server;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    // Here default values of the arguments will be given to the fields of this type of instances
    public DnsApp(String[] args) {
        try {
            this.timeOut = 5;
            this.max_retries = 3;
            this.portNumber = 53;
            this.queryType = Query.A;
            this.server = new byte[4];

//            this.server[0] = (byte)(8);
//            this.server[1] = (byte)(8);
//            this.server[2] = (byte)(8);
//            this.server[3] = (byte)(8);

            parseInputs(args);
            request = new DnsRequest(getDomainName(), getQueryType());
        } catch(Exception e) {
            System.out.println("\n"+ e.getMessage()); // this is here for debugging purposes
            throw new IllegalArgumentException("ERROR\tIncorrect input syntax: Please check your arguments and try again!" + "\n");
        }
        if (server == null || domainName == null) {
            throw new IllegalArgumentException("ERROR\tIncorrect input syntax: Server IP and domain name must be provided!" + "\n");
        }
        //printArgumentValues();
    }

    //------------------------
    // INTERFACE
    //------------------------

    // Make a DNS request
    public void makeRequestAndGetResponse() throws InterruptedException {

        // Required request formatting
        System.out.println("\n" + "DnsClient sending request for " + getDomainName());
        System.out.println("Server: " + getServer().toString());
        System.out.println("Request type: " + getQueryType() + "\n");

        // To display what the DNS packet byte structure look like
//        byte[] tmp = getRequest().makeDnsRequest();
//        System.out.println("DNS packet length: " + tmp.length);
//        for (int i = 0; i < tmp.length; i++) {
//            System.out.println("DNS packet byte " + (i + 1) + ": " + tmp[i]);
//        }
//        System.out.println();
        pollDnsRequest(1);
    }

    // Polling DNS requests
    private void pollDnsRequest(int curRetryNum) throws InterruptedException {

        // Request & response bytes (for the datagram packet objects later on)
        byte[] requestBytes = new byte[1500];
        byte[] responseBytes = new byte[1500];

        while (curRetryNum <= getMax_retries()) {
            try {

                // Instantiations
                DatagramSocket clientSocket = new DatagramSocket();
                InetAddress destAddress = InetAddress.getByAddress(getServer());

                // Preparing data to be sent to server
                requestBytes = getRequest().makeDnsRequest();
//            System.out.println("Data packet length: " + requestBytes.length);
//            for (int i = 0; i < requestBytes.length; i++) {
//                System.out.println("DNS packet byte " + (i + 1) + ": " + requestBytes[i]);
//            }
//            System.out.println();

                // Datagram packet
                DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, destAddress, getPortNumber());
                DatagramPacket response = new DatagramPacket(responseBytes, responseBytes.length);

                // Sending packet, calculating response time, start timer, and set timeout
                clientSocket.setSoTimeout(getTimeOut() * 1000);
                long startTime = System.currentTimeMillis();
                clientSocket.send(request);

                // Receiving response from server, if server sent something back then normal execution till finish
                clientSocket.receive(response);
                if (response.getData() != null) {
                    long endTime = System.currentTimeMillis();
                    // Printing out response time
                    System.out.println("\n" + "Response received after: " + ((endTime - startTime) / 1000.) + " seconds " + "(" + (curRetryNum - 1) + " retries)");
                    System.out.println("Response from server: " + response.toString() + "\n");
                    clientSocket.close();
                    return;
                }

            // To deal with exceptions when there's no response from server and we need to keep sending until max retries have been reached
            } catch (SocketException e) {
                System.out.println("ERROR\tCould not create a socket: " + e.getMessage());
            } catch (UnknownHostException e) {
                System.out.println("ERROR\tUnknown host: " + e.getMessage());
            } catch (SocketTimeoutException e) {
                System.out.println("ERROR\t"+ e.getLocalizedMessage());
                System.out.println("Reattempting request...");
                curRetryNum++;
            } catch(Exception e) {
                System.out.println("ERROR\t:" + e.getMessage());
                System.out.println("Reattempting request...");
                curRetryNum++;
            }
            Thread.sleep(3000);
        }
        // Error handling when max retries have been reached
        if (curRetryNum > getMax_retries()) {
            System.out.println("ERROR\tMaximum number of retries of " + getMax_retries() + " exceeded");
            return;
        }
    }

    // To test if the parsing function parsed the input arguments correctly or not
    public void printArgumentValues() {
        System.out.println("\n" + "Time Out: " + this.timeOut);
        System.out.println("Max Retries: " + this.max_retries);
        System.out.println("Port Number: " + this.portNumber);
        System.out.println("Query Type: " + this.queryType);
        System.out.println("Server: " + this.server.toString());
        System.out.println("Domain Name: " + this.domainName + "\n");
    }

    // This function will parse arguments received from the commandline
    public void parseInputs(String[] args) {

        // Temporary char holder for copying over the chars from the @____ part of the arguments
        char[] tmpChar;

        // Parsing the input arguments based on the query conditions supplied
        for(int i = 0; i< args.length; i++) {
            if (args[i].charAt(0) == '-' && args[i].charAt(1) == 't') {
                this.timeOut = Integer.parseInt(args[i+1]);
                //System.out.println(args[i + 1]);
            } else if (args[i].charAt(0) == '-' && args[i].charAt(1) == 'r') {
                this.max_retries = Integer.parseInt(args[i+1]);
            } else if (args[i].charAt(0) == '-' && args[i].charAt(1) == 'p') {
                this.portNumber = Integer.parseInt(args[i+1]);
            } else if ((args[i].charAt(0) == '-' && args[i].charAt(1) == 'm' && args[i].charAt(2) == 'x')
                    || (args[i].charAt(0) == '-' && args[i].charAt(1) == 'n' && args[i].charAt(2) == 's')) {
                this.queryType = Query.valueOf(args[i].toUpperCase().substring(1,3));
            } else if (args[i].charAt(0) == '@') {
                tmpChar = new char[args[i].length()-1];
                for (int j = 1; j < args[i].length(); j++) {
                    tmpChar[j-1] = args[i].charAt(j);
                }

                System.out.println("Server before converting to bytes: " + new String(tmpChar));
                String truncatedServerName = new String(tmpChar);
                String[] serverLables = truncatedServerName.split("\\.");
                int ipValue;
                for (int x = 0; x < serverLables.length; x++) {
                    ipValue = Integer.parseInt(serverLables[x]);
                    if (ipValue < 0 || ipValue > 255) {
                        throw new NumberFormatException("ERROR\tIncorrect input syntax: IP Address numbers must be between 0 & 255 inclusive.");
                    }
                    server[x] = (byte) ipValue;
                }

            } else if((args[i] != null && i == 1) || (args[i] != null && i == 2) || (args[i] != null && i == 3)
            || (args[i] != null && i == 4) || (args[i] != null && i == 5) || (args[i] != null && i == 6) || (args[i] != null && i == 7) || (args[i] != null && i == 8)) {
                this.domainName = args[i];
            }
         }
    }

    public DnsRequest getRequest() {
        return request;
    }

    public void setRequest(DnsRequest request) {
        this.request = request;
    }

    // All getters and setters
    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getMax_retries() {
        return max_retries;
    }

    public void setMax_retries(int max_retries) {
        this.max_retries = max_retries;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public Query getQueryType() {
        return queryType;
    }

    public void setQueryType(Query queryType) {
        this.queryType = queryType;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public byte[] getServer() {
        return server;
    }

    public void setServer(byte[] server) {
        this.server = server;
    }
}
