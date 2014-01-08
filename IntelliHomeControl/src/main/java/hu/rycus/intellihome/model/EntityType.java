package hu.rycus.intellihome.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;

/**
 * Data class representing an entity type.
 *
 * Created by Viktor Adam on 12/18/13.
 */
public class EntityType implements Parcelable {

    /** Local lookup cache used to store the entity types. */
    private static final Map<Integer, EntityType> cache = new LinkedHashMap<>();

    /** The identifier of the type. */
    private final int id;
    /** The name of the type. */
    private final String name;
    /** The HTML ARGB color code of the type. */
    private final String colorCode;
    /** The filename of the image related to the type. */
    private final String imageFilename;
    /** The array of commands available for the type. */
    private final EntityCommand[] commands;

    /** The image of the type as Drawable. */
    private Drawable image;

    /**
     * Private constructor.
     * @param id The identifier of the type
     * @param name The name of the type
     * @param colorCode The HTML ARGB color code of the type
     * @param imageFilename The filename of the image related to the type
     * @param commands The array of commands available for the type
     */
    private EntityType(int id, String name, String colorCode, String imageFilename, EntityCommand[] commands) {
        this.id = id;
        this.name = name;
        this.colorCode = colorCode;
        this.imageFilename = imageFilename;
        this.commands = commands;
    }

    /** Returns the identifier of the type. */
    public int getId() { return id; }
    /** Returns the name of the type. */
    public String getName() { return name; }
    /** Returns the HTML ARGB color code of the type. */
    public String getColorCode() { return colorCode; }
    /** Returns the filename of the image related to the type. */
    public String getImageFilename() { return imageFilename; }
    /** Returns the array of commands available for the type. */
    public EntityCommand[] getCommands() { return commands; }

    /** Returns true if the image of the type is loaded. */
    public boolean isImageSet() { return image != null; }
    /** Returns the image of the type or the default image if it is not set yet. */
    public Drawable getImage(Resources resources) {
        if(image != null) {
            return image;
        } else {
            return resources.getDrawable(R.drawable.ic_unknown);
        }
    }
    /** Sets the actual image of the type. */
    public void setImage(Drawable image) { this.image = image; }

    /** Returns the registered instance of the type with the given identifier. */
    public static EntityType get(int id) { return cache.get(id); }
    /** Returns the collection of registered types. */
    public static Collection<EntityType> list() { return cache.values(); }

    /**
     * Deserializes the parameter values of an entity type with
     * data received from the server and instantiates one
     * then registers it in the local lookup cache.
     * @param data The string data received from the server
     * @param offset The offset in the string holding the parameter values of the entity type
     * @return The number of characters used to process the values
     */
    public static int deserialize(String data, int offset) {
        int id = 0;
        String name = null;
        String color = null;
        String image = null;
        List<EntityCommand> commands = new LinkedList<>();

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
                } else if(var == 1) {
                    name = builder.toString();
                    builder.setLength(0);
                } else if(var == 2) {
                    color = builder.toString();
                    if(color.trim().isEmpty()) {
                        color = null;
                    }
                    builder.setLength(0);
                } else if(var == 3) {
                    image = builder.toString();
                    if(image.trim().isEmpty()) {
                        image = null;
                    }
                    builder.setLength(0);
                }
                var++;
            } else if(var == 4) {
                // if(c == '[' || c == ',') continue;
                if(c == ']') {
                    read++;
                    offset++;
                    break;
                }

                EntityCommand[] cOutput = new EntityCommand[1];
                int rc = EntityCommand.deserialize(data, offset, cOutput);
                if(rc > 0) {
                    commands.add(cOutput[0]);
                    read += rc;
                    offset += rc;
                }
            } else {
                builder.append(c);
            }
        }

        EntityType type = new EntityType(id, name, color, image, commands.toArray(new EntityCommand[0]));
        cache.put(id, type);

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
    public static final Creator<EntityType> CREATOR = new Creator<EntityType>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public EntityType createFromParcel(Parcel source) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public EntityType[] newArray(int size) { return new EntityType[0]; }
    };

}
