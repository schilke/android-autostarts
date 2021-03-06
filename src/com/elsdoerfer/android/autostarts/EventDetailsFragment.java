package com.elsdoerfer.android.autostarts;

import android.app.*;
import android.support.v4.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import com.elsdoerfer.android.autostarts.db.IntentFilterInfo;
import src.com.elsdoerfer.android.autostarts.opt.MarketUtils;


public class EventDetailsFragment extends DialogFragment {

    static EventDetailsFragment newInstance(IntentFilterInfo event) {
        EventDetailsFragment f = new EventDetailsFragment();

        Bundle args = new Bundle();
        args.putParcelable("event", event);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final IntentFilterInfo event = getArguments().getParcelable("event");
        final ListActivity activity = (ListActivity)getActivity();

        View v = activity.getLayoutInflater().inflate(
                R.layout.receiver_info_panel, null, false);
                String formattedString = String.format(
				getString(R.string.receiver_info),
				event.componentInfo.componentName, event.action, event.priority);
		((TextView)v.findViewById(R.id.message)).setText(
				Html.fromHtml(formattedString));

        Dialog d = new AlertDialog.Builder(activity).setItems(
            new CharSequence[] {
                    getResources().getString(
                            (event.componentInfo.isCurrentlyEnabled())
                            ? R.string.disable
                            : R.string.enable),
                    getResources().getString(R.string.appliation_info),
                    getResources().getString(MarketUtils.FIND_IN_MARKET_TEXT)},
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which) {
                    activity.mLastChangeRequestDoEnable =
                        !event.componentInfo.isCurrentlyEnabled();
                    switch (which) {
                    case 0:
                        // Depending on what we disable, show a warning specifically
                        // for that component, a general warning or just proceed without
                        // any explicit warning whatsoever.
                        if (!activity.mLastChangeRequestDoEnable &&
                                event.componentInfo.packageInfo.packageName.equals("com.google.android.apps.gtalkservice") &&
                                event.componentInfo.componentName.equals("com.google.android.gtalkservice.ServiceAutoStarter"))
                            activity.showDialog(ListActivity.DIALOG_CONFIRM_GOOGLE_TALK_WARNING);
                        else if (event.componentInfo.packageInfo.isSystem && !activity.mLastChangeRequestDoEnable)
                            activity.showDialog(ListActivity.DIALOG_CONFIRM_SYSAPP_CHANGE);
                        else {
                            activity.mToggleTask = new ToggleTask(activity);
                            activity.mToggleTask.execute(
                                    event.componentInfo,
                                    activity.mLastChangeRequestDoEnable);
                        }
                        break;
                    case 1:
                        String packageName =
                            event.componentInfo.packageInfo.packageName;
                        Intent infoIntent = new Intent();
                        infoIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        infoIntent.setData(Uri.parse("package:" + packageName));
                        try {
                            startActivity(infoIntent);
                        }
                        catch (ActivityNotFoundException e) {
                            // 2.2 and below.
                            infoIntent = new Intent();
                            infoIntent.setClassName("com.android.settings",
                                    "com.android.settings.InstalledAppDetails");
                            infoIntent.putExtra("com.android.settings.ApplicationPkgName",
                                    packageName);
                            try {
                                startActivity(infoIntent);
                            } catch (ActivityNotFoundException e2) {}
                        }
                        break;
                    case 2:
                        MarketUtils.findPackageInMarket(activity,
                                event.componentInfo.packageInfo.packageName);
                        break;
                    }
                    dialog.dismiss();
                }
            })
            .setTitle(event.componentInfo.getLabel()).setView(v).create();

        return d;
    }

}
