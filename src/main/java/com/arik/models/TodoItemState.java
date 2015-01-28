package com.arik.models;

/**
 * Class for representing various done states
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
         * Parse a human-created boolean descriptor string into a proper state
         *
         * @param isDoneString The string that is to be parsed and converted into a done state
         * @return The resulting done state
         */
        public static DoneState fromString(final String isDoneString) {

            if(isDoneString == null){
                return DoneState.UNCHANGED;
            }

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
