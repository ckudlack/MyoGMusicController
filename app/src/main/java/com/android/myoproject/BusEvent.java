package com.android.myoproject;

import com.thalmic.myo.Arm;
import com.thalmic.myo.Pose;

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

    public static class GestureUpdatedEvent extends BusEvent {
        private Pose pose;
        private Arm arm;

        public GestureUpdatedEvent(Pose pose, Arm arm) {
            this.pose = pose;
            this.arm = arm;
        }

        public Pose getPose() {
            return pose;
        }

        public Arm getArm() {
            return arm;
        }
    }
}
