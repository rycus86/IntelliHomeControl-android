package hu.rycus.intellihome.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import hu.rycus.intellihome.R;
import hu.rycus.intellihome.RemoteService;
import hu.rycus.intellihome.model.EntityHistory;
import hu.rycus.intellihome.util.RemoteServiceCreator;


/**
 * UI Fragment displaying entries in history.
 *
 * Created by Viktor Adam on 15/11/13.
 */
public class HistoryFragment extends Fragment {

    private static final int PREFETCH_LIMIT = 25;

    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(RemoteService service) {
            super.onServiceInstanceReceived(service);
            createCountTask().execute();
        }
    };

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    private TextView txtFrom;
    private EditText editFromDate;
    private EditText editFromTime;
    private TextView txtTo;
    private EditText editToDate;
    private EditText editToTime;
    private TextView txtEntityLabel;
    private TextView txtEntityName;
    private ImageButton btnToggle;
    private ListView listContents;

    private HistoryAdapter mAdapter;

    private Long tsFrom;
    private Long tsTo;
    private String entityId;

    private boolean filterHidden = true;

    public static HistoryFragment create(String entityId) {
        HistoryFragment fragment = new HistoryFragment();

        if(entityId != null) {
            Bundle bundle = new Bundle();
            bundle.putString("entityId", entityId);
            fragment.setArguments(bundle);
        }

        return fragment;
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
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        txtFrom         = (TextView) root.findViewById(R.id.hist_label_from);
        editFromDate    = (EditText) root.findViewById(R.id.hist_txt_from_date);
        editFromTime    = (EditText) root.findViewById(R.id.hist_txt_from_time);
        txtTo           = (TextView) root.findViewById(R.id.hist_label_to);
        editToDate      = (EditText) root.findViewById(R.id.hist_txt_to_date);
        editToTime      = (EditText) root.findViewById(R.id.hist_txt_to_time);
        txtEntityLabel  = (TextView) root.findViewById(R.id.hist_label_device);
        txtEntityName   = (TextView) root.findViewById(R.id.hist_label_device_name);
        btnToggle       = (ImageButton) root.findViewById(R.id.hist_filter_toggle);
        listContents    = (ListView) root.findViewById(R.id.hist_contents);

        if(mAdapter == null) {
            mAdapter = new HistoryAdapter();
        }

        listContents.setAdapter(mAdapter);

        Bundle args = getArguments();
        if(args != null) {
            entityId = args.getString("entityId");
        }

        btnToggle.setImageResource(filterHidden ?
            R.drawable.ic_filter_expand :
            R.drawable.ic_filter_collapse);

        txtFrom.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        editFromDate.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        editFromTime.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        txtTo.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        editToDate.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        editToTime.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
        txtEntityLabel.setVisibility( entityId != null && !filterHidden ? View.VISIBLE : View.GONE );
        txtEntityName.setVisibility(  entityId != null && !filterHidden ? View.VISIBLE : View.GONE );

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterHidden = !filterHidden;

                btnToggle.setImageResource(filterHidden ?
                        R.drawable.ic_filter_expand :
                        R.drawable.ic_filter_collapse);

                txtFrom.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                editFromDate.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                editFromTime.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                txtTo.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                editToDate.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                editToTime.setVisibility(filterHidden ? View.GONE : View.VISIBLE);
                txtEntityLabel.setVisibility( entityId != null && !filterHidden ? View.VISIBLE : View.GONE );
                txtEntityName.setVisibility(  entityId != null && !filterHidden ? View.VISIBLE : View.GONE );
            }
        });

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, 1);

        tsTo    = calendar.getTimeInMillis();
        tsFrom  = tsTo - (7 * 24 * 60 * 60 * 1000L); // one week

        editFromDate.setText(dateFormat.format(new Date(tsFrom)));
        editFromTime.setText(timeFormat.format(new Date(tsFrom)));
        editToDate.setText(dateFormat.format(new Date(tsTo)));
        editToTime.setText(timeFormat.format(new Date(tsTo)));

        editFromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(tsFrom);

                int year  = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int date  = calendar.get(Calendar.DATE);

                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int date) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(tsFrom);
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DATE, date);
                        tsFrom = calendar.getTimeInMillis();
                        editFromDate.setText(dateFormat.format(calendar.getTime()));

                        createCountTask().execute();
                    }
                }, year, month, date);
                dialog.show();
            }
        });

        editFromTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(tsFrom);

                int hour    = calendar.get(Calendar.HOUR_OF_DAY);
                int minute  = calendar.get(Calendar.MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(tsFrom);
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        tsFrom = calendar.getTimeInMillis();
                        editFromTime.setText(timeFormat.format(calendar.getTime()));

                        createCountTask().execute();
                    }
                }, hour, minute, true);
                dialog.show();
            }
        });

        editToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(tsTo);

                int year  = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int date  = calendar.get(Calendar.DATE);

                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int date) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(tsTo);
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DATE, date);
                        tsTo = calendar.getTimeInMillis();
                        editToDate.setText(dateFormat.format(calendar.getTime()));

                        createCountTask().execute();
                    }
                }, year, month, date);
                dialog.show();
            }
        });

        editToTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(tsTo);

                int hour    = calendar.get(Calendar.HOUR_OF_DAY);
                int minute  = calendar.get(Calendar.MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(tsTo);
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        tsTo = calendar.getTimeInMillis();
                        editToTime.setText(timeFormat.format(calendar.getTime()));

                        createCountTask().execute();
                    }
                }, hour, minute, true);
                dialog.show();
            }
        });

        return root;
    }

    private class HistoryAdapter extends BaseAdapter {

        private final ArrayList<HistoryItem> items = new ArrayList<>();

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
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder = null;

            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_history, parent, false);

                holder = new ViewHolder();
                holder.txtLoading   = (TextView) view.findViewById(R.id.hist_item_loading);
                holder.txtTimestamp = (TextView) view.findViewById(R.id.hist_item_timestamp);
                holder.txtName      = (TextView) view.findViewById(R.id.hist_item_name);
                holder.txtAction    = (TextView) view.findViewById(R.id.hist_item_action);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            HistoryItem item = (HistoryItem) getItem(position);

            holder.txtLoading.setVisibility(item.loaded ? View.GONE : View.VISIBLE);
            holder.txtTimestamp.setVisibility(item.loaded ? View.VISIBLE : View.GONE);
            holder.txtName.setVisibility(item.loaded ? View.VISIBLE : View.GONE);
            holder.txtAction.setVisibility(item.loaded ? View.VISIBLE : View.GONE);

            if(item.loaded) {
                EntityHistory hi = item.data;
                holder.txtTimestamp.setText(timestampFormat.format(new Date(hi.getTimestamp())));
                holder.txtName.setText(hi.getEntityName());
                holder.txtAction.setText(hi.getAction());
            } else if(!item.loading) {
                loadItems(position);
            }

            view.setBackgroundColor(position % 2 == 0 ? getResources().getColor(R.color.history_alternate_list_color) : Color.TRANSPARENT); // TODO from resource

            return view;
        }

        private class ViewHolder {
            TextView txtLoading;
            TextView txtTimestamp;
            TextView txtName;
            TextView txtAction;
        }

        private void loadItems(int fromPosition) {
            int endPosition = fromPosition;

            HistoryItem firstItem = (HistoryItem) getItem(fromPosition);
            firstItem.loading = true;

            for(int idx = 1; idx < PREFETCH_LIMIT; idx++) {
                if(fromPosition + idx >= getCount()) break;

                HistoryItem item = (HistoryItem) getItem(fromPosition + idx);
                if(!item.loaded) {
                    endPosition = fromPosition + idx;
                    item.loading = true;
                }
            }

            int limit = endPosition - fromPosition + 1;
            int offset = fromPosition;

            createLoaderTask().execute(limit, offset);
        }

        private AsyncTask<Integer, Void, Boolean> createLoaderTask() {
            return new AsyncTask<Integer, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Integer... params) {
                    int limit  = params[0];
                    int offset = params[1];

                    EntityHistory[] loadedItems = rsc.getService().listHistory(tsFrom, tsTo, entityId, limit, offset);
                    if(loadedItems != null) {
                        for(int idx = 0; idx < loadedItems.length; idx++) {
                            HistoryItem item = items.get(offset + idx);
                            if(!item.loaded) {
                                item.data = loadedItems[idx];
                                item.loaded = true;
                            }
                        }

                        return true;
                    }

                    return false;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    notifyDataSetChanged();

                    if(!success) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.error_list_history), Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }

    }

    private AsyncTask<Void, Void, Integer> createCountTask() {
        return new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... none) {
                int count = rsc.getService().countHistory(tsFrom, tsTo, entityId);
                if(count >= 0) {
                    mAdapter.items.clear();
                    mAdapter.items.ensureCapacity(count);

                    for(int idx = 0; idx < count; idx++) {
                        mAdapter.items.add(new HistoryItem()); // TODO use only around 200 items on the list, clear what isn't used at the moment
                    }
                }
                return count;
            }

            @Override
            protected void onPostExecute(Integer count) {
                mAdapter.notifyDataSetChanged();

                if(count < 0 && getActivity() != null) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.error_count_history), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private class HistoryItem {
        boolean loaded = false;
        boolean loading = false;
        EntityHistory data = null;
    }

}
