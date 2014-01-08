package hu.rycus.intellihome.util;

/**
 * Utility class storing preference keys.
 *
 * Created by Viktor Adam on 12/4/13.
 */
public interface PreferenceKeys {

    /** Communication related preference keys. */
    public interface Communication {

        String PREFIX   = "communication.";
        String MODE     = PREFIX + "mode";
        String HOST     = PREFIX + "host";
        String PORT     = PREFIX + "port";

    }

    /** Authentication related preference keys. */
    public interface  Authentication {

        String PREFIX   = "auth.";
        String USERNAME = PREFIX + "username";
        String PASSWORD = PREFIX + "password";

    }

}
