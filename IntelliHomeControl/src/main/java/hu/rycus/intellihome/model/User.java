package hu.rycus.intellihome.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class representing a user of the application.
 *
 * Created by Viktor Adam on 12/28/13.
 */
public class User implements Parcelable {

    /** The identifier of the user. */
    private final int id;
    /** The username of the user. */
    private final String username;
    /** Flag used to indicate whether the user is the administrator. */
    private final boolean administrator;

    /**
     * Public constructor.
     * @param id The identifier of the user
     * @param username The username of the user
     * @param administrator Flag used to indicate whether the user is the administrator
     */
    public User(int id, String username, boolean administrator) {
        this.id = id;
        this.username = username;
        this.administrator = administrator;
    }

    /** Returns the identifier of the user. */
    public int getId() { return id; }
    /** Returns the username of the user. */
    public String getUsername() { return username; }
    /** Returns the flag used to indicate whether the user is the administrator. */
    public boolean isAdministrator() { return administrator; }

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
    public static final Creator<User> CREATOR = new Creator<User>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public User createFromParcel(Parcel parcel) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public User[] newArray(int i) { return new User[0]; }
    };

}
