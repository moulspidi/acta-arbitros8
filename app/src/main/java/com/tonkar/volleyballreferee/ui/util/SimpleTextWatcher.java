package com.tonkar.volleyballreferee.ui.util;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    public interface OnText { void accept(String s); }
    public static SimpleTextWatcher on(OnText cb){
        return new SimpleTextWatcher(){
            @Override public void onTextChanged(CharSequence s, int a, int b, int c){
                cb.accept(s == null ? "" : s.toString());
            }
        };
    }
    @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
    @Override public void afterTextChanged(Editable s) {}
}
