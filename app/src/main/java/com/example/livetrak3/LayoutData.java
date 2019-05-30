package com.example.livetrak3;

import java.util.ArrayList;

public class LayoutData {
    private static final String NEW_COLUMN = "<COLUMN>";
    private static final String SPACE = "<SPACE>";
    private static String TAG = "LayoutData";
    private int maxColItemCount = 0;
    public ArrayList<TabData> tabDatas;

    public LayoutData(ArrayList<TabData> tabDatas, int maxColItemCount) {
        this.tabDatas = tabDatas;
        this.maxColItemCount = maxColItemCount;
    }
}