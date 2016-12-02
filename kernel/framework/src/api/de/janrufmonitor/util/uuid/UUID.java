package de.janrufmonitor.util.uuid;

import java.net.InetAddress;
import java.util.Date;
import java.util.Random;
import java.io.*;
import java.security.MessageDigest;

import de.janrufmonitor.util.io.PathResolver;

public class UUID implements Serializable, Comparable {

    final static long serialVersionUID = -844803884714259801L;

    private static byte[] internetAddress = null;
    private static String uuidFile = null;
    private long time;
    private short clockSequence;
    private byte version = 1;
    private byte[] node = new byte[6];

    private byte[] m_uuid;

    private final static int UUIDsPerTick = 128;
    private static long lastTime = new Date().getTime();
    private static int uuidsThisTick = UUIDsPerTick;
    private static UUID previousUUID = null;
    private static long nextSave = new Date().getTime();
    private static Random randomGenerator = new Random(new Date().getTime());
    private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	private static ObjectInputStream s;

    static {
        try {
            internetAddress = InetAddress.getLocalHost().getAddress();
        } catch (Exception exc) {
            //System.out.println("UUID: Can't get host address: " + exc);
            //exc.printStackTrace();
        }
        try {

            //uuidFile = System.getProperty("UUID_HOME");
        	uuidFile = PathResolver.getInstance().getInstallDirectory();
            if (uuidFile == null) {
                uuidFile = System.getProperty("java.home");
            }
            if (!uuidFile.endsWith(File.separator)) {
                uuidFile = uuidFile + File.separator;
            }
            uuidFile = uuidFile + "/UUID";
            previousUUID = getUUIDState();
        } catch (Exception exc) {
            //exc.printStackTrace();
        }
    }

    public UUID() {
        synchronized (UUID.class) {
            time = getCurrentTime();
            node = previousUUID.getNode();
            if (previousUUID == null || nodeChanged(previousUUID)) {
                clockSequence = (short) random();
            } else if (time < previousUUID.getTime()) {
                clockSequence++;
            }
            previousUUID = this;
            setUUIDState(this);
        }
        m_uuid = genUUID();
    }


    public UUID(String str) throws NumberFormatException {
        int i;

        m_uuid = parseUUID(str);

        time = m_uuid[7] & 0xf;
        for (i = 1; i < 8; i++) {
            time <<= 8;
            time |= m_uuid[7 - i];
        }
        version = (byte) (m_uuid[7] >> 4);
        clockSequence = (short) (((m_uuid[9] & 0x3F) << 8) | m_uuid[8]);
        for (i = 0; i < 6; i++) {
            node[i] = m_uuid[10 + i];
        }
    }

    public UUID(byte[] node) {
        synchronized (UUID.class) {
            time = getCurrentTime();
            this.node = node;
            setUUIDState(this);
        }
        m_uuid = genUUID();
    }


    public int compareTo(Object withUUID) {
        if (!(withUUID instanceof UUID)) {
            return -1;
        }

        byte[] other = ((UUID) withUUID).getUUID();
        int i;

        for (i = 0; i < 16; i++) {
            if (m_uuid[i] < other[i]) {
                return -1;
            } else if (m_uuid[i] > other[i]) {
                return 1;
            }
        }
        return 0;
    }


