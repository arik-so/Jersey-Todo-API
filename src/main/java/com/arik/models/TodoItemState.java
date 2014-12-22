package com.arik.models;

/**
 * Created by arik-so on 12/22/14.
 */
public class TodoItemState {

    public enum DoneState {

        DONE(true, true, "done"),
        NOT_DONE(true, false, "not done"),
        UNCHANGED(false, false, null);

        /**
         * True if this state alters the boolean value of the done flag
         */
        private final boolean isModifier;

        /**
         * Target boolean value if state is a modifier
         */
        private final boolean isDone;

        /**
         * Plain English state representation
         */
        private final String stateMessage;

        private DoneState(final boolean isModifier, final boolean isDone, final String statusMessage) {

            this.isModifier = isModifier;
            this.isDone = isDone;
            this.stateMessage = statusMessage;

        }

        /**
         * *
         *
         * @param isDoneString
         * @return
         */
        public static DoneState fromString(final String isDoneString) {

            if (isDoneString.equalsIgnoreCase("true") || isDoneString.equalsIgnoreCase("1")) {
                return DoneState.DONE;
            } else if (isDoneString.equalsIgnoreCase("false") || isDoneString.equalsIgnoreCase("0")) {
                return DoneState.NOT_DONE;
            }

            return DoneState.UNCHANGED;

        }

        public boolean isModifier() {
            return isModifier;
        }

        public String getStateMessage() {
            return stateMessage;
        }

        public boolean isDone() {
            return isDone;
        }
    }

}
