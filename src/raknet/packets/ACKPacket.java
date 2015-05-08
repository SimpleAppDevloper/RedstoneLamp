package raknet.packets;

import redstonelamp.utils.NetworkUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ACK Packet
 */
public class ACKPacket {
    public byte PID = (byte) 0xc0;
    public int[] seqNums;

    public byte[] buf;

    public void encode(){
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.position(3);
        Arrays.sort(seqNums);
        int count = seqNums.length;
        int records = 0;

        if(count > 0){
            int pointer = 1;
            int start = seqNums[0];
            int last = seqNums[0];
            while(pointer < count){
                int current = seqNums[pointer++];
                int diff = current - last;
                if(diff == 1){
                    last = current;
                }else if(diff > 1){ //Forget about duplicated packets (bad queues?)
                    if(start == last){
                        bb.put((byte) 0x01);
                        bb.put(NetworkUtils.writeLTriad(start));
                        start = last = current;
                    }else{
                        bb.put((byte) 0x00);
                        bb.put(NetworkUtils.writeLTriad(start));
                        bb.put(NetworkUtils.writeLTriad(last));
                        start = last = current;
                    }
                    ++records;
                }
            }
            if(start == last){
                bb.put((byte) 0x01);
                bb.put(NetworkUtils.writeLTriad(start));
            }else{
                bb.put((byte) 0x00);
                bb.put(NetworkUtils.writeLTriad(start));
                bb.put(NetworkUtils.writeLTriad(last));
            }
            ++records;
        }
        int length = bb.position();
        bb.position(0);
        bb.put(PID);
        bb.putShort((short) records);
        buf = Arrays.copyOf(bb.array(), length);
    }

    public void decode(){
        ByteBuffer bb = ByteBuffer.wrap(buf, 1, buf.length - 1);
        int count = bb.getShort();
        List<Integer> packets = new ArrayList<>();
        for(int i = 0; i < count && bb.position() < bb.capacity(); ++i){
            byte[] tmp = new byte[6];
            if(bb.get() == 0x00){
                bb.get(tmp);
                int start = NetworkUtils.readLTriad(tmp, 0);
                int end = NetworkUtils.readLTriad(tmp, 3);
                if((end - start) > 4096){
                    end = start + 4096;
                }
                for(int c = start; c <= end; ++c){
                    packets.add(c);
                }
            }else{
                bb.get(tmp, 0, 3);
                packets.add(NetworkUtils.readLTriad(tmp));
            }
        }

        seqNums = new int[packets.size()];
        for(int i = 0; i < seqNums.length; i++){
            seqNums[i] = packets.get(i);
        }
    }
}