    /**
     *  Get a 48 bit cryptographic quality random number to use as the node
     *  field of a UUID as specified in section 6.4.1 of version 10 of the
     *  WebDAV spec. This is an alternative to the IEEE 802 host address which
     *  is not available from Java. The number will not conflict with any IEEE
     *  802 host address because the most significant bit of the first octet is
     *  set to 1.
     *
     *@return    a 48 bit number specifying an id for this node
     */
    private static byte[] computeNodeAddress() {
        byte[] address = new byte[6];
        // create a random number by concatenating:
        //    the hash code for the current thread
        //    the current time in milli-seconds
        //    the internet address for this node
        int thread = Thread.currentThread().hashCode();
        long time = System.currentTimeMillis();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            if (internetAddress != null) {
                out.write(internetAddress);
            }
            out.write(thread);
            out.writeLong(time);
            out.close();
        } catch (IOException exc) {
        }
        byte[] rand = byteOut.toByteArray();
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception exc) {
        }
        md5.update(rand);
        byte[] temp = md5.digest();
        // pick the middle 6 bytes of the MD5 digest
        for (int i = 0; i < 6; i++) {
            address[i] = temp[i + 5];
        }

        address[0] = (byte) (address[0] | (byte) 0x80);
        return address;
    }


    /**
     *  Get the clock sequence number.
     *
     *@return    the clock sequence number
     */
    public int getClockSequence() {
        return clockSequence;
    }


    /**
     *  Get the current time compensating for the fact that the real clock
     *  resolution may be less than 100ns.
     *
     *@return    the current date and time
     */
    private static long getCurrentTime() {
        long now = 0;
        boolean waitForTick = true;
        while (waitForTick) {
            now = new Date().getTime();
            if (lastTime < now) {
                uuidsThisTick = 0;
                waitForTick = false;
            } else if (uuidsThisTick < UUIDsPerTick) {
                //
                uuidsThisTick++;
                waitForTick = false;
            }
        }

        now += uuidsThisTick;
        lastTime = now;
        return now;
    }


    /**
     *  Get the spatially unique portion of the UUID. This is either the 48 bit
     *  IEEE 802 host address, or if on is not available, a random number that
     *  will not conflict with any IEEE 802 host address.
     *
     *@return    node portion of the UUID
     */
    public byte[] getNode() {
        return node;
    }


    /**
     *  Get the temporal unique portion of the UUID.
     *
     *@return    the time portion of the UUID
     */
    public long getTime() {
        return time;
    }


    /**
     *  Get the 128 bit UUID.
     *
     *@return    The uUID value
     */
    public byte[] getUUID() {
        return m_uuid;
    }


    /**
     *  Generate the 128 bit UUID.
     *
     *@return    Description of the Return Value
     */
    private byte[] genUUID() {
        byte[] uuid = new byte[16];
        long t = time;
        for (int i = 0; i < 8; i++) {
            uuid[i] = (byte) ((t >> 8 * i) & 0xFF);
        }
        uuid[7] |= (byte) (version << 4);
        uuid[8] = (byte) (clockSequence & 0xFF);
        uuid[9] = (byte) ((clockSequence & 0x3F00) >> 8);
        uuid[9] |= 0x80;
        // clock sequence hi and reserved
        for (int i = 0; i < 6; i++) {
            uuid[10 + i] = node[i];
            // node
        }
        return uuid;
    }


    /**
     *  Get the UUID generator state. This consists of the last (or nearly last)
     *  UUID generated. This state is used in the construction of the next UUID.
     *  May return null if the UUID state is not available.
     *
     *@return    the last UUID generator state
     */
    private static UUID getUUIDState() {
        UUID uuid = null;
        try {
            FileInputStream in = new FileInputStream(uuidFile);
            s = new ObjectInputStream(in);
            uuid = (UUID) s.readObject();
        } catch (Exception exc) {
            uuid = new UUID(computeNodeAddress());
            //System.err.println("UUID: Can't get saved UUID state: " + exc);
        }
        return uuid;
    }


    /**
     *  Get the UUID version number.
     *
     *@return    The version value
     */
    public int getVersion() {
        return version;
    }


    /**
     *  Compare two UUIDs
     *
     *@param  toUUID  Description of the Parameter
     *@return         true if the UUIDs are equal
     */
    public boolean equals(Object toUUID) {
        return compareTo(toUUID) == 0;
    }


    /**
     *  Determine if the node changed with respect to previousUUID.
     *
     *@param  previousUUID  the UUID to compare with
     *@return               true if the the previousUUID has a different node
     *      than this UUID
     */
    private boolean nodeChanged(UUID previousUUID) {
        byte[] previousNode = previousUUID.getNode();
        boolean nodeChanged = false;
        int i = 0;
        while (!nodeChanged && i < 6) {
            nodeChanged = node[i] != previousNode[i];
            i++;
        }
        return nodeChanged;
    }


    /**
     *  Generate a crypto-quality random number. This implementation doesn't do
     *  that.
     *
     *@return    a random number
     */
    private static int random() {
        return randomGenerator.nextInt();
    }


    /**
     *  Set the persistent UUID state.
     *
     *@param  aUUID  the UUID to save
     */
    private static void setUUIDState(UUID aUUID) {
        if (aUUID.getTime() > nextSave) {
            try {
                FileOutputStream f = new FileOutputStream(uuidFile);
                ObjectOutputStream s = new ObjectOutputStream(f);
                s.writeObject(aUUID);
                s.close();
                nextSave = aUUID.getTime() + 10 * 10 * 1000 * 1000;
            } catch (Exception exc) {
                //System.err.println("UUID: Can't save UUID state: " + exc);
            }
        }
    }


    /**
     *  Provide a String representation of a UUID as specified in section 3.5 of
     *  [leach].
     *
     *@return    Description of the Return Value
     */
    public String toString() {
        byte[] uuid = getUUID();
        StringWriter s = new StringWriter();
        for (int i = 0; i < 16; i++) {
            s.write(hexDigits[(uuid[i] & 0xF0) >> 4]);
            s.write(hexDigits[uuid[i] & 0x0F]);
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                s.write('-');
            }
        }
        return s.toString();
    }


    /**
     *  Parse a string represention of the uuid.
     *
     *@param  str                     The string representation
     *@return                         The binary representation of the uuid
     *@throws  NumberFormatException  if the string representation is invalid
     */
    private byte[] parseUUID(String str)
             throws NumberFormatException {
        char ch;
        int i;
        StringBuffer buf = new StringBuffer(32);

        byte[] uuid = new byte[16];

        for (i = 0; i < str.length(); i++) {
            ch = str.charAt(i);

            if ((ch >= '0') && (ch <= '9')) {
                buf.append(ch);
            } else if ((ch >= 'A') && (ch <= 'F')) {
                buf.append(ch);
            } else if ((ch >= 'a') && (ch <= 'f')) {
                buf.append(ch);
            } else if ((ch != ' ') && (ch != '-')) {
                throw new NumberFormatException("Invalid UUID string");
            }
        }

        if (buf.length() != 32) {
            throw new NumberFormatException("Invalid UUID string");
        }

        for (i = 0; i < 16; i++) {
            uuid[i] = (byte) ((Character.digit(buf.charAt(2 * i), 16) << 4) |
                    Character.digit(buf.charAt(2 * i + 1), 16));
        }

        return uuid;
    }


    /**
     *  Generate a hash code of the uuid.
     *
     *@return    Description of the Return Value
     */
    public int hashCode() {
        int hash = 0;
        int i;

        for (i = 0; i < 16; i++) {
            hash ^= m_uuid[i];
            hash <<= 2;
        }

        return hash;
    }

}
