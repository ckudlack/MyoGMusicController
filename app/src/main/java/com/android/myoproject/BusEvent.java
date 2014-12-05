package com.android.myoproject;

import com.thalmic.myo.Arm;
import com.thalmic.myo.Pose;

public class BusEvent {

    public BusEvent() {
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

    public static class MyoConnectionStatusEvent extends BusEvent {
        private boolean isConnected;

        public MyoConnectionStatusEvent(boolean isConnected) {
            this.isConnected = isConnected;
        }

        public boolean isConnected() {
            return isConnected;
        }
    }

    public static class MyoSyncStatusEvent extends BusEvent {
        private boolean isSynced;

        public MyoSyncStatusEvent(boolean isSynced) {
            this.isSynced = isSynced;
        }

        public boolean isSynced() {
            return isSynced;
        }
    }

    public static class UserNeedsPermissionEvent extends BusEvent {
        public UserNeedsPermissionEvent() {
        }
    }

    public static class RestartControllerEvent extends BusEvent {
        public RestartControllerEvent() {
        }
    }
}
