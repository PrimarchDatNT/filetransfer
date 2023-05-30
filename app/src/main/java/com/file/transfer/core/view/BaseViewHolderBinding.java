package com.file.transfer.core.view;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public class BaseViewHolderBinding<T extends ViewBinding> extends RecyclerView.ViewHolder {
    public T binding;

    public BaseViewHolderBinding(@NonNull View itemView) {
        super(itemView);
    }

    public BaseViewHolderBinding(T binding) {
        this(binding.getRoot());
        this.binding = binding;
    }

    public Context getContext() {
        return binding.getRoot().getContext();
    }
}
