package hu.rycus.intellihome;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;

import hu.rycus.intellihome.model.Entity;
import hu.rycus.intellihome.model.EntityType;
import hu.rycus.intellihome.ui.DeviceListFragment;
import hu.rycus.intellihome.ui.HistoryFragment;
import hu.rycus.intellihome.ui.StatusFragment;
import hu.rycus.intellihome.ui.UserListFragment;
import hu.rycus.intellihome.util.Intents;
import hu.rycus.intellihome.util.RemoteServiceCreator;

/**
 * The main activity of the application.
 * The source is based on the default implementation of
 * ActionBarActivity with Navigation Drawer.
 *
 * Created by Viktor Adam on 12/1/13.
 */
public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    /** Helper object to bind/unbind the remote service. */
    private RemoteServiceCreator rsc;

    /** @see android.app.Activity#onStop() */
    @Override
    protected void onStop() {
        super.onStop();
        if(isFinishing()) {
            rsc.unbind(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing() && rsc != null) {
            rsc.unbind(this);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return rsc;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Intents.ACTION_CALLBACK));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Intents.ACTION_DEVICE_TYPES_LISTED));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mTitle = getTitle();

        rsc = (RemoteServiceCreator) getLastCustomNonConfigurationInstance();
        if(rsc == null) {
            rsc = new RemoteServiceCreator();
            rsc.bind(this);
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    public boolean isConnected() {
        return rsc != null && rsc.isServiceBound() && rsc.getService().isConnected();
    }

    public boolean isLoggedInAsAdministrator() {
        if(rsc != null && rsc.isServiceBound()) {
            return rsc.getService().isAdministratorUser();
        }
        return false;
    }

    public boolean allowsSelection(int navigationItemPosition) {
        if(mNavigationDrawerFragment == null) return true; // initialization

        if(navigationItemPosition > 0 && !isConnected()) return false;

        String item = mNavigationDrawerFragment.getItemAt(navigationItemPosition);
        if(item != null) {
            return !item.endsWith("*") || isLoggedInAsAdministrator();
        }

        return false;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment = null;
        String tag = null;

        int staticCountAtStart = mNavigationDrawerFragment != null ? mNavigationDrawerFragment.getStaticItemCountAtStart() : 0;
        int dynamicCount = mNavigationDrawerFragment != null ? mNavigationDrawerFragment.getDynamicItemCount() : 0;

        if(position == 0) {
            tag = "Status";
            fragment = fragmentManager.findFragmentByTag(tag);
            if(fragment == null) {
                fragment = StatusFragment.create();
            }
        } else if(position == 1) {
            tag = "DeviceList";
            fragment = fragmentManager.findFragmentByTag(tag);
            if(fragment == null) {
                fragment = DeviceListFragment.create(null);
            }
        } else if(position > 1 && position < staticCountAtStart + dynamicCount) {
            int index  = 0;
            int target = position - staticCountAtStart;

            EntityType type = null;
            for(EntityType et : EntityType.list()) {
                if(index++ == target) {
                    type = et;
                    break;
                }
            }

            tag = "DeviceList." + type.getId();
            fragment = fragmentManager.findFragmentByTag(tag);
            if(fragment == null) {
                fragment = DeviceListFragment.create(type.getId());
            }
        } else if(position == staticCountAtStart + dynamicCount + 0) {
            tag = "History";
            fragment = fragmentManager.findFragmentByTag(tag);
            if(fragment == null) {
                fragment = HistoryFragment.create(null);
            }
        } else if(position == staticCountAtStart + dynamicCount + 1) {
            tag = "Users";
            fragment = fragmentManager.findFragmentByTag(tag);
            if(fragment == null) {
                fragment = UserListFragment.create();
            }
        }

        onSectionAttached(position);

        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.app_name);
                break;
            default:
            {
                String title = mNavigationDrawerFragment.getItemAt(number);
                if(title != null) {
                    if(title.endsWith("*")) {
                        title = title.substring(0, title.length() - 1);
                    }
                    mTitle = title;
                }
                break;
            }
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    public void startSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void updateDrawer() {
        mNavigationDrawerFragment.updated();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.container);
            if(fragment != null) {
                fragment.onCreateOptionsMenu(menu, getMenuInflater());
            }

            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startSettingsActivity();
            return true;
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.container);
            if(fragment != null) {
                if(fragment.onOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intents.ACTION_CALLBACK.equals(intent.getAction())) {
                String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                if(error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                }

                if(intent.hasExtra(Intents.EXTRA_CONNECTION_STATE)) {
                    updateDrawer();

                    boolean connected = intent.getBooleanExtra(Intents.EXTRA_CONNECTION_STATE, false);

                    if(!connected) {
                        mNavigationDrawerFragment.selectItem(0);
                    }

                    if(connected && rsc != null && rsc.isServiceBound()) {
                        rsc.getService().requestDeviceTypeList();
                    }
                }
            } else if(Intents.ACTION_DEVICE_TYPES_LISTED.equals(intent.getAction())) {
                String error = intent.getStringExtra(Intents.EXTRA_ERROR);
                if(error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                } else {
                    Collection<EntityType> eTypes = EntityType.list();
                    String[] types = new String[eTypes.size()];

                    String postfix = getResources().getString(R.string.postfix_devices);

                    int index = 0;
                    for(EntityType type : eTypes) {
                        types[index++] = type.getName() + " " + postfix;
                    }

                    mNavigationDrawerFragment.setDynamicListContents(types);
                }
            }
        }
    };

}
