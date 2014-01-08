package hu.rycus.intellihome.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import hu.rycus.intellihome.MainActivity;
import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;
import hu.rycus.intellihome.model.Entity;
import hu.rycus.intellihome.model.EntityCommand;
import hu.rycus.intellihome.util.Intents;
import hu.rycus.intellihome.util.PopupUtil;
import hu.rycus.intellihome.util.RemoteServiceCreator;

/**
 * UI Fragment displaying the list of entities alias devices.
 *
 * Created by Viktor Adam on 12/11/13.
 */
public class DeviceListFragment extends Fragment {

    private RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(RemoteService service) {
            super.onServiceInstanceReceived(service);
            service.requestDeviceList(typeFilter);
        }
    };

    private GridView gridDeviceList;
    private DeviceListAdapter adapter;

    private Integer typeFilter;

    private boolean isAdministrator = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_device_list, container, false);

        gridDeviceList = (GridView) root.findViewById(R.id.grid_device_list);

        if(adapter == null) { // this could be retained from previous state
            adapter = new DeviceListAdapter();
        }

        gridDeviceList.setAdapter(adapter);

        return root;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item); // TODO popup
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(Intents.ACTION_DEVICE_LIST));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(Intents.ACTION_DEVICE_STATE_CHANGED));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Bundle args = getArguments();
        if(args != null) {
            typeFilter = args.getInt("type");
        }

        isAdministrator = ((MainActivity) getActivity()).isLoggedInAsAdministrator();

        rsc.bind(getActivity());
    }

    @Override
    public void onDestroy() {
        rsc.unbind(getActivity());
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.device_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.refresh) {
            if(rsc != null && rsc.isServiceBound()) {
                rsc.getService().requestDeviceList(typeFilter);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static DeviceListFragment create(Integer type) {
        DeviceListFragment fragment = new DeviceListFragment();

        if(type != null) {
            Bundle args = new Bundle();
            args.putInt("type", type);
            fragment.setArguments(args);
        }

        return fragment;
    }

    private class DeviceListAdapter extends BaseAdapter {

        private final SimpleDateFormat checkinDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private Entity[] entities = new Entity[0];

        @Override
        public int getCount() {
            return entities.length;
        }

        @Override
        public Object getItem(int position) {
            return entities[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder = null;

            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_device, parent, false);

                TextView txtName            = (TextView) view.findViewById(R.id.device_name);
                TextView txtType            = (TextView) view.findViewById(R.id.device_type);
                TextView txtState           = (TextView) view.findViewById(R.id.device_state);
                TextView txtLastCheckin     = (TextView) view.findViewById(R.id.device_last_checkin);
                ImageView imgImage          = (ImageView) view.findViewById(R.id.device_image);
                Button btnCommand1          = (Button) view.findViewById(R.id.device_command_1);
                Button btnCommand2          = (Button) view.findViewById(R.id.device_command_2);
                ImageButton btnCommandMore  = (ImageButton) view.findViewById(R.id.device_command_more);

                btnCommandMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupUtil.showPopup(getActivity(), v);
                    }
                });

                holder = new ViewHolder();
                holder.txtName          = txtName;
                holder.txtType          = txtType;
                holder.txtState         = txtState;
                holder.txtLastCheckin   = txtLastCheckin;
                holder.imgImage         = imgImage;
                holder.btnCommand1      = btnCommand1;
                holder.btnCommand2      = btnCommand2;
                holder.btnCommandMore   = btnCommandMore;
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Entity item = (Entity) getItem(position);

            holder.txtName.setText(item.getName());
            if(isAdministrator) {
                holder.txtName.setTag(R.id.item_entity, item);
                holder.txtName.setOnClickListener(renameListener);
            } else {
                holder.txtName.setCompoundDrawables(null, null, null, null);
            }

            holder.txtType.setText(item.getType().getName());
            holder.txtState.setText(item.getState().getName());
            holder.txtLastCheckin.setText(checkinDateFormat.format(new Date(item.getLastCheckin())));
            holder.imgImage.setImageDrawable(item.getType().getImage(getResources()));

            GradientDrawable background = (GradientDrawable) getResources().getDrawable(R.drawable.box_bg);
            if(item.getType().getColorCode() != null) {
                int color = Color.parseColor(item.getType().getColorCode());
                ((GradientDrawable) background.mutate()).setColor(color);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(background);
            } else {
                view.setBackgroundDrawable(background);
            }

            final EntityCommand[] commands = item.getType().getCommands();
            holder.btnCommand1.setVisibility(commands.length > 0 ? View.VISIBLE : View.GONE);
            holder.btnCommand2.setVisibility(commands.length > 1 ? View.VISIBLE : View.GONE);
            holder.btnCommandMore.setVisibility(commands.length > 2 ? View.VISIBLE : View.GONE);

            if(commands.length > 0) {
                holder.btnCommand1.setTag(R.id.item_entity, item);
                holder.btnCommand1.setTag(R.id.item_command, commands[0]);
                holder.btnCommand1.setText(commands[0].getName());
                holder.btnCommand1.setOnClickListener(commandClickListener);
            }
            if(commands.length > 1) {
                holder.btnCommand2.setTag(R.id.item_entity, item);
                holder.btnCommand2.setTag(R.id.item_command, commands[1]);
                holder.btnCommand2.setText(commands[1].getName());
                holder.btnCommand2.setOnClickListener(commandClickListener);
            }
            if(commands.length > 2) {
                LinkedHashMap<Integer, String> extraItems = new LinkedHashMap<>();
                for(int idx = 2; idx < commands.length; idx++) {
                    extraItems.put(commands[idx].getId(), commands[idx].getName());
                }

                final Entity fEntity = item;
                PopupUtil.register(getActivity(), holder.btnCommandMore, extraItems, new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        EntityCommand command = null;
                        for(EntityCommand cmd : commands) {
                            if(cmd.getId() == item.getItemId()) {
                                command = cmd;
                                break;
                            }
                        }

                        if(command.getParameterType() != null) {
                            final EntityCommand fCommand = command;
                            final String[] parameter = new String[] { fEntity.getStateValue() };
                            command.createParameterInputDialog(getActivity(), command.getName(), parameter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    rsc.getService().sendCommand(fEntity.getId(), fCommand.getId(), parameter[0]);
                                }
                            });
                        } else {
                            rsc.getService().sendCommand(fEntity.getId(), command.getId(), null);
                        }

                        return true;
                    }
                });
            }

            return view;
        }

        private final View.OnClickListener commandClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Entity entity = (Entity) view.getTag(R.id.item_entity);
                final EntityCommand command = (EntityCommand) view.getTag(R.id.item_command);

                if(command.getParameterType() != null) {
                    final String[] parameter = new String[] { entity.getStateValue() };
                    command.createParameterInputDialog(getActivity(), command.getName(), parameter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            rsc.getService().sendCommand(entity.getId(), command.getId(), parameter[0]);
                        }
                    });
                } else {
                    rsc.getService().sendCommand(entity.getId(), command.getId(), null);
                }
            }
        };

        private final View.OnClickListener renameListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Entity entity = (Entity) view.getTag(R.id.item_entity);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.rename_device));

                final EditText vEdit = new EditText(getActivity());
                vEdit.setText(entity.getName());
                builder.setView(vEdit);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = vEdit.getText().toString();
                        if(rsc != null && rsc.isServiceBound()) {
                            rsc.getService().renameDevice(entity.getId(), name);
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
                        vEdit.selectAll();
                        vEdit.requestFocus();

                        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        manager.showSoftInput(vEdit, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                dialog.show();
            }
        };

        private class ViewHolder {
            TextView txtName;
            TextView txtType;
            TextView txtState;
            TextView txtLastCheckin;
            ImageView imgImage;
            Button btnCommand1;
            Button btnCommand2;
            ImageButton btnCommandMore;
        }

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intents.ACTION_DEVICE_LIST.equals(intent.getAction())) {
                String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                if(error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                } else {
                    Entity[] entities = (Entity[]) intent.getParcelableArrayExtra(Intents.EXTRA_DEVICE_LIST_ENTITIES);
                    adapter.entities = entities;
                    adapter.notifyDataSetChanged();
                }
            } else if(Intents.ACTION_DEVICE_STATE_CHANGED.equals(intent.getAction())) {
                String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                if(error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                } else {
                    Entity changed = (Entity) intent.getParcelableExtra(Intents.EXTRA_DEVICE_STATE);

                    if(typeFilter != null && typeFilter != changed.getType().getId()) {
                        // The device is not shown on this fragment
                        return;
                    }

                    boolean found = false;
                    for (int index = 0; index < adapter.entities.length; index++) {
                        Entity entity = adapter.entities[index];
                        if(entity.getId().equals(changed.getId())) {
                            adapter.entities[index] = changed;
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        adapter.notifyDataSetChanged();
                    } else {
                        if(rsc != null && rsc.isServiceBound()) {
                            rsc.getService().requestDeviceList(typeFilter);
                        }
                    }
                }
            }
        }
    };

}