package redstonelamp.utils;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Network Utility class for triads, etc.
 */
public class NetworkUtils {

    public static int readLTriad(ByteBuffer bb) {
        return (int) (bb.get() << 16 | bb.get() << 8 | bb.get());
    }

    public static int readLTriad(byte[] triad){
        return triad[0]
                + (triad[1] << 8)
                + (triad[2] << 16);
    }

    public static int readLTriad(byte[] data, int offset){
        return (data[offset] & 0xff) | (data[offset+1] & 0xff) << 8 | (data[offset+2] & 0xff) << 16;
    }

    public static void writeLTriad(int triad, ByteBuffer bb){
        byte[] buffer = new byte[3];
        int shift = (3 - 1) * 8;
        for(int i = 0; i < 3; i++){
            buffer[true ? (3 - i - 1):i] = (byte) (triad >> shift);
            shift -= 8;
        }
        bb.put(buffer);
    }

    public static byte[] writeLTriad(int triad){
        byte[] buffer = new byte[3];
        int shift = (3 - 1) * 8;
        for(int i = 0; i < 3; i++){
            buffer[true ? (3 - i - 1):i] = (byte) (triad >> shift);
            shift -= 8;
        }
        return buffer;
    }

    public static int getPort(SocketAddress address){
        String[] a = address.toString().replaceAll("/", "").split(":");
        return Integer.parseInt(a[1]);
    }

}
