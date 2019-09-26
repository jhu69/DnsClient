public class DnsResponse {

    public static void outputResponse(byte[] sentData, byte[] receivedData) throws Exception{

        //First, we validate the received packet and output any error we found
        checkResponse(sentData,receivedData);

        //Then, we output the number of records received in the packet
        //This number is found in the AnCount section (byte 6 and 7)
        long numRecords = getANCount(receivedData);
        //First we check if there is any record to print
        if(numRecords == 0){
            //If there is no record, we print NOTFOUND
            System.out.println("NOTFOUND");
        }
        else {
            //Print the number of records
            System.out.println("***Answer Section" + " " + numRecords + " records***");

            //We get the type of alias from the type section of the answer
            long responseType = responseType(receivedData);

            //We print the received records in the specified format using the printRecords method
            String[] records = printRecords(receivedData, responseType);
            int l = records.length;
            for (int i = 0; i < l; i++) {
                System.out.println(records[i]);
            }
        }

        //Now we check if there are any additional records
        int addRecords = getARCount(receivedData);

        //If there additional records in the received packet, we print them in the specified format
        if (addRecords != 0){
            System.out.println(" ");
            System.out.println("***Additional Section " + addRecords + " records***");
            String[] additionalRecords = printAdditionalRecords(receivedData)   ;
            int k = additionalRecords.length;
            for (int i=0; i<k; i++){
                System.out.println(additionalRecords[i]);
            }
        }
    }
    //Helper method that throws errors after checking the received packed
    public static void checkResponse(byte[] sentData, byte[] receivedData) throws Exception {

        //First, we compare the IDs of both the received and sent packets
        if((sentData[0] != receivedData[0]) || (sentData[1] != receivedData[1])){
            //The ID from the sent packet and the received packet do not match
            throw new Exception("Sent and received packets' ID's do not match.");
        }

        //We get the 4-bit field RCode from the received packet to check for different kind of errors
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
            //qname ends by a byte = 0
            if (temp == 0){
                break;
            }
            n++;
            length ++;
        }
        return length;
    }
    //Helper method that returns the type of response we get in the answer field of the received packet
    public static long responseType(byte[] receivedData){
        //We first get the length of the qname section
        int qNameLength = qnameLength(receivedData);
        byte temp;
        //We locate the position of the type section without pointers
        int index = 12 + qNameLength + 4 + qNameLength;
        int position = index;

        //We update the position of the type section if any pointer is encountered
        for (int i = (12 + qNameLength + 4) ; i<(12 + qNameLength + 4 + qNameLength); i=i+1){
            temp = receivedData[i];
            //Pointer recognized as a byte starting with 11 (from left to right)
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                index = i;
                position = index+2;
                break;
            }
        }

        //We return the byte from the 16-bit integer
        long type = get16bit(receivedData, position);

        return type;
    }
    //Helper method that converts the numerical value of the type to a String
    public static String getType(long type){
        String responseType = "";
        if(type == 0x0001){
            responseType = "A-query";
        }
        else if(type == 0x0002){
            responseType = "NS-query";
        }
        else if(type == 0x000f){
            responseType ="Mx-query";
        }
        else if(type == 0x0005){
            responseType ="Cname";
        }
        else{
            responseType ="Query Not Found";
        }
        return responseType;
    }

    //Helper function that returns the bit at a specific position in a byte
    public static int getBit(byte inputByte, int position){
        int a = inputByte >> position;
        int bit = a & 1;
        return bit;
    }
    //Helper method that returns the value
    public static long getTTL(byte[] receivedBytes){
        //We first find the starting position of the TTL field in the answer section if there are no pointers
        int qNameLength = qnameLength(receivedBytes);
        byte temp;
        int TTLPosition = 12 + qNameLength + 4 + qNameLength + 4;

        //We update the position of the TTL field if any pointer is encountered
        for (int i = 12 ; i<(12 + qNameLength + 4 + qNameLength + 10); i=i+1){
            temp = receivedBytes[i];
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                TTLPosition = i+6;
                break;
            }
        }
        //We return the integer value of the 32-bit integer of the TTL section
        long TTL = get32bit(receivedBytes, TTLPosition);
        return TTL;
    }
    //Helper method to find the number of records in the answer section by reading the ANcount section
    public static long getANCount(byte[] receivedBytes){
        int position = 6;
        long ANCount = get16bit(receivedBytes, position);
        return ANCount;
    }

    //Helper method that returns the value of a 16-bit integer by combining 2 8-bit integers
    public static long get16bit(byte[] input, int startingPosition){
        long result = 0;
        int n = 0;
        int position = startingPosition;
        //The value of the 16-bit integer is calculated by converting the binary number into decimal
        //We multiply each bit of the 16-bit integer by 2 to a power equals to its position
        for (int i=0; i<8; i++){
            int bit = getBit(input[position+1], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(input[position], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        return result;
    }
    //Method that reads the AA bit in the header to know if the response is authoritative or not
    public static String getAuthorithy(byte[] receivedByte){
        String authority = "";
        //A is the 5th bit in the third byte of the header (the byte right after the header)
        int AA = getBit(receivedByte[2], 5);
        if (AA == 1){
            authority = "auth";
        }
        else {
            authority = "nonauth";
        }
        return authority;
    }
    //Method that prints each record in the specified format
    public static String[] printRecords(byte[] receivedPacket, long type){
        //Start by defining the String array that the method will return
        //This array contain all the records in the specified format
        int numberOfRecords = (int) getANCount(receivedPacket);
        String[] records = new String[numberOfRecords];
        String alias = "";

        int qNameLength = qnameLength(receivedPacket);
        byte temp;
        int rDataPosition = 12 + qNameLength + 4 + qNameLength + 10;
        long offset=0;
        int RDLengthPosition = 0;
        long RDLength = 0;
        int ip =0;

        //We get the type of alias
        long responseType = responseType(receivedPacket);
        String responseTypeString = getType(type);

        //We get the seconds can cache
        long secondsCache = getTTL(receivedPacket);

        //We get the authority
        String authority = getAuthorithy(receivedPacket);

        //Find the starting position of the RDATA bytes and the RDLength bytes
        for (int i = 12 ; i<(12 + qNameLength + 4 + qNameLength + 10); i=i+1){
            temp = receivedPacket[i];
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                rDataPosition = i+12;
                RDLengthPosition = i + 10;
                break;
            }
        }
        //We make sure that the class is equal to 1
        long classData = get16bit(receivedPacket, (rDataPosition-8));
        if(classData != 1){
            System.out.println("ERROR: Class not equal to 1");
        }

            //Length of the RDATA field
            RDLength = get16bit(receivedPacket, RDLengthPosition);

        //Case where the response type is A which means RDATA holds ip addresses
        //In this case the record is an IP address of length RDLength
        if (type == 1){
            for (int j=0; j<numberOfRecords; j++) {
                for (int i = rDataPosition; i < (rDataPosition + RDLength); i = i + 1) {
                    temp = receivedPacket[i];
                    ip = temp;
                    //Here we are parsing to get numbers so if the byte returns a negative number we need to
                    //convert it to a positive one
                    if (ip < 0) {
                        ip = ip + 256;
                    }
                    alias = alias + ip;

                    if(i < (rDataPosition + RDLength - 1)){
                        alias = alias + ".";
                    }

                }
                //We print the record in the specified format
                records[j] = responseTypeString + " " + alias + " " + secondsCache + " " + authority;
                rDataPosition = rDataPosition + (int) RDLength;
                alias = "";
            }
        }
        //Case where the type of response is CNAME
        else if (type == 5){
            //Print as many records as specified in the ANCount
            for (int j=0; j<numberOfRecords; j++) {
                for (int i = rDataPosition; i < (rDataPosition + RDLength); i = i + 1) {
                    temp = receivedPacket[i];

                    //Checking if there is a pointer and if so, parse the name where it points
                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset2 = get16bit(receivedPacket, i) - 49152;
                        alias = alias + parseName(receivedPacket, (int) offset2);
                        break;
                    }
                    //Parsing the name
                    alias = alias + (char)receivedPacket[i];
                }
                //Add the record in the specified format
                records[j] = responseTypeString + " " + alias + " " + secondsCache + " " + authority;
                alias = "";
            }
        }

        else if (type == 2){
            for (int j=0; j<numberOfRecords; j++){
                int index = rDataPosition;
                //Print as many records as specified in the ANCount
                for (int i = rDataPosition; i < (rDataPosition + RDLength); i = i + 1){
                    temp = receivedPacket[index];
                    //We stop parsing when we encounter the 0-byte
                    if(temp == 0){
                        break;
                    }
                    //If any pointer is encountered, we record where it's pointing
                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset2 = get16bit(receivedPacket, index) - 49152;
                        alias = alias + parseName(receivedPacket, (int) offset2);
                        break;
                    }
                    //Parse the record that is the same format as the Qname field
                    int n = temp;
                    for (int l = 1; l<n; l++){
                        alias = alias + (char) receivedPacket[index+l];
                    }
                    index = index + n + 1;
                }
                //Record the answer in the specified format
                records[j] = responseTypeString + " " + alias + " " + secondsCache + " " + authority;
                alias = "";
            }
        }
        //Case where the type of response is MX
        else if (type == 0x000f){
            //We first get the record in the preference field
            long preference = get16bit(receivedPacket, rDataPosition);

            //Print as many records as specified in the ANCount
            for (int j=0; j<numberOfRecords; j++){
                int index = rDataPosition + 2;
                for (int i = rDataPosition; i < (rDataPosition + RDLength); i = i + 1){
                    temp = receivedPacket[index];
                    //We stop parsing when we encounter the 0-byte
                    if(temp == 0){
                        break;
                    }
                    //If any pointer is encountered, we record where it's pointing
                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset15 = get16bit(receivedPacket, index) - 49152;
                        alias = alias + parseName(receivedPacket, (int) offset15);
                        break;
                    }
                    //We parse the record which is in the same format as the qname field
                    int n = temp;
                    for (int l = 1; l<n; l++){
                        alias = alias + (char) receivedPacket[index+l];
                    }
                    index = index + n + 1;

                }
                //Save the record in the specified format
                records[j] = responseTypeString + " " + "Preference: " + preference + " " + "Exchange: " + alias + " " + secondsCache + " " + authority;
                alias = "";
            }
        }
        else {
            for (int j=0; j<numberOfRecords; j++){
                records[j] = "Type not recognized";
            }

        }
        return records;
    }
    //Helper method that parses any record that is the format of the QName field
    public static String parseName(byte[] input, int position){
        byte temp = 0x01;
        String name = "";
        int n = 0;
        //Stops parsing when the 0-byte is encountered
        while(temp != 0){
            //First byte holds the number of characters coming in the following bytes
            n = input[position];
            //Parsing the following bytes
            for(int i=1; i<=n; i++){
                name = name + (char) input[position + i];
            }
            //Updating the position after the bytes are parsed
            position = position + n + 1;
            temp = input[position];
            if (temp != 0){
                name = name + ".";
            }
        }
        return name;

    }
    //Helper method that returns the number of records in the additional section
    //We get this number by reading the 16-bit number in the ARCount field
    public static int getARCount(byte[] input){
        int ARCount = (int) get16bit(input, 10);
        return ARCount;
    }
    //Method that reads the records in the additional section
    public static String[] printAdditionalRecords(byte[] receivedPacket){

        int numberOfAdditionalRecords = (int) getARCount(receivedPacket);
        String[] additionalRecords = new String[numberOfAdditionalRecords];
        String additionalAlias = "";

        int qNameLength = qnameLength(receivedPacket);
        byte temp;

        int RDLengthPosition = 0;
        long RDLength = 0;
        int ip =0;

        //We get the authority
        String authority = getAuthorithy(receivedPacket);

        //We first find the starting position of the RDATA bytes and the RDLength bytes in the answer field
        //We set this position to a know number if no pointers are encountered
        int rDataPosition = 12 + qNameLength + 4 + qNameLength + 10;

        //If any pointer is encountered, we update the position of the RDATA position
        for (int i = 12 ; i<(12 + qNameLength + 4 + qNameLength + 10); i=i+1){
            temp = receivedPacket[i];
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                rDataPosition = i+12;
                RDLengthPosition = i + 10;
                break;
            }
        }
        //Length of the RDATA field in the answer section
        RDLength = get16bit(receivedPacket, RDLengthPosition);

        //We find the position of the RData field in the authority section by adding the length of the RDATA
        //Which is held in the RLength field
        int rAuthPosition = rDataPosition + (int) RDLength + qNameLength + 10;
        int RDLengthAuthPosition = rDataPosition + (int) RDLength + qNameLength + 8;
        int rank = rDataPosition + (int) RDLength;
        //We update the position if any pointer is encountered
        for (int i = rank ; i<(rank + qNameLength + 10); i=i+1){
            temp = receivedPacket[i];
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                rAuthPosition = i+12;
                RDLengthAuthPosition = i + 10;
                break;
            }
        }
        //Length of the RDATA field in the authority section
        int RDAuthLength = (int) get16bit(receivedPacket, RDLengthAuthPosition);

        //Finally, using the previous information and the same logic, we find the starting position of the
        //RDATA and RLength fields in the additional section
        int rAdditionalPosition = rAuthPosition + (int) RDAuthLength + qNameLength + 10;
        int RDLengthAdditionalPosition = rAuthPosition + (int) RDAuthLength + qNameLength + 8;
        int rank2 = rAuthPosition + (int) RDAuthLength;

        for (int i = rank2 ; i<(rank2 + qNameLength + 10); i=i+1){
            temp = receivedPacket[i];
            if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                rAdditionalPosition = i+12;
                RDLengthAdditionalPosition = i + 10;
                break;
            }
        }
        //Checking the class field is equal to 1
        long classData = get16bit(receivedPacket, (rAdditionalPosition-8));
        if(classData != 1){
            System.out.println("ERROR: Class not equal to 1");
        }

        int RDAdditionalLength = (int) get16bit(receivedPacket, RDLengthAdditionalPosition);

        //We get the type of alias from the type field of the additional section
        int type = (int) get16bit(receivedPacket, (RDLengthAdditionalPosition - 8));
        String responseTypeString = getType(type);

        //We get the seconds can cache in the TTL field of the additional section
        long secondsCache = get32bit(receivedPacket, (RDLengthAdditionalPosition - 4));

        //Printing each record follows the same logic and steps as the the printRecord method
        //Refer to printRecord for detailed comments
        //Case where the response type is A which means RDATA holds ip addresses
        if (type == 1){
            for (int j=0; j<numberOfAdditionalRecords; j++) {
                for (int i = rAdditionalPosition; i < (rAdditionalPosition + RDAdditionalLength); i = i + 1) {
                    temp = receivedPacket[i];
                    ip = temp;
                    if (ip < 0) {
                        ip = ip + 256;
                    }
                    additionalAlias = additionalAlias + ip;

                    if(i < (rAdditionalPosition + RDAdditionalLength - 1)){
                        additionalAlias = additionalAlias + ".";
                    }

                }
                additionalRecords[j] = responseTypeString + " " + additionalAlias + " " + secondsCache + " " + authority;
                rAdditionalPosition = rAdditionalPosition + (int) RDLengthAdditionalPosition;
                additionalAlias = "";
            }
        }

        else if (type == 5){
            for (int j=0; j<numberOfAdditionalRecords; j++) {
                for (int i = rAdditionalPosition; i < (rAdditionalPosition + RDLengthAdditionalPosition); i = i + 1) {
                    temp = receivedPacket[i];

                    //Checking if there is a pointer and if so, parse the name where it points
                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset2 = get16bit(receivedPacket, i) - 49152;
                        additionalAlias = additionalAlias + parseName(receivedPacket, (int) offset2);
                        break;
                    }
                    //Parsing the name
                    additionalAlias = additionalAlias + (char)receivedPacket[i];
                }
                additionalRecords[j] = responseTypeString + " " + additionalAlias + " " + secondsCache + " " + authority;
                additionalAlias = "";
            }
        }

        else if (type == 2){
            int index = rAdditionalPosition;
            for (int j=0; j<numberOfAdditionalRecords; j++){
                //int index = rDataPosition;

                for (int i = rAdditionalPosition; i < (rAdditionalPosition + RDLengthAdditionalPosition); i = i + 1){
                    temp = receivedPacket[index];

                    if(temp == 0){
                        break;
                    }

                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset2 = get16bit(receivedPacket, index) - 49152;
                        additionalAlias = additionalAlias + parseName(receivedPacket, (int) offset2);
                        break;
                    }

                    int n = temp;
                    for (int l = 1; l<n; l++){
                        additionalAlias = additionalAlias + (char) receivedPacket[index+l];
                    }

                    index = index + n + 1;

                }
                additionalRecords[j] = responseTypeString + " " + additionalAlias + " " + secondsCache + " " + authority;
                additionalAlias = "";
            }
        }

        else if (type == 0x000f){
            long preference = get16bit(receivedPacket, rAdditionalPosition);

            for (int j=0; j<numberOfAdditionalRecords; j++){
                int index = rAdditionalPosition + 2;
                for (int i = rAdditionalPosition; i < (rAdditionalPosition + RDLengthAdditionalPosition); i = i + 1){
                    temp = receivedPacket[index];
                    if(temp == 0){
                        break;
                    }

                    if (getBit(temp, 6) == 1 & getBit(temp, 7) == 1){
                        long offset15 = get16bit(receivedPacket, index) - 49152;
                        additionalAlias = additionalAlias + parseName(receivedPacket, (int) offset15);
                        break;
                    }

                    int n = temp;
                    for (int l = 1; l<n; l++){
                        additionalAlias = additionalAlias + (char) receivedPacket[index+l];
                    }
                    index = index + n + 1;

                }
                additionalRecords[j] = responseTypeString + " " + "Preference: " + preference + " " + "Exchange: " + additionalAlias + " " + secondsCache + " " + authority;
                additionalAlias = "";
            }
        }
        else {
            for (int j=0; j<numberOfAdditionalRecords; j++){
                additionalRecords[j] = "Type not recognized";
            }

        }
        return additionalRecords;
    }
    //Helper method that returns the decimal value of 32bit integer of a packet
    public static long get32bit(byte[] input, int position){
        long result = 0;
        int n = 0;

        //The value of the 32-bit integer is calculated by converting the binary number into decimal
        //We multiply each bit of the 32-bit integer by 2 elevated to a power equals to its position
        for (int i=0; i<8; i++){
            int bit = getBit(input[position+3], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(input[position+2], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(input[position+1], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        for (int i=0; i<8; i++){
            int bit = getBit(input[position], i);
            result = result + bit * (long) Math.pow(2, n);
            n=n+1;
        }

        return result;
    }

}