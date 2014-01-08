package hu.rycus.intellihome.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class representing an entity.
 *
 * Created by Viktor Adam on 12/18/13.
 */
public class Entity implements Parcelable {

    /** The unique identifier. */
    private final String id;
    /** The type of the entity. */
    private final EntityType type;
    /** The name of the entity. */
    private final String name;
    /** The current state of the entity. */
    private final EntityState state;
    /** A parameter value related to the state of the entity. */
    private final String stateValue;
    /** The timestamp of the last check-in of the entity. */
    private final long lastCheckin;

    /**
     * Private constructor.
     * @param id The unique identifier
     * @param type The type of the entity
     * @param name The name of the entity
     * @param state The current state of the entity
     * @param stateValue A parameter value related to the state of the entity
     * @param lastCheckin The timestamp of the last check-in of the entity
     */
    private Entity(String id, EntityType type, String name, EntityState state, String stateValue, long lastCheckin) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.state = state;
        this.stateValue = stateValue;
        this.lastCheckin = lastCheckin;
    }

    /** Returns the unique identifier of the entity. */
    public String getId() { return id; }
    /** Returns the type of the entity. */
    public EntityType getType() { return type; }
    /** Returns the name of the entity. */
    public String getName() { return name; }
    /** Returns the current state of the entity. */
    public EntityState getState() { return state; }
    /** Returns a parameter value related to the state of the entity. */
    public String getStateValue() { return stateValue; }
    /** Returns the timestamp of the last check-in of the entity. */
    public long getLastCheckin() { return lastCheckin; }

    /**
     * Deserializes the parameter values of an entity with
     * data received from the server and instantiates one.
     * @param data The string data received from the server
     * @param offset The offset in the string holding the parameter values of the entity
     * @param output An array used to store the output entity
     * @return The number of characters used to process the values
     */
    public static int deserialize(String data, int offset, Entity[] output) {
        String id = null;
        EntityType type = null;
        String name = null;
        EntityState state = null;
        String stateValue = null;
        long lastCheckin = 0L;

        StringBuilder builder = new StringBuilder();

        int var = 0;

        int read = 0;
        while(offset < data.length()) {
            char c = data.charAt(offset++);
            read++;

            if(c == ',' && var == 5) {
                break;
            } else if(c == ';') {
                if(var == 0) {
                    id = builder.toString();
                    builder.setLength(0);
                    var++;
                } else if(var == 1) {
                    int eType = Integer.parseInt(builder.toString());
                    type = EntityType.get(eType);
                    builder.setLength(0);
                    var ++;
                } else if(var == 2) {
                    name = builder.toString();
                    builder.setLength(0);
                    var++;

                    // if (var == 3):
                    EntityState[] sOutput = new EntityState[1];
                    int rs = EntityState.deserialize(data, offset, sOutput);

                    state = sOutput[0];
                    read += rs;
                    offset += rs;

                    var++;
                } else if(var == 4) {
                    if(builder.length() > 0) {
                        stateValue = builder.toString();
                    }
                    builder.setLength(0);
                    var++;
                }
            } else {
                builder.append(String.valueOf(c));
            }
        }

        lastCheckin = (long) (Double.parseDouble(builder.toString()) * 1000L);

        output[0] = new Entity(id, type, name, state, stateValue, lastCheckin);

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
    public static final Creator<Entity> CREATOR = new Creator<Entity>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public Entity createFromParcel(Parcel source) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public Entity[] newArray(int size) { return new Entity[0]; }
    };

}
