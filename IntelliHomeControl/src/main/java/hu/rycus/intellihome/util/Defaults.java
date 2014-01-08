package hu.rycus.intellihome.util;

/**
 * Utility class storing various default values.
 *
 * Created by Viktor Adam on 12/4/13.
 */
public interface Defaults {

    /** Communication related defaults. */
    public interface Communication {

        String  MODE            = "mcast";

        String  PORT            = "49001";
        String  MCAST_GROUP     = "227.1.1.10";
        String  BCAST_ADDRESS   = "255.255.255.255";

    }

    /** Authentication related defaults. */
    public interface Authentication {

        String USERNAME = "admin";
        String PASSWORD = MD5Util.toMD5(USERNAME);

    }

}
