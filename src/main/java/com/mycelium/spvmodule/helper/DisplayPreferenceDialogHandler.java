package com.mycelium.spvmodule.helper;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mycelium.spvmodule.R;
import com.mycelium.view.adapter.DialogListAdapter;


public class DisplayPreferenceDialogHandler implements PreferenceManager.OnDisplayPreferenceDialogListener {
    private Context context;
    private AlertDialog alertDialog;

    public DisplayPreferenceDialogHandler(Context context) {
        this.context = context;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        int theme = R.style.MyceliumSettings_Dialog;
        switch (preference.getKey()) {
            case "node_option":
                theme = R.style.MyceliumSettings_Dialog_Small;
                break;
        }

        if (preference instanceof ListPreference) {
            final ListPreference listPreference = (ListPreference) preference;

            View view = LayoutInflater.from(context).inflate(R.layout.dialog_pref_list, null);
            TextView title = view.findViewById(R.id.title);
            title.setText(listPreference.getDialogTitle());
            final RecyclerView listView = view.findViewById(android.R.id.list);
            listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            final int selectedIndex = listPreference.findIndexOfValue(listPreference.getValue());
            final DialogListAdapter arrayAdapter = new DialogListAdapter(listPreference.getEntries(), selectedIndex);
            listView.setAdapter(arrayAdapter);
            listView.scrollToPosition(selectedIndex);
            view.findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });

            view.findViewById(R.id.buttonok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                    String value = listPreference.getEntryValues()[arrayAdapter.getSelected()].toString();
                    if (listPreference.callChangeListener(value)) {
                        listPreference.setValue(value);
                    }
                }
            });
            alertDialog = new AlertDialog.Builder(context, theme)
                    .setView(view)
                    .create();
            alertDialog.show();
        } else {
            throw new IllegalArgumentException("Tried to display dialog for unknown " +
                    "preference type. Did you forget to override onDisplayPreferenceDialog()?");
        }

    }
}
