package com.file.transfer.core.view;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.file.transfer.R;
import com.file.transfer.core.util.DensityUtil;
import com.file.transfer.databinding.PopupConfigOptionBinding;

import java.util.List;

public class CustomPopupMenu {
    private Context context;
    private int currentValue;
    private boolean isProfileShow;

    private List<String> listData;
    private BottomConfigAdapter bottomConfigAdapter;
    private BottomConfigAdapter.OnItemConfigListener itemConfigListener;

    private View viewAnchor;
    private String currItem;
    private PopupWindow popupWindow;
    private boolean isWidthWrapContent;
    private PopupConfigOptionBinding popupBinding;

    public CustomPopupMenu(Context context, View viewAnchor) {
        this.context = context;
        this.viewAnchor = viewAnchor;
        this.popupBinding = PopupConfigOptionBinding.inflate(LayoutInflater.from(context));
    }

    public void setCurrItem(String currItem) {
        this.currItem = currItem;
    }

    public void setDatas(List<String> listData, boolean profileShow) {
        this.isProfileShow = profileShow;
        this.listData = listData;
    }

    public void changeData(List<String> listData, boolean profileShow) {
        this.listData = listData;
        this.isProfileShow = profileShow;
        if (this.bottomConfigAdapter != null) {
            this.bottomConfigAdapter.setListData(this.listData);
        }
    }

    public List<String> getListDataSize() {
        return this.listData;
    }

    public int getCurrentValuePosition() {
        return this.currentValue;
    }

    public String getCurrentValue() {
        try {
            return this.listData.get(this.currentValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setCurrentValue(int position) {
        this.currentValue = position;
    }

    public void setWidthWrapContent(boolean widthWrapContent) {
        isWidthWrapContent = widthWrapContent;
    }

    public void setItemConfigListener(BottomConfigAdapter.OnItemConfigListener itemConfigListener) {
        this.itemConfigListener = itemConfigListener;
    }

    private void buildConfig() {
        this.initPopupMenu();
        this.initAdapter();
    }

    private void initPopupMenu() {
        this.popupWindow = new PopupWindow(this.context);
        this.popupWindow.setContentView(this.popupBinding.getRoot());

        if (this.isWidthWrapContent) {
            int spacing = (int) context.getResources().getDimension(R.dimen.px28);
            float pixels = (float) DensityUtil.widthPixels(context);

            this.popupWindow.setWidth((int) (pixels / 3) + spacing * 2);
        } else {
            int width = this.viewAnchor.getWidth();
            this.popupWindow.setWidth(width);
        }
        this.popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setFocusable(true);
        this.popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this.context, R.drawable.bg_transparrent));
    }

    private void initAdapter() {
        this.bottomConfigAdapter = new BottomConfigAdapter(this.context);
        this.bottomConfigAdapter.setCurrItem(this.currItem);
        this.bottomConfigAdapter.setItemConfigListener(this.itemConfigListener);
        this.popupBinding.rcvOptionPdfFile.setLayoutManager(new LinearLayoutManager(this.context));
        this.popupBinding.rcvOptionPdfFile.setAdapter(this.bottomConfigAdapter);
        this.bottomConfigAdapter.setListData(this.listData);
    }

    public boolean isShowwing() {
        return this.popupWindow != null && this.popupWindow.isShowing();
    }

    public void show() {
        show(false);
    }

    public void show(boolean isMainOption) {
        if (isShowwing()) {
            return;
        }
        this.buildConfig();
        if (this.popupWindow == null) {
            return;
        }

        int[] values = new int[2];
        this.viewAnchor.getLocationInWindow(values);
        int positionOfIconHeight = values[1];
        int positionOfIconWidth = values[0];

        DisplayMetrics displayMetrics = this.context.getResources().getDisplayMetrics();
        int centerHeight = displayMetrics.heightPixels / 2;
        int centerWidth = displayMetrics.widthPixels / 2;

        popupBinding.rcvOptionPdfFile.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int rvHeight = popupBinding.rcvOptionPdfFile.getMeasuredHeight();
        int rvWidth = popupBinding.rcvOptionPdfFile.getMeasuredWidth();

        int xoff = -(2 * this.viewAnchor.getWidth()) - rvWidth;
        int width = !isMainOption || positionOfIconWidth < centerWidth ? 0 : xoff;
        if (positionOfIconHeight > centerHeight) {
            this.popupWindow.showAsDropDown(this.viewAnchor, width, -this.viewAnchor.getHeight() - rvHeight);
        } else {
            this.popupWindow.showAsDropDown(this.viewAnchor, width, 0);
        }
    }

    public void dismiss() {
        if (this.popupWindow != null) {
            this.popupWindow.dismiss();
        }
    }
}
