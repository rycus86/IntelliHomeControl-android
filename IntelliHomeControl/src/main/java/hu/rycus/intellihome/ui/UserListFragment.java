package hu.rycus.intellihome.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;
import hu.rycus.intellihome.model.User;
import hu.rycus.intellihome.util.Defaults;
import hu.rycus.intellihome.util.Intents;
import hu.rycus.intellihome.util.MD5Util;
import hu.rycus.intellihome.util.PreferenceKeys;
import hu.rycus.intellihome.util.RemoteServiceCreator;

/**
 * UI Fragment related to user management.
 *
 * Created by Viktor Adam on 12/28/13.
 */
public class UserListFragment extends Fragment {

    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(RemoteService service) {
            super.onServiceInstanceReceived(service);
            service.requestUserList();
        }
    };

    private Button btnCreate;
    private ListView listUsers;

    private UserListAdapter mAdapter;

    public static UserListFragment create() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        rsc.bind(getActivity());
    }

    @Override
    public void onDestroy() {
        rsc.unbind(getActivity());
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_users, container, false);

        btnCreate = (Button)   root.findViewById(R.id.users_btn_create);
        listUsers = (ListView) root.findViewById(R.id.users_list);

        if(mAdapter == null) {
            mAdapter = new UserListAdapter();
        }

        listUsers.setAdapter(mAdapter);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditDialog(null, null);
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(Intents.ACTION_USER_LIST));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(Intents.ACTION_USERS_CHANGED));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    private void showEditDialog(final Integer userId, String username) {
        View root = View.inflate(getActivity(), R.layout.dialog_useredit, null);

        final EditText editUsername = (EditText) root.findViewById(R.id.login_username);
        final EditText editPassword1 = (EditText) root.findViewById(R.id.login_password1);
        final EditText editPassword2 = (EditText) root.findViewById(R.id.login_password2);

        if(username != null) {
            editUsername.setText(username);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(username != null) {
            builder.setTitle(String.format(getResources().getString(R.string.user_edit, username)));
        } else {
            builder.setTitle(getResources().getString(R.string.user_create));
        }
        builder.setView(root);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                editUsername.selectAll();

                InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.showSoftInput(editUsername, InputMethodManager.SHOW_IMPLICIT);

                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String username = editUsername.getText().toString();
                        String password1 = editPassword1.getText().toString();
                        String password2 = editPassword2.getText().toString();

                        if (password1.trim().isEmpty()) {
                            editPassword1.setError(getResources().getString(R.string.error_password_empty));
                            editPassword1.selectAll();
                            editPassword1.requestFocus();
                            return;
                        } else if (password2.trim().isEmpty()) {
                            editPassword2.setError(getResources().getString(R.string.error_password_empty));
                            editPassword2.selectAll();
                            editPassword2.requestFocus();
                            return;
                        } else if (!password1.equals(password2)) {
                            editPassword2.setError(getResources().getString(R.string.error_password_mismatch));
                            editPassword2.selectAll();
                            editPassword2.requestFocus();
                            return;
                        }

                        if (rsc != null && rsc.isServiceBound()) {
                            if(userId != null) {
                                rsc.getService().requestEditUser(userId, username, MD5Util.toMD5(password1));
                            } else {
                                rsc.getService().requestCreateUser(username, MD5Util.toMD5(password1));
                            }
                        }

                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private class UserListAdapter extends BaseAdapter {

        private final ArrayList<User> items = new ArrayList<>();

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return items.get(i).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder = null;

            if(view == null) {
                view = View.inflate(parent.getContext(), R.layout.item_user, null);

                holder = new ViewHolder();
                holder.txtUsername  = (TextView)    view.findViewById(R.id.user_txt_username);
                holder.btnEdit      = (ImageButton) view.findViewById(R.id.user_btn_edit);
                holder.btnDelete    = (ImageButton) view.findViewById(R.id.user_btn_delete);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            User item = (User) getItem(position);

            holder.txtUsername.setText(item.getUsername());
            holder.btnEdit.setTag(item);
            holder.btnEdit.setOnClickListener(editListener);
            holder.btnDelete.setEnabled(!item.isAdministrator());
            if(!item.isAdministrator()) {
                holder.btnDelete.setEnabled(true);
                holder.btnDelete.setTag(item);
                holder.btnDelete.setOnClickListener(deleteListener);
            }

            return view;
        }

        private class ViewHolder {
            TextView txtUsername;
            ImageButton btnEdit;
            ImageButton btnDelete;
        }

        private final View.OnClickListener editListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = (User) view.getTag();
                showEditDialog(user.getId(), user.getUsername());
            }
        };

        private final View.OnClickListener deleteListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final User user = (User) view.getTag();

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.user_confirm_delete);
                builder.setMessage(getResources().getString(R.string.user_delete, user.getUsername()));
                builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(rsc != null && rsc.isServiceBound()) {
                            rsc.getService().requestDeleteUser(user.getId());
                        }
                    }
                });
                builder.show();
            }
        };

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intents.ACTION_USER_LIST.equals(intent.getAction())) {
                User[] users = (User[]) intent.getParcelableArrayExtra(Intents.EXTRA_USER_LIST);
                if(users != null) {
                    mAdapter.items.clear();
                    mAdapter.items.addAll(Arrays.asList(users));
                    mAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.error_list_users), Toast.LENGTH_SHORT).show();
                }
            } else if(Intents.ACTION_USERS_CHANGED.equals(intent.getAction())) {
                if(rsc != null && rsc.isServiceBound()) {
                    rsc.getService().requestUserList();
                }
            }
        }
    };

}
