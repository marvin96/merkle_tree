package de.marvin.merkletree;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class Proof {
    private byte[] rootHash;
    private ArrayList<byte[]> proofSet;
    private int leafIndex;
    private int leafSize;
    private String hashAlg;

    public Proof(byte[] rootHash, ArrayList<byte[]> proofSet, int leafIndex, int leafSize, String hashAlg){
        this.rootHash = rootHash;
        this.proofSet = proofSet;
        this.leafIndex = leafIndex;
        this.leafSize = leafSize;
        this.hashAlg = hashAlg;
    }

    /**
     * hash length | proofSet Size | leafIndex | leafSize | rootHash | proofSet | hashAlgorithm
     * @return
     */
    public byte[] asBytes(){
        ByteBuffer bb = ByteBuffer.allocate((proofSet.size() + 1) * rootHash.length + 4 * Integer.BYTES + hashAlg.length());
        bb.putInt(rootHash.length);
        bb.putInt(proofSet.size());
        bb.putInt(leafIndex);
        bb.putInt(leafSize);
        bb.put(rootHash);
        for(byte[] hash: proofSet){
            bb.put(hash);
        }
        bb.put(hashAlg.getBytes(StandardCharsets.UTF_8));
        return bb.array();
    }

    @Override
    public String toString() {
        return "Proof{" +
                "rootHash=" + Arrays.toString(rootHash) +
                ", proofSet=" + proofSet +
                ", leafIndex=" + leafIndex +
                ", leafSize=" + leafSize +
                ", hashAlg='" + hashAlg + '\'' +
                '}';
    }

    public static Proof getFromBytes(byte[] proof){
        ByteBuffer bb = ByteBuffer.wrap(proof);
        int hashLength = bb.getInt();
        int proofSetSize = bb.getInt();
        int leafIndex = bb.getInt();
        int leafSize = bb.getInt();
        int position = bb.position();
        byte[] rootHash = Arrays.copyOfRange(proof, position, position+hashLength);

        ArrayList<byte[]> proofSet = new ArrayList<>();
        for(int i = 0; i < proofSetSize; i++){
            position += hashLength;
            proofSet.add(Arrays.copyOfRange(proof, position, position+hashLength));
        }
        position += hashLength;
        String hahsAlg = new String(Arrays.copyOfRange(proof, position, proof.length), StandardCharsets.UTF_8);

        return new Proof(rootHash, proofSet, leafIndex, leafSize, hahsAlg);
    }

    public static boolean verify(byte[] data, Proof proof) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(proof.hashAlg);
        byte[] currentHash = md.digest(data);
        byte[] combinedChildHashes = new byte[2*md.getDigestLength()];
        int index = proof.leafIndex;
        int leafSize = proof.leafSize;
        for(byte[] hash: proof.proofSet){
            if(index == leafSize - 1 || index % 2 == 1){
                System.arraycopy(hash, 0, combinedChildHashes, 0, hash.length);
                System.arraycopy(currentHash, 0, combinedChildHashes, md.getDigestLength(), currentHash.length);
            } else {
                System.arraycopy(currentHash, 0, combinedChildHashes, 0, currentHash.length);
                System.arraycopy(hash, 0, combinedChildHashes, md.getDigestLength(), hash.length);
            }
            currentHash = md.digest(combinedChildHashes);
            leafSize = leafSize/2 + leafSize % 2;
            index = index / 2;
        }
        return MessageDigest.isEqual(proof.rootHash, currentHash);
    }
}
