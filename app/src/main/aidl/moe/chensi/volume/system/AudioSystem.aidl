package moe.chensi.volume.system;

interface AudioSystem {
    boolean isStreamActive(int streamType, int inPastMs);
}
