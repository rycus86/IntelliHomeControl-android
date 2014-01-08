package hu.rycus.intellihome.util;

/**
 * Utility class containing action and extra names for intent parameters.
 *
 * Created by Viktor Adam on 11/10/13.
 */
public interface Intents {

    /** The common prefix of actions. */
    String PREFIX_ACTION                = "hu.rycus.ihcontrol";
    /** The common prefix of extras. */
    String PREFIX_EXTRA                 = "ihc:";

    String ACTION_CALLBACK              = PREFIX_ACTION + ".CALLBACK";

    String EXTRA_CONNECTION_STATE       = PREFIX_EXTRA + "connected";

    String EXTRA_ERROR                  = PREFIX_EXTRA + "error";

    String ACTION_DEVICE_LIST           = PREFIX_ACTION + ".DEVICE_LIST";

    String ACTION_DEVICE_TYPES_LISTED   = PREFIX_ACTION + ".DEVICE_TYPES_LISTED";

    String EXTRA_DEVICE_LIST_ENTITIES   = PREFIX_EXTRA + "entities";

    String ACTION_USER_LIST             = PREFIX_ACTION + ".USER_LIST";

    String ACTION_USERS_CHANGED         = PREFIX_ACTION + ".USERS_CHANGED";

    String EXTRA_USER_LIST              = PREFIX_EXTRA + "users";

    String ACTION_DEVICE_STATE_CHANGED  = PREFIX_ACTION + ".STATE_CHANGED";

    String EXTRA_DEVICE_STATE           = PREFIX_EXTRA + "state";

}
