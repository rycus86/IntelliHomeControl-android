package hu.rycus.intellihome.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import hu.rycus.intellihome.R;

/**
 * Data class representing a command that
 * can be sent to an entity (alias device).
 *
 * Created by Viktor Adam on 12/18/13.
 */
public class EntityCommand implements Parcelable {

    /** The identifier of the command. */
    private final int id;
    /** The name of the command. */
    private final String name;
    /** Type of the optional parameter for the command. */
    private final String parameterType;

    /**
     * Private constructor.
     * @param id The identifier of the command
     * @param name The name of the command
     * @param parameterType Type of the optional parameter for the command
     */
    private EntityCommand(int id, String name, String parameterType) {
        this.id = id;
        this.name = name;
        this.parameterType = parameterType;
    }

    /** Returns the identifier of the command. */
    public int getId() { return id; }
    /** Returns the name of the command. */
    public String getName() { return name; }
    /** Returns the type of the optional parameter for the command. */
    public String getParameterType() { return parameterType; }

    /**
     * Deserializes the parameter values of a command with
     * data received from the server and instantiates one.
     * @param data The string data received from the server
     * @param offset The offset in the string holding the parameter values of the command
     * @param output An array used to store the output command
     * @return The number of characters used to process the values
     */
    public static int deserialize(String data, int offset, EntityCommand[] output) {
        int id = -1;
        String name = null;
        String pType = null;

        StringBuilder builder = new StringBuilder();

        int read = 0;
        int var = 0;

        while(offset < data.length()) {
            char c = data.charAt(offset++);
            read++;

            if(var == 2 && (c == ',' || c == ']')) {
                pType = builder.toString();
                if(pType.trim().isEmpty()) {
                    pType = null;
                }

                read--;
                break;
            } else if(c == ';') {
                if(var == 0) {
                    id = Integer.parseInt(builder.toString());
                    builder.setLength(0);
                } else if(var == 1) {
                    name = builder.toString();
                    builder.setLength(0);
                }
                var++;
            } else {
                builder.append(c);
            }
        }

        if(read > 0) {
            output[0] = new EntityCommand(id, name, pType);
        }

        return read;
    }

    /**
     * Creates a dialog to enter the value of the command parameter.
     * @param context The Context object used to show the dialog
     * @param title The title of the dialog
     * @param commandParameter An array used to hold the value of the parameter
     * @param listener The OnClickListener instance for the dialog
     * @return The created dialog instance
     */
    public AlertDialog createParameterInputDialog(Context context, String title, final String[] commandParameter, final DialogInterface.OnClickListener listener) {
        if(parameterType == null) return null;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        if(parameterType.matches("range.*")) {
            int min = 0;
            int max = 100;
            if(parameterType.matches("range\\([0-9]+-[0-9]+\\)")) {
                min = Integer.parseInt(parameterType.replaceFirst("range\\(([0-9]+)-([0-9]+)\\)", "$1"));
                max = Integer.parseInt(parameterType.replaceFirst("range\\(([0-9]+)-([0-9]+)\\)", "$2"));
            } else if(parameterType.matches("range\\([0-9]+\\)")) {
                max = Integer.parseInt(parameterType.replaceFirst("range\\(([0-9]+)\\)", "$1"));
            }

            final int fMin = min;

            View view = View.inflate(context, R.layout.command_param_progress, null);
            final SeekBar bar = (SeekBar) view.findViewById(R.id.command_param_range_seekbar);
            bar.setMax(max - min);

            if(commandParameter[0] != null && commandParameter[0].matches("[0-9]+")) {
                int value = Integer.parseInt(commandParameter[0]);
                bar.setProgress(value);
            }

            final TextView txtValue = (TextView) view.findViewById(R.id.command_param_range_value);
            txtValue.setText(Integer.toString(min + bar.getProgress()));

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }

                @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    txtValue.setText(Integer.toString(fMin + value));
                }
            });

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    commandParameter[0] = Integer.toString(fMin + bar.getProgress());
                    listener.onClick(dialogInterface, i);
                }
            });

            builder.setView(view);
        } else {
            return null;
        }

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        return builder.show();
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
    public static final Creator<EntityCommand> CREATOR = new Creator<EntityCommand>() {
        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
         */
        @Override public EntityCommand createFromParcel(Parcel source) { return null; }

        /**
         * Unused method implementation since
         * local serialization is done with reflection.
         * @see android.os.Parcelable.Creator#newArray(int)
         */
        @Override public EntityCommand[] newArray(int size) { return new EntityCommand[0]; }
    };

}
