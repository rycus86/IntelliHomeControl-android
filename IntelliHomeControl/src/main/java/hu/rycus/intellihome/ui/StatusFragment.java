package hu.rycus.intellihome.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import hu.rycus.intellihome.MainActivity;
import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;
import hu.rycus.intellihome.util.Defaults;
import hu.rycus.intellihome.util.Intents;
import hu.rycus.intellihome.util.MD5Util;
import hu.rycus.intellihome.util.PreferenceKeys;
import hu.rycus.intellihome.util.RemoteServiceCreator;

/**
 * UI Fragment to display the current connection status.
 *
 * Created by Viktor Adam on 12/28/13.
 */
public class StatusFragment extends Fragment {

    private TextView txtStatus;
    private View vLoginPanel;
    private TextView txtLogin;
    private TextView txtUsername;
    private TextView txtSettings;

    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(final RemoteService service) {
            super.onServiceInstanceReceived(service);
            txtStatus.post(new Runnable() {
                @Override
                public void run() {
                    setConnected(service.isConnected());
                }
            });
        }
    };

    public static StatusFragment create() {
        return new StatusFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        rsc.bind(getActivity());
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(Intents.ACTION_CALLBACK));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        rsc.unbind(getActivity());
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_status, container, false);

        txtStatus = (TextView) root.findViewById(R.id.status_txt_status);

        vLoginPanel = root.findViewById(R.id.status_panel_login);
        txtLogin    = (TextView) vLoginPanel.findViewById(R.id.status_txt_login);
        txtUsername = (TextView) vLoginPanel.findViewById(R.id.status_txt_username);

        txtSettings = (TextView) root.findViewById(R.id.status_txt_settings);
        txtSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).startSettingsActivity();
            }
        });

        vLoginPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View root = View.inflate(view.getContext(), R.layout.dialog_useredit, null);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String username = prefs.getString(PreferenceKeys.Authentication.USERNAME, Defaults.Authentication.USERNAME);

                final EditText editUsername = (EditText) root.findViewById(R.id.login_username);
                editUsername.setText(username);

                final EditText editPassword1 = (EditText) root.findViewById(R.id.login_password1);

                EditText editPassword2 = (EditText) root.findViewById(R.id.login_password2);
                editPassword2.setVisibility(View.GONE);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setView(root);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String username = editUsername.getText().toString();
                        String password = editPassword1.getText().toString();

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        prefs.edit()
                                .putString(PreferenceKeys.Authentication.USERNAME, username)
                                .putString(PreferenceKeys.Authentication.PASSWORD, MD5Util.toMD5(password))
                                .commit();

                        if(rsc != null && rsc.isServiceBound()) {
                            rsc.getService().restartCommunication();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        editUsername.selectAll();

                        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        manager.showSoftInput(editUsername, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                dialog.show();
            }
        });

        return root;
    }

    private void setConnected(boolean connected) {
        if(connected) {
            txtStatus.setText(R.string.status_connected);
            txtStatus.setBackgroundColor(getResources().getColor(R.color.status_connected));
            txtLogin.setText(R.string.status_connected_login);
        } else {
            txtStatus.setText(R.string.status_disconnected);
            txtStatus.setBackgroundColor(getResources().getColor(R.color.status_disconnected));
            txtLogin.setText(R.string.status_disconnected_login);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = prefs.getString(PreferenceKeys.Authentication.USERNAME, Defaults.Authentication.USERNAME);

        txtUsername.setText(username);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intents.ACTION_CALLBACK.equals(intent.getAction())) {
                boolean connected = intent.getBooleanExtra(Intents.EXTRA_CONNECTION_STATE, false);
                setConnected(connected);
            }
        }
    };

}
