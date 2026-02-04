package tw.nekomimi.nekogram.utils.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

public abstract class PopupUtils {

    public interface OnItemClickListener {
        void onClick(int i);
    }

    public static void showDialog(CharSequence[] items, String title, int selectedIndex, Context context, OnItemClickListener listener) {
        showDialog(items, null, title, selectedIndex, context, listener, null, true);
    }

    public static void showDialog(CharSequence[] items, int[] icons, String title, int selectedIndex, Context context, final OnItemClickListener listener, Theme.ResourcesProvider resourcesProvider, boolean useCustomView) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(title);

        if (useCustomView) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            final AlertDialog[] dialogRef = new AlertDialog[1];

            for (int i = 0; i < items.length; i++) {
                RadioColorCell radioColorCell = new RadioColorCell(context);
                radioColorCell.setPadding(AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(4.0f), 0);
                radioColorCell.setCheckColor(Theme.getColor(Theme.key_radioBackground, resourcesProvider), Theme.getColor(Theme.key_dialogRadioBackgroundChecked, resourcesProvider));
                radioColorCell.setTextAndValue(items[i], selectedIndex == i);
                radioColorCell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
                
                linearLayout.addView(radioColorCell);
                
                final int position = i;
                radioColorCell.setOnClickListener(view -> {
                    if (dialogRef[0] != null) {
                        dialogRef[0].dismiss();
                    }
                    if (listener != null) {
                        listener.onClick(position);
                    }
                });
            }
            dialogRef[0] = builder.create();
            builder.show();
        } else {
            if (icons != null) {
                builder.setItems(items, icons, (dialogInterface, i) -> {
                    if (listener != null) {
                        listener.onClick(i);
                    }
                });
            } else {
                builder.setItems(items, (dialogInterface, i) -> {
                    if (listener != null) {
                        listener.onClick(i);
                    }
                });
            }
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            builder.show();
        }
    }

    public static void showDialogWithoutRadio(List<?> list, String title, Context context, OnItemClickListener listener) {
        if (list == null) return;
        
        CharSequence[] items = list.stream()
                .map(String::valueOf)
                .toArray(CharSequence[]::new);

        showDialog(items, null, title, -1, context, listener, null, false);
    }
}