public class DnsResponse {

    public static void outputResponse(byte[] sentData, byte[] receivedData) throws Exception{
        responseType(receivedData);

        //First, we validate the received packet and output any error we found
        //checkResponse(sentData,receivedData);

        //Then, we output the number of records received in the packet
        //This number is found in the AnCount section (byte 6 and 7) DOUBLE CHECK !!!
        long numRecords = getANCount(receivedData);
        System.out.println("***Answer Section" + " " + numRecords + " records***");

        System.out.println("testing the response packet " + sentData[12+15]);
        byte[] test = new byte[2];

    }

    public static void checkResponse(byte[] sentData, byte[] receivedData) throws Exception {

        //First, we compare the IDs of both the received and sent packets
        if((sentData[0] != receivedData[0]) || (sentData[1] != receivedData[1])){
            //The ID from the sent packet and the received packet do not match
            throw new Exception("Sent and received packets' ID's do not match.");
        }

        //We get the 4-bit field RCode from the received packet to check ftor different kind of errors
        byte rCode = (byte) (receivedData[3] & 15);
        if(rCode == 1) {
            //Format Error
            throw new Exception("Format Error: the name server was unable to interpret the query");
        }
        else if(rCode == 2) {
            //Server Failure
            throw new Exception("Server Failure Error: the name server was unable to process this query due to a problem with the name server");
        }
        else if(rCode == 3) {
            //Name Error
            throw new Exception("Name Error: domain name referenced in the query does not exist");
        }
        else if(rCode == 4) {
            //Not Implemented
            throw new Exception("Not Implemented Error: the name server does not support the requested kind of query");
        }
        else if(rCode == 5) {
            //Refused
            throw new Exception("Refused Error: the name server refuses to perform the requested operation for policy reasons");
        }

        //Finally, we check whether the server accepted or not our recursive queries
        int RA = getBit(receivedData[3], 7);
        if (RA==0){ //RA bit
            //Error when the server does not support the recursive queries we are sending
            throw new Exception("Requested server does not support recursive queries");
        }
    }

    //Helper method that returns the length of the domain name in the dns packet
    public static int qnameLength(byte[] receivedData){
        int n = 12;
        int length = 1;
        int temp = 0;
        //The standard defining domain names (RFC 1034) restricts labels to be at most 63 octets long
        for (int i=n; i<76; i++){
            temp = receivedData[n];
            if (temp == 0){
                break;
            }
            n++;
            length ++;
        }
        return length;
    }

    public static int responseType(byte[] receivedData){
        int qNameLength = qnameLength(receivedData);
        System.out.println(qNameLength);
        System.out.println(receivedData[12 + qNameLength+4]);
        //We first find the position of the type byte using the qNameLength
        int position = 12 + qNameLength + 4 + qNameLength;

        int type = (receivedData[position - 1] + receivedData[position] + receivedData[position + 1]);
        System.out.println(type);
        if(type == 1){
            System.out.println("A-query");
        }
        else if(type == 0x0002){
            System.out.println("NS-query");
        }
        else if(type == 0x000f){
            System.out.println("Mx-query");
        }
        else if(type == 0x0005){
            System.out.println("Cname");
        }
        else{
            System.out.println("Query Not Found");
        }
        return type;
    }

    //Helper function that returns the bit at a specific position in a byte
    public static int getBit(byte inputByte, int position){
        int a = inputByte >> position;
        int bit = a & 1;
        return bit;
    }

    public static long getTTL(byte[] receivedBytes, int qNamelength){

        long TTL = 0;
        int n = 0;
        int position = 12 + qNamelength + 4 + qNamelength +4;

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position], i);
            TTL = TTL + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position+1], i);
            TTL = TTL + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position+2], i);
            TTL = TTL + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position+3], i);
            TTL = TTL + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        return TTL;
    }

    public static long getANCount(byte[] receivedBytes){
        long ANCount = 0;
        int n = 0;
        int position = 6;

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position], i);
            ANCount = ANCount + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(receivedBytes[position+1], i);
            ANCount = ANCount + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        return ANCount;
    }

    public static String getAuthorithy(byte[] receivedByte){
        String authority = "";
        int AA = getBit(receivedByte[2], 5);
        if (AA == 1){
            authority = "Authoritative Response";
        }
        else {
            authority = "Non-authoritative Response";
        }
        return authority;
    }


}