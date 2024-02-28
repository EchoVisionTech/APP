package com.dam.echovisiontech.ui.historial;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistorialViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HistorialViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Welcome to EchoVisionPro App!\n Still in development. :)");
    }

    public LiveData<String> getText() {
        return mText;
    }
}