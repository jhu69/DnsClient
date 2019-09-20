/*
  This class represents the DNS requests which includes the DNS packet header and questions
 */

import java.nio.ByteBuffer;
import java.util.Random;

public class DnsRequest {

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    // This is for creating the Dns Packet Structure to hold Header & Question sections
    private ByteBuffer requestPacket;

    // Random number generator for generating a new 16 bit ID for header section
    private Random rand;

    // A DNS request shall take in the basic information required from the arguments parsed from the commandline
    private String domain;
    private Query typeOfRequest;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    public DnsRequest(String domain, Query typeOfRequest) {
        try {
            this.rand = new Random();
            this.domain = domain;
            this.typeOfRequest = typeOfRequest;
            // Allocating total size for request packet: 12 bytes for header, 5 bytes for QTYPE + QCLASS & unknown many of bytes for QName
            requestPacket = ByteBuffer.allocate(12 + 5 + findQNameByteLength());
            // Relevant info for data impression onto DNS packet
            System.out.println("\n" + "Finished allocating space for DNS packet buffer!");
            System.out.println("QName length: " + findQNameByteLength());

        } catch (Exception e) {
            System.out.println("\n" + e.getMessage() + "\n");
        }
    }

    //------------------------
    // INTERFACE
    //------------------------

    // Makes the DNS request
    public byte[] makeDnsRequest() {

        createDnsRequestPacket();
        return this.requestPacket.array();
    }

    // Creates the full DNS Request packet
    private void createDnsRequestPacket() {

        // Populate DNS Header and Question sections
        populateRequestHeader();
        populateQuestion();
    }

    // This function will populate the request header
    private void populateRequestHeader() {

        // Unique ID for our packet
        byte[] randomID = new byte[2];
        this.rand.nextBytes(randomID);
        this.requestPacket.put(randomID);

        // Unique ID for our packet
//        this.requestPacket.put((byte) 0xEE); // 1st byte
//        this.requestPacket.put((byte) 0xCE); // 2nd byte

        // section 2 after ID?? Ask TA
        // QR: 0		(DNS Request)
        // Opcode: 0000 (Standard query)
        // AA: 0		(Authoritative response - reserved for response)
        // TC: 0		(Truncated response - reserved for response)
        // RD: 1		(Recursion desired)

        // RA: 0		(Recursion supported - reserved for response)
        // Z: 000		(Reserved for future use)
        // Rcode: 0000	(Response code - reserved for response)

        this.requestPacket.put( (byte) 0x01 );
        this.requestPacket.put( (byte) 0x00 );

        // putting the QDCOUNT
        this.requestPacket.put( (byte) 0x00 );
        this.requestPacket.put( (byte) 0x01 );

        // putting the ANCOUNT
        this.requestPacket.put( (byte) 0x00 );
        this.requestPacket.put( (byte) 0x00 );

        // putting the NSCOUNT
        this.requestPacket.put((byte) 0x00);
        this.requestPacket.put((byte) 0x00);

        // putting the ARCOUNT
        this.requestPacket.put((byte) 0x00);
        this.requestPacket.put((byte) 0x00);
    }

    // Populates the question section
    private void populateQuestion() {

        // Splitting the domain name into it's labels
        String[] tmp = this.domain.split("\\.");
        for (int i = 0; i < tmp.length; i++) {
            // Put's the length of each label in first then followed by
            this.requestPacket.put((byte) tmp[i].length());
            for (int j = 0; j < tmp[i].length(); j++) {
                this.requestPacket.put( (byte) (tmp[i].charAt(j)) );
            }
        }

        // Now putting the '0' byte to the end of the bytebuffer to signal the end of the domain name
        this.requestPacket.put((byte) 0x00);

        // Now have to add in Query type byte portion
        this.requestPacket.put(hexToByteArray(this.typeOfRequest));

        // Adding in the QCLASS (Internet Address)
        this.requestPacket.put((byte) 0x00);
        this.requestPacket.put((byte) 0x01);
    }

    // Converts type of query request into byte version of hex representations
    // QTYPE: 0x0001	(A Query)
    // QTYPE: 0x0002	(NS Query)
    // QTYPE: 0x000f	(MX Query)
    private byte hexToByteArray(Query typeOfRequest) {
        if (typeOfRequest == Query.A) {
            return (byte) 0x01;
        } else if (typeOfRequest == Query.NS) {
            return (byte) 0x02;
        } else {
            return (byte) 0x0f;
        }
    }

    // www = label, mcgill = label, ca = label
    // 1 byte (1 octet) for length of label + a byte for each char of the label following the length
    // www.mcgill.ca = 3 w w w 6 m c g i l l 2 c a = 14 bytes in total
    // Calculates the QName byte length

    private int findQNameByteLength() {

        int byteLength = 0;
        String domainName = getDomain();
        String[] splitDomainName = domainName.split("\\.");
        for (int i = 0; i < splitDomainName.length; i++) {
            byteLength += splitDomainName[i].length() + 1;
        }
        // To add one more byte at the end to signal the end of a domain name
        byteLength += 1;
        return byteLength;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Query getTypeOfRequest() {
        return typeOfRequest;
    }

    public void setTypeOfRequest(Query typeOfRequest) {
        this.typeOfRequest = typeOfRequest;
    }
}
