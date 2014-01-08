package hu.rycus.intellihome.network;

/**
 * Header bytes for packets in network communication.
 *
 * Created by Viktor Adam on 10/30/13.
 */
public interface Header {

    /* These headers are defined by the remote IntelliHomeControl server. */

    int MSG_A_LOGIN                 = 0xA1;
    int MSG_A_LIST_DEVICE_TYPES     = 0xA2;
    int MSG_A_LIST_DEVICES          = 0xA3;
    int MSG_A_SEND_COMMAND          = 0xA4;
    int MSG_A_STATE_CHANGED         = 0xA5;
    int MSG_A_LOAD_TYPE_IMAGE       = 0xA6;
    int MSG_A_RENAME_DEVICE         = 0xA7;
    int MSG_A_COUNT_HISTORY         = 0xB1;
    int MSG_A_LIST_HISTORY          = 0xB2;
    int MSG_A_LIST_USERS            = 0xC1;
    int MSG_A_USER_CREATE           = 0xC2;
    int MSG_A_USER_EDIT             = 0xC3;
    int MSG_A_USER_DELETE           = 0xC4;
    int MSG_A_USERS_CHANGED         = 0xC5;
    int MSG_A_KEEPALIVE             = 0xE0;
    int MSG_A_ERROR                 = 0xF0;
    int MSG_A_EXIT                  = 0xFE;

    int MSG_A_ERROR_INVALID_SESSION = 0xF1;

}
