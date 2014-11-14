package com.android.myoproject;

public class BusEvent {

    public BusEvent() {
    }

    public static class PlaybackUpdatedEvent extends BusEvent {
        private boolean isPlaying;

        public PlaybackUpdatedEvent(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }

        public boolean isPlaying() {
            return isPlaying;
        }
    }

    public static class DestroyServiceEvent extends BusEvent {
        public DestroyServiceEvent() {
        }
    }
}
