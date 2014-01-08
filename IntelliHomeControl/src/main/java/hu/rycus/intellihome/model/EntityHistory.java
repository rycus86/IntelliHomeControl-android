package hu.rycus.intellihome.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class representing an entry in the history.
 *
 * Created by Viktor Adam on 12/27/13.
 */
public class EntityHistory implements Parcelable {

    /** The timestamp of the history entry. */
    private final long timestamp;
    /** The identifier of the entity related to the history entry. */
    private final String entityId;
    /** The name of the entity related to the history entry. */
    private final String entityName;
    /** The action related to the history entry as human-readable string. */
    private final String action;
    /** The type of the action related to the history entry. */
    private final String actionType;

    /**
     * Private constructor.
     * @param timestamp The timestamp of the history entry
     * @param entityId The identifier of the entity related to the history entry
     * @param entityName The name of the entity related to the history entry
     * @param action The action related to the history entry as human-readable string
     * @param actionType The type of the action related to the history entry
     */
    private EntityHistory(long timestamp, String entityId, String entityName, String action, String actionType) {
        this.timestamp = timestamp;
        this.entityId = entityId;
        this.entityName = entityName;
        this.action = action;
        this.actionType = actionType;
    }

    /** Retruns the timestamp of the history entry. */
    public long getTimestamp() { return timestamp; }
    /** Returns the identifier of the entity related to the history entry.*/
    public String getEntityId() { return entityId; }
    /** Returns the name of the entity related to the history entry.*/
    public String getEntityName() { return entityName; }
    /** Returns the action related to the history entry as human-readable string.*/
    public String getAction() { return action; }
    /** Returns the type of the action related to the history entry.*/
    public String getActionType() { return actionType; }

    /**
     * Deserializes the parameter values of a history entry with
     * data received from the server and instantiates one.
     * @param data The string data received from the server
     * @param offset The offset in the string holding the parameter values of the history entry
     * @param output An array used to store the output history entry
     * @return The number of characters used to process the values
     */
    public static int deserialize(String data, int offset, EntityHistory[] output) {
        long timestamp = -1;
        String eId = null;
        String eName = null;
        String action = null;
        String aType = null;

        StringBuilder builder = new StringBuilder();

        int read = 0;
        int var = 0;

        while(offset < data.length()) {
            char c = data.charAt(offset++);
            read++;

            if(c == '#') {
                break;
            } else if(c == ';') {
                if(var == 0) {
                    timestamp = Math.round(Double.parseDouble(builder.toString())) * 1000L;
                    builder.setLength(0);
                } else if(var == 1) {
                    eId = builder.toString();
                    builder.setLength(0);
                } else if(var == 2) {
                    eName = builder.toString();
                    builder.setLength(0);
                } else if(var == 3) {
                    action = builder.toString();
                    builder.setLength(0);
                }
                var++;
            } else {
                builder.append(c);
            }
        }

        if(read > 0 && var == 4) {
            aType = builder.toString();
        }

        if(read > 0) {
            output[0] = new EntityHistory(timestamp, eId, eName, action, aType);
        }

        return read;
    }

    /**
     * Unused method implementation since
     * local serialization is done with reflection.
     * @see android.os.Parcelable#describeContents()
     */
    @Override public int describeContents() { return 0; }
    /**
     * Unused method implementation since
     * local serialization is done with reflection.
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override public void writeToParcel(Parcel dest, int flags) { }

    /**
     * Parcelable.Creator object for the data class.
     * Unused class implementation since
     * local serialization is done with reflection.
     */
    public static final Creator<EntityHistory> CREATOR = new Creator<EntityHistory>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public EntityHistory createFromParcel(Parcel parcel) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public EntityHistory[] newArray(int i) { return new EntityHistory[0]; }
    };

}
