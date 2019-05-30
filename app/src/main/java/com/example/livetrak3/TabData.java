package com.example.livetrak3;

import java.util.ArrayList;

class TabData {
    public String label = "";
    public ArrayList<ColumnData> columnDatas = new ArrayList();

    public TabData(String label) {
        this.label = label;
    }

    public void addColumn(ColumnData od) {
        this.columnDatas.add(od);
    }
}

