
/*
 This class represents the start of the whole DNS application
 1) To run this program from the commandline cd into the "out" folder until you see the .jar file
 2) Then do java -jar DNSClientLab.jar arg1 arg2 arg3 etc however many arguments needed for the query
 3) Watch it run and do some magic tricks for you :)
*/

public class DnsClient {

    // Beginning of application
    public static void main(String[] args) throws Exception {
        try {
            // Creating DnsApp object to begin running the application
            DnsApp DNSApp = new DnsApp(args);
            DNSApp.printArgumentValues();
            DNSApp.makeRequestAndGetResponse();
            byte a = (byte) 0x01;
        } catch(Exception e) {
            System.out.println("\n" + e.getMessage());
        }


    }


}
