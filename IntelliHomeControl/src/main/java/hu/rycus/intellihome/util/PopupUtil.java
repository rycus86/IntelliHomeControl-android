package hu.rycus.intellihome.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.util.LinkedHashMap;
import java.util.Map;

import hu.rycus.intellihome.R;

/**
 * Utility class to help popup creation and display.
 *
 * Created by Viktor Adam on 12/23/13.
 */
public class PopupUtil {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void register(Activity activity, View view, LinkedHashMap<Integer, String> menuItems, final MenuItem.OnMenuItemClickListener listener) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PopupMenu menu = new PopupMenu(activity, view);
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return listener.onMenuItemClick(item);
                }
            });
            view.setTag(R.id.mi_popup, menu);
            view.setTag(R.id.mi_popup_menu_items, menuItems);
        } else {
            view.setTag(R.id.mi_popup_listener, listener);
            view.setTag(R.id.mi_popup_menu_items, menuItems);
            activity.registerForContextMenu(view);
        }
    }

    public static void dispatchCreateContextMenuCreation(Activity activity, View view, ContextMenu menu) {
        LinkedHashMap<Integer, String> menuItems = (LinkedHashMap<Integer, String>) view.getTag(R.id.mi_popup_menu_items);
        if(menuItems != null) {
            for(Map.Entry<Integer, String> entry : menuItems.entrySet()) {
                menu.add(Menu.NONE, entry.getKey(), Menu.NONE, entry.getValue());
            }
        }
    }

    public static boolean dispatchContextMenuItemSelected(View view, MenuItem item) {
        MenuItem.OnMenuItemClickListener listener = (MenuItem.OnMenuItemClickListener) view.getTag(R.id.mi_popup_listener);
        if(listener != null) {
            return listener.onMenuItemClick(item);
        }

        return false;
    }

    public static void showPopup(Activity activity, View view) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PopupMenu menu = (PopupMenu) view.getTag(R.id.mi_popup);
            if(menu != null) {
                menu.getMenu().clear();

                LinkedHashMap<Integer, String> menuItems = (LinkedHashMap<Integer, String>) view.getTag(R.id.mi_popup_menu_items);
                if(menuItems != null) {
                    for(Map.Entry<Integer, String> entry : menuItems.entrySet()) {
                        menu.getMenu().add(Menu.NONE, entry.getKey(), Menu.NONE, entry.getValue());
                    }

                    menu.show();
                }
            }

            return;
        }

        activity.openContextMenu(view);
    }

}
