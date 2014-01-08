package hu.rycus.intellihome.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class representing the state of an entity.
 *
 * Created by Viktor Adam on 12/18/13.
 */
public class EntityState implements Parcelable {

    /** The identifier of the state. */
    private final int id;
    /** The name of the state. */
    private final String name;

    /**
     * Private constructor.
     * @param id The identifier of the state
     * @param name The name of the state
     */
    private EntityState(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Returns the identifier of the state.*/
    public int getId() { return id; }
    /** Returns the name of the state.*/
    public String getName() { return name; }

    /**
     * Deserializes the parameter values of a state object with
     * data received from the server and instantiates one.
     * @param data The string data received from the server
     * @param offset The offset in the string holding the parameter values of the state object
     * @param output An array used to store the output state object
     * @return The number of characters used to process the values
     */
    public static int deserialize(String data, int offset, EntityState[] output) {
        int id = -1;
        String name = null;

        StringBuilder builder = new StringBuilder();

        int read = 0;
        int var = 0;

        while(offset < data.length()) {
            char c = data.charAt(offset++);
            read++;

            if(c == ';') {
                if(var == 0) {
                    id = Integer.parseInt(builder.toString());
                    builder.setLength(0);
                    var++;
                } else if(var == 1) {
                    name = builder.toString();
                    break;
                }
            } else {
                builder.append(c);
            }
        }

        if(read > 0) {
            output[0] = new EntityState(id, name);
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
    public static final Creator<EntityState> CREATOR = new Creator<EntityState>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public EntityState createFromParcel(Parcel source) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public EntityState[] newArray(int size) { return new EntityState[0]; }
    };

}
