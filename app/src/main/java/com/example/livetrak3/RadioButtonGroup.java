package com.example.livetrak3;

import android.view.View;
import java.util.HashSet;
import java.util.Set;

public class RadioButtonGroup {
    private static final String TAG = "RadioButtonGroup";
    Set<RadioButtonX> buttons = new HashSet();
    private RadioButtonX checkedView = null;
    public String groupName = "";
    private boolean mProtectFromCheckedChange = false;
    private SessionActivity session;

    public RadioButtonGroup(SessionActivity session, String groupName) {
        this.session = session;
        this.groupName = groupName;
    }

    public void addButton(RadioButtonX button) {
        this.buttons.add(button);
        button.setGroup(this);
    }

    public void check(RadioButtonX b, boolean recordChange) {
        if ((b == null || b != this.checkedView) && !this.mProtectFromCheckedChange) {
            this.mProtectFromCheckedChange = true;
            if (this.checkedView != null) {
                this.checkedView.setChecked(false);
            }
            this.mProtectFromCheckedChange = false;
            setCheckedView(b);
            if (recordChange) {
                this.session.recordChange(this);
            }
        }

    }
    public void check(RadioButtonX b) {
        check(b, true);
    }

    public View getCheckedRadioButton() {
        return this.checkedView;
    }

    public String toString() {
        if (this.checkedView != null) {
            return this.checkedView.toString();
        }
        return "<no selection>";
    }

    public boolean isChecked() {
        return this.checkedView != null;
    }

    private void setCheckedView(RadioButtonX b) {
        this.checkedView = b;
    }
}
