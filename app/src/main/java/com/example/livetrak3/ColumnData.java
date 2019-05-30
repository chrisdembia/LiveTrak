package com.example.livetrak3;

import java.util.ArrayList;

class ColumnData {
    public String label = "";
    public ArrayList<OptionData> optionDatas = new ArrayList();

    public ColumnData(String label) {
        this.label = label;
    }

    public void addOption(OptionData od) {
        this.optionDatas.add(od);
    }
}
